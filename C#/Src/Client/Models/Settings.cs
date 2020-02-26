using kCura.Relativity.DataReaderClient;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Client.Models
{
    [DataContract]
    class Settings
    {
        [DataMember(Name = "fieldsSettings")]
        public FieldsSettings FieldsSettings { get; set; }
        
        [DataMember(Name = "uploadSettings")]
        public UploadSettings UploadSettings { get; set; }

        [DataMember(Name = "relativitySettings")]
        public RelativitySettings RelativitySettings { get; set; }
    }
}
