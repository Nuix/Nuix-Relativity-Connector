using System;
using System.Collections.Generic;
using System.Text;
using System.IO;
using Client.Models;
using System.Runtime.Serialization.Json;
using kCura.Relativity.ImportAPI;
using System.Data;
using kCura.Relativity.DataReaderClient;
using Microsoft.VisualBasic.FileIO;

namespace Client
{
    class Program
    {
        string password;
        int workspaceId;
        int folderId;
        FileInfo settingsFileInfo;
        FileInfo loadfileFileInfo;
        Models.Settings settings;

        const string concordanceQuotes = "þ";
        const string concordanceDelimiter = "\x0014";

        static void Main(string[] args)
        {
            new Program(args);
        }

        void WriteUsage()
        {
            Console.WriteLine("usage: Client <settings.json> <loadfile.dat> <workspaceId> <folderId> password");
            Console.WriteLine("example:");
            Console.WriteLine("\tClient C:\\sample\\settings.json C:\\sample\\loadfile.dat 10000051 29100007 Secret1234");
            Console.WriteLine();
        }

        void ParseArguments(string[] args)
        {
            var pathToSettings = args[0];
            settingsFileInfo = new FileInfo(pathToSettings);

            var pathToLoadfile = args[1];
            loadfileFileInfo = new FileInfo(pathToLoadfile);

            workspaceId = int.Parse(args[2]);
            folderId = int.Parse(args[3]);
            password = args[4];
        }


        public Program(string[] args)
        {
            if (args.Length == 0)
            {
                WriteUsage();
                return;
            }

            ParseArguments(args);
            var dataContractJsonSerializer = new DataContractJsonSerializer(typeof(Models.Settings));
            settings = (Models.Settings)dataContractJsonSerializer.ReadObject(settingsFileInfo.OpenRead());
            runImportJob();
            Environment.Exit(Environment.ExitCode);
        }

        private void runImportJob()
        {

            var eventHandler = new EventHandler();
            var importApi = new ImportAPI(settings.RelativitySettings.Username, password, settings.RelativitySettings.WebServiceUrl);
            var importJob = importApi.NewNativeDocumentImportJob();

            importJob.OnMessage += new ImportBulkArtifactJob.OnMessageEventHandler(eventHandler.ImportJobOnMessage);
            importJob.OnError += new ImportBulkArtifactJob.OnErrorEventHandler(eventHandler.ImportJobOnError);
            importJob.OnFatalException += new IImportNotifier.OnFatalExceptionEventHandler(eventHandler.ImportJobOnFatalException);

            importJob.Settings.ArtifactTypeId = 10;
            importJob.Settings.CaseArtifactId = workspaceId;

            if (folderId > 0)
            {
                importJob.Settings.DestinationFolderArtifactID = folderId;
            }
            importJob.Settings.ExtractedTextEncoding = Encoding.UTF8;
            importJob.Settings.ExtractedTextFieldContainsFilePath = true;
            importJob.Settings.MaximumErrorCount = int.MaxValue - 1;
            importJob.Settings.NativeFileCopyMode = settings.UploadSettings.NativeCopyMode;

            if (!settings.UploadSettings.NativeCopyMode.Equals(NativeFileCopyModeEnum.DoNotImportNativeFiles))
            {
                importJob.Settings.NativeFilePathSourceFieldName = "NativeFilePathSourceFieldName";
                settings.FieldsSettings.FieldsMapping.Add(
                    new Field()
                    {
                        LoadfileColumn = "ITEMPATH",
                        WorkspaceColumn = "NativeFilePathSourceFieldName"
                    });
            }

            if (settings.FieldsSettings.FolderPathSourceFieldName != null && settings.FieldsSettings.FolderPathSourceFieldName.Length > 0)
            {
                importJob.Settings.FolderPathSourceFieldName = "FolderPathSourceFieldName";
                settings.FieldsSettings.FieldsMapping.Add(
                    new Field()
                    {
                        LoadfileColumn = settings.FieldsSettings.FolderPathSourceFieldName,
                        WorkspaceColumn = "FolderPathSourceFieldName"
                    });
            }

            var dataTable = readLoadfile(loadfileFileInfo);
            importJob.SourceData.SourceData = dataTable.CreateDataReader();

            importJob.Settings.OverwriteMode = settings.UploadSettings.OverwriteMode;
            importJob.Settings.RelativityUsername = settings.RelativitySettings.Username;
            importJob.Settings.RelativityPassword = password;
            importJob.Settings.WebServiceURL = settings.RelativitySettings.WebServiceUrl;

            bool identifierSet = false;
            foreach (var field in settings.FieldsSettings.FieldsMapping)
            {
                if (!identifierSet && field.Identifier)
                {
                    importJob.Settings.SelectedIdentifierFieldName = field.WorkspaceColumn;
                    identifierSet = true;
                    Console.WriteLine("Using identifier loadfile field " + field.LoadfileColumn);
                }
            }
            importJob.Execute();
        }

