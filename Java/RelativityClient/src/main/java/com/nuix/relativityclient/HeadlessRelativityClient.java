package com.nuix.relativityclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuix.relativityclient.clients.RelativityJdbcUploadClient;
import com.nuix.relativityclient.clients.RelativityLoadfileUploadClient;
import com.nuix.relativityclient.enums.ModuleType;
import com.nuix.relativityclient.model.ClientLauncherSettings;
import com.nuix.relativityclient.model.ModuleSettings;
import com.nuix.relativityclient.utils.ApplicationLogger;
import com.nuix.relativityclient.utils.JdbcClient;
import nuix.Case;
import nuix.MetadataProfile;
import nuix.ProductionSet;
import nuix.Utilities;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class HeadlessRelativityClient {


    private static final Logger LOGGER = LogManager.getLogger(HeadlessRelativityClient.class.getName());

    public HeadlessRelativityClient(ApplicationLogger applicationLogger, Utilities utilities, Case nuixCase, String pathToScript, Map<String, Object> settings) {

        applicationLogger.logProgress("Initializing", 0, 1, -1, 1);
        ObjectMapper objectMapper = new ObjectMapper();
        ModuleSettings moduleSettings;
        try {
            String serializedSettings = objectMapper.writeValueAsString(settings.get("moduleSettings"));
            moduleSettings = objectMapper.readValue(serializedSettings, ModuleSettings.class);
        } catch (IOException e) {
            LOGGER.error("Cannot deserialize module settings", e);
            applicationLogger.logException("Cannot deserialize module settings", e);
            return;
        }

        try {

            Map<String, Object> bootstrapSettings = (Map<String, Object>) settings.get("bootstrapSettings");
            if (bootstrapSettings == null) {
                applicationLogger.logError("Missing bootstrapSettings");
                return;
            }

            Integer workspaceArtifactId = (Integer) bootstrapSettings.get("workspaceArtifactId");
            if (workspaceArtifactId == null) {
                throw new IllegalArgumentException("Missing bootstrap setting workspaceArtifactId");
            }

            Integer folderArtifactId = (Integer) bootstrapSettings.get("folderArtifactId");
            if (folderArtifactId == null) {
                throw new IllegalArgumentException("Missing bootstrap setting folderArtifactId");
            }


            ClientLauncherSettings clientLauncherSettings = new ClientLauncherSettings();

            if (ModuleType.LOADFILE_IMPORT.name().equals(settings.get("moduleType"))) {
                clientLauncherSettings.setModuleType(ModuleType.LOADFILE_IMPORT);

                String loadfilePath = (String) bootstrapSettings.get("loadfile");
                if (loadfilePath == null) {
                    throw new IllegalArgumentException("Missing bootstrap setting loadfile");
                }

                File loadfile = new File(loadfilePath);
                if (!loadfile.exists()) {
                    throw new IllegalArgumentException("The loadfile could not be found under:\n" + loadfile.getAbsolutePath());
                }

                String relativityPassword = (String) bootstrapSettings.get("relativityPassword");
                if (relativityPassword == null) {
                    throw new IllegalArgumentException("Missing bootstrap setting relativityPassword");
                }

                String relativityVersion = (String) bootstrapSettings.get("relativityVersion");
                if (relativityVersion == null) {
                    throw new IllegalArgumentException("Missing bootstrap setting relativityVersion");
                }

                clientLauncherSettings.setLoadfilePath(loadfilePath);
                clientLauncherSettings.setWorkspaceArtifactId(workspaceArtifactId);
                clientLauncherSettings.setFolderArtifactId(folderArtifactId);

                clientLauncherSettings.setRelativityPassword(relativityPassword);
                clientLauncherSettings.setRelativityVersion(relativityVersion);
                clientLauncherSettings.setScriptPath(pathToScript);

                File tempSettingsPath = File.createTempFile("relativity-client-settings", ".json");
                applicationLogger.logInfo("Copying settings to "+tempSettingsPath.getAbsolutePath());

                objectMapper.writeValue(tempSettingsPath,moduleSettings);
                clientLauncherSettings.setClientSettingsPath(tempSettingsPath.getAbsolutePath());
                RelativityLoadfileUploadClient relativityLoadfileUploadClient = new RelativityLoadfileUploadClient(applicationLogger, clientLauncherSettings);

                Path clientPath = relativityLoadfileUploadClient.getClientPath();
                if (!clientPath.toFile().exists()) {
                    String message = "The Relativity Upload Client for Relativity version " + clientLauncherSettings.getRelativityVersion() + " could not be found under:\n" + clientPath.toString() + "\n\nPlease see documentation instructions on how to build the client and retry.";
                    IOException ioException = new IOException(message);
                    throw ioException;
                }
                relativityLoadfileUploadClient.run();

            } else if (ModuleType.JDBC.name().equals(settings.get("moduleType"))) {

                String sqlPassword = (String) bootstrapSettings.get("sqlPassword");
                if (sqlPassword == null) {
                    throw new IllegalArgumentException("Missing bootstrap setting sqlPassword");
                }


                String productionSetName = (String) bootstrapSettings.get("productionSetName");
                if (nuixCase == null) {
                    throw new IllegalArgumentException("This module requires a case");
                }

                ProductionSet productionSet = null;
                for (ProductionSet caseProductionSet : nuixCase.getProductionSets()) {
                    if (caseProductionSet.getName().equals(productionSetName)) {
                        productionSet = caseProductionSet;
                        break;
                    }
                }

                if (productionSet == null) {
                    throw new IllegalArgumentException("Cannot find production set " + productionSetName + " in case");
                }

                String metadataProfileName = moduleSettings.getFieldsSettings().getMetadataProfileName();
                MetadataProfile metadataProfile = utilities.getMetadataProfileStore().getMetadataProfile(metadataProfileName);
                if (metadataProfile == null) {
                    metadataProfile = nuixCase.getMetadataProfileStore().getMetadataProfile(metadataProfileName);
                }

                if (metadataProfile == null) {
                    throw new IllegalArgumentException("Cannot find metadata profile " + metadataProfileName);
                }

                try {


                    JdbcClient jdbcClient = new JdbcClient(moduleSettings.getSqlSettings().getServerName(),
                            moduleSettings.getSqlSettings().getServerPort(),
                            moduleSettings.getSqlSettings().getInstanceName(),
                            moduleSettings.getSqlSettings().getDomain(),
                            moduleSettings.getSqlSettings().getUsername(),
                            sqlPassword);

                    RelativityJdbcUploadClient client = new RelativityJdbcUploadClient(applicationLogger, jdbcClient, moduleSettings, productionSet, metadataProfile, workspaceArtifactId, folderArtifactId);
                    client.run();
                } catch (SQLException e) {
                    throw new IllegalArgumentException("Cannot connect to SQL Server: " + e.getMessage(), e);
                }


            } else {
                LOGGER.error("Unrecognized or unspecified moduleType, expected " + ModuleType.JDBC.name() + " or " + ModuleType.LOADFILE_IMPORT.name() + ", got " + settings.get("moduleType"));
                applicationLogger.logError("Unrecognized or unspecified moduleType, expected " + ModuleType.JDBC.name() + " or " + ModuleType.LOADFILE_IMPORT.name() + ", got " + settings.get("moduleType"));
            }

        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error("Cannot start upload", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            applicationLogger.logError("Cannot start upload, " + sw.toString());
            return;
        }

    }
}
