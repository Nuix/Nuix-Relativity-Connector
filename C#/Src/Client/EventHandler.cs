using kCura.Relativity.DataReaderClient;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Data;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Client
{
    class EventHandler
    {
        public void ImportJobOnMessage(Status status)
        {
            Console.WriteLine(status.Message);
        }


        public void ImportJobOnError(IDictionary row)
        {
            foreach (var key in row.Keys)
            {
                Console.Error.WriteLine("Error "+key + ": " + row[key]);
            }
        }

        public void ImportJobOnFatalException(JobReport jobReport)
        {
            Console.Error.WriteLine(jobReport.FatalException);
            foreach (var errorRow in jobReport.ErrorRows)
            {
                Console.Error.WriteLine(errorRow);
            }
        }
    }
}