        private DataTable readLoadfile(FileInfo loadfileFileInfo)
        {
            var dataTable = new DataTable();

            Dictionary<string, int> loadfileColumns = new Dictionary<string, int>();
            int fileLoadfileColumnId = -1;
            int textLoadfileColumnId = -1;
            int loadfileLineId = 0;

            using (var parser = new TextFieldParser(loadfileFileInfo.OpenRead()))
            {
                Console.WriteLine("Parsing loadfile as Concordance");
                var loadfileQuotes = concordanceQuotes;
                parser.SetDelimiters(concordanceDelimiter);
                parser.HasFieldsEnclosedInQuotes = true;

                var loadfileColumnNames = parser.ReadFields();

                int loadfileColumnId = 0;
                foreach (var loadfileColumnName in loadfileColumnNames)
                {
                    var unquotedLoadfileColumnName = loadfileColumnName.Replace(loadfileQuotes, "");

                    if (!loadfileColumns.ContainsKey(unquotedLoadfileColumnName))
                    {
                        loadfileColumns.Add(unquotedLoadfileColumnName, loadfileColumnId);
                    }
                    else
                    {
                        Console.WriteLine("WARNING: Column " + unquotedLoadfileColumnName + " apprears multiple times in the loadfile");
                    }
                    loadfileColumnId++;
                }

                Dictionary<int, int> dataTableToloadfileColumnId = new Dictionary<int, int>();

                int dataTableColumnId = 0;

                foreach (var field in settings.FieldsSettings.FieldsMapping)
                {
                    var loadfileColumnName = field.LoadfileColumn;
                    if (!loadfileColumns.ContainsKey(loadfileColumnName))
                    {
                        throw new Exception("Column \"" + loadfileColumnName + "\" defined in the settings file could not be found in the loadfile");
                    }
                    loadfileColumnId = loadfileColumns[loadfileColumnName];

                    dataTable.Columns.Add(field.WorkspaceColumn, typeof(string));

                    dataTableToloadfileColumnId.Add(dataTableColumnId, loadfileColumnId);

                    if ("ITEMPATH".Equals(field.LoadfileColumn))
                    {
                        fileLoadfileColumnId = loadfileColumnId;
                    }
                    else if ("TEXTPATH".Equals(field.LoadfileColumn))
                    {
                        textLoadfileColumnId = loadfileColumnId;
                    }
                    dataTableColumnId++;
                }

                while (!parser.EndOfData)
                {
                    var dataTableRow = dataTable.NewRow();
                    var lineValues = parser.ReadFields();

                    foreach (KeyValuePair<int, int> entry in dataTableToloadfileColumnId)
                    {
                        var fieldValue = lineValues[entry.Value].Replace(loadfileQuotes, "");
                        if (entry.Value == fileLoadfileColumnId || entry.Value == textLoadfileColumnId)
                        {
                            if (!Path.IsPathRooted(fieldValue))
                            {
                                fieldValue = Path.Combine(loadfileFileInfo.Directory.FullName, fieldValue);
                            }
                        }
                        dataTableRow.SetField<string>(entry.Key, fieldValue);
                    }

                    dataTable.Rows.Add(dataTableRow);
                    loadfileLineId++;
                }
            }
            Console.WriteLine("Loadfile records count: " + loadfileLineId);
            return dataTable;
        }
    }
}
