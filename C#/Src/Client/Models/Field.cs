using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Client.Models
{
    [DataContract]
    class Field
    {
        [DataMember(Name = "loadfileColumn")]
        public string LoadfileColumn { get; set; }

        [DataMember(Name = "workspaceColumn")]
        public string WorkspaceColumn { get; set; }

        [DataMember(Name = "identifier")]
        public bool Identifier { get; set; }

    }
}
