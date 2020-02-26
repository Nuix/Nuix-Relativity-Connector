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
    class RelativitySettings
    {
        [DataMember(Name = "username")]
        public String Username { get; set; }

        [DataMember(Name = "webServiceUrl")]
        public String WebServiceUrl { get; set; }

    }
}
