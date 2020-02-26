using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Client.Models
{
    [DataContract]
    class FieldsSettings
    {
        [DataMember(Name = "metadataProfileName")]
        public string MetadataProfileName { get; set; }


        [DataMember(Name = "folderPathSourceFieldName")]
        public string FolderPathSourceFieldName { get; set; }


        [DataMember(Name = "FieldList")]
        public List<Field> FieldsMapping { get; set; }


    }
}
