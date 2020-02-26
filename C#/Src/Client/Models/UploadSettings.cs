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
    class UploadSettings
    {
        [DataMember(Name = "nativeCopyMode")]
        public NativeFileCopyModeEnum NativeCopyMode { get; set; }

        [DataMember(Name = "overwriteMode")]
        public OverwriteModeEnum OverwriteMode { get; set; }


    }
}
