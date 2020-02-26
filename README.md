Nuix Relativity Connector
===============================

![This script was last tested in Nuix 8.0](https://img.shields.io/badge/Script%20Tested%20in%20Nuix-8.0-green.svg)

View the GitHub project [here](https://github.com/Nuix/Nuix-Relativity-Connector).

# Overview

Uploads documents to Relativity, either from a Nuix case by directly writing to the Relativity File Share and SQL database, or from a Concordance loadfile using the Relativity Import API.

# Direct SQL and File Share Upload

## Getting Started

The following modules can be used for the Direct SQL and File Share Upload scenario
- JRuby Direct SQL and File Share Upload Client
- Java User Interface

### Prerequisites
- Network access to the Relativity SQL server
- An account with read access to the `EDDS` SQL database, and read/write access to the workspace SQL database
- If uploading text or native files, network access to the Relativity file share server(s)
- If uploading text files, read/write access with currently logged on Windows account to the workspace default data grid folder
- If uploading native files, read/write access with currently logged on Windows account to the workspace default file share folder

### Setup

#### JRuby Direct SQL and File Share Upload Client (optional)
- Not setup required.

#### Java User Interface
- Download and install the [Java Development Kit](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
- Download and install [Apache Maven](https://maven.apache.org/download.cgi)
- The Maven `pom.xml` file for the Java User Interface references Nuix Workstation 8.0.3.121. If using a different version of Nuix Workstation when building the Java User Interface, modify the file `Java\RelativityClientUserInterface\pom.xml` and update the property `scripting-version` to reflect the version of Nuix Workstation installed.
- In the `Java\RelativityClientUserInterface` folder from this repository, build the Java User Interface by running the command `mvn package`.
- Copy the resulting file `Java\RelativityClientUserInterface\target\relativity-client-1.0.jar` to `JavaScript\RelativityClient.nuixscript`
- Copy the folder `JavaScript\RelativityClient.nuixscript` to the Nuix Scripts folder

### Running the User Interface
From the Nuix Workstation Scripts menu, run the *Relativity Upload* script.

### Running the JRuby Direct SQL and File Share Upload Client
See sample files `JdbcUploadLauncher.rb` and `JdbcUploadSettings.json` from `Ruby\Samples` for an example of how to start the JRuby Direct SQL and File Share Upload Client.

### Running in Nuix RESTful Service
- Build the Java User Interface as indicated above
- Run the Java User Interface, fill-in the settings and save the settings file `RelativityClientSettings.json` 
- Copy the folder `JavaScript\RelativityClient.nuixscript` to the Nuix RESTful Service Scripts folder
    - The default location on Windows is `C:\Program Files\Nuix\Nuix RESTful Service\user-scripts`
    - If using a different location, update the `pathToScripts` variable from `JavaScript\RelativityClient.nuixscript\headlessLauncher`
- If copying native or text files, ensure that the account under which the service `Nuix-REST` is running has access to the default Relativity workspace file share and data grid location
- Call to the PUT `/v1/userScripts` REST API endpoint, with the scriptRequest JSON body constructed as follows:
    - `async`: `true`
    - `customArguments`: List containing:
        - `moduleType`: `JDBC`
        - `moduleSettings`: The contents of the previously saved settings file `RelativityClientSettings.json` 
        - `bootstrapSettings`: List containing:
            - `workspaceArtifactId`: The Artifact ID of the Relativity workspace
            - `folderArtifactId`: The Artifact ID of the Relativity folder inside the workspace
            - `productionSetName`: The name of the production set to upload
            - `sqlPassword`: The SQL Server password for the username provided in the moduleSettings section
    - `description`: `Load to Relativity`
    - `fileName`: `RelativityClient.nuixscript/headlessLauncher`
    - `language`: `PYTHON`

See below sample scriptRequest body:
````
{
    "async": true,
    "customArguments": {
        "bootstrapSettings": {
            "folderArtifactId": 1003697,
            "productionSetName": "Production00001",
            "sqlPassword": "password1234",
            "workspaceArtifactId": 1018980
        },
        "moduleSettings": {
            "fieldsSettings": {
                "FieldList": [
                    {
                        "identifier": true,
                        "loadfileColumn": "DOCID",
                        "workspaceColumn": "Control Number"
                    }
                ],
                "metadataProfileName": "Default"
            },
            "relativitySettings": {
                "username": "jsmith@example.com",
                "webServiceUrl": "https://relativity.example.com/relativitywebapi"
            },
            "sqlSettings": {
                "serverName": "SQL01",
                "serverPort": 1433,
                "username": "jsmith"
            },
            "uploadSettings": {
                "nativeCopyMode": 1,
                "overwriteMode": 0
            }
        },
        "moduleType": "JDBC"
    },
    "description": "Load to Relativity",
    "fileName": "RelativityClient.nuixscript/headlessLauncher",
    "language": "PYTHON"
}
````

# Loadfile Upload

## Getting Started

The following modules can be used for the Loadfile Upload scenario
- JRuby Relativity Loadfile Upload Client Wrapper
- Java User Interface

Both modules call the **C# Relativity Loadfile Upload Client** which is built using the Relativity Import API

### Setup

#### C# Relativity Loadfile Upload Client 
- Download the latest version of this repository
- Download [Visual Studio 2019 Community Edition](https://visualstudio.microsoft.com/downloads)  and install the .NET desktop development workload
- Download [nuget.exe](https://www.nuget.org/downloads) and place in the `C#` folder from this repository
- If support for Relativity versions 9.6, 9.5 and 9.4 is required, download the Relativity [9.6](https://platform.relativity.com/9.6/Content/Downloads/Download_the_SDKs.htm), [9.5](https://platform.relativity.com/9.5/Content/Downloads/Download_the_SDKs.htm) and [9.4](https://platform.relativity.com/9.4/Content/Downloads/Download_the_SDKs.htm) SDKs. For each SDK `VERSION`, install it and copy contents of folder `C:\Program Files\kCura Corporation\Relativity SDK` to the corresponding `C#\Clients\VERSION\Relativity SDK` folder of this repository, then uninstall the SDK
- Run the batch file `C#\build.bat` and resolve any eventual errors encountered

#### JRuby Relativity Loadfile Upload Client Wrapper (optional)
- Build the C# Relativity Loadfile Upload Client as indicated above
- Copy contents of `C#\Clients` to `Ruby\RelativityLoadfileUpload\Clients`

#### Java User Interface
- Build the C# Relativity Loadfile Upload Client as indicated above
- Copy contents of `C#\Clients` to `JavaScript\RelativityClient.nuixscript\RelativityLoadfileUpload\Clients`
- Download and install the [Java Development Kit](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
- Download and install [Apache Maven](https://maven.apache.org/download.cgi)
- The Maven `pom.xml` file for the Java User Interface references Nuix Workstation 8.0.3.121. If using a different version of Nuix Workstation when building the Java User Interface, modify the file `Java\RelativityClientUserInterface\pom.xml` and update the property `scripting-version` to reflect the version of Nuix Workstation installed.
- In the `Java\RelativityClientUserInterface` folder from this repository, build the Java User Interface by running the command `mvn package`.
- Copy the resulting file `Java\RelativityClientUserInterface\target\relativity-client-1.0.jar` to `JavaScript\RelativityClient.nuixscript`
- Copy the folder `JavaScript\RelativityClient.nuixscript` to the Nuix Scripts folder

### Running the User Interface
From the Nuix Workstation Scripts menu, run the *Relativity Upload* script.

### Running the JRuby Loadfile Upload Client Wrapper
See sample files `LoadfileUploadLauncher.rb` and `LoadfileUploadSettings.json` from `Ruby\Samples` for an example of how to start the JRuby Relativity Loadfile Upload Client Wrapper.

### Running in Nuix RESTful Service
- Build the Java User Interface as indicated above
- Run the Java User Interface, fill-in the settings and save the settings file `RelativityClientSettings.json` 
- Copy the folder `JavaScript\RelativityClient.nuixscript` to the Nuix RESTful Service Scripts folder
    - The default location on Windows is `C:\Program Files\Nuix\Nuix RESTful Service\user-scripts`
    - If using a different location, update the `pathToScripts` variable from `JavaScript\RelativityClient.nuixscript\headlessLauncher`
- Call to the PUT `/v1/userScripts` REST API endpoint, with the scriptRequest JSON body constructed as follows:
    - `async`: `true`
    - `customArguments`: List containing:
        - `moduleType`: `LOADFILE_IMPORT`
        - `moduleSettings`: The contents of the previously saved settings file `RelativityClientSettings.json` 
        - `bootstrapSettings`: List containing:
            - `workspaceArtifactId`: The Artifact ID of the Relativity workspace
            - `folderArtifactId`: The Artifact ID of the Relativity folder inside the workspace
            - `loadfile`: The path to the Concordance loadfile to upload
            - `relativityPassword`: The Relativity password for the username provided in the moduleSettings section
            - `relativityVersion`: The Relativity version, in the *major*.*minor* format, for example `10.2`
            
    - `description`: `Load to Relativity`
    - `fileName`: `RelativityClient.nuixscript/headlessLauncher`
    - `language`: `PYTHON`

See below sample scriptRequest body:
````
{
    "async": true,
    "customArguments": {
        "bootstrapSettings": {
            "folderArtifactId": 1003697,
            "loadfile": "C:\\Cases\\Case 1\\Export\\Production00001\\loadfile.dat",
            "relativityPassword": "password1234",
            "relativityVersion": "10.2",
            "workspaceArtifactId": 1018980
        },
        "moduleSettings": {
            "fieldsSettings": {
                "FieldList": [
                    {
                        "identifier": true,
                        "loadfileColumn": "DOCID",
                        "workspaceColumn": "Control Number"
                    }
                ],
                "metadataProfileName": "Default"
            },
            "relativitySettings": {
                "username": "jsmith@example.com",
                "webServiceUrl": "https://relativity.example.com/relativitywebapi"
            },
            "uploadSettings": {
                "nativeCopyMode": 1,
                "overwriteMode": 0
            }
        },
        "moduleType": "LOADFILE_IMPORT"
    },
    "description": "Load to Relativity",
    "fileName": "RelativityClient.nuixscript/headlessLauncher",
    "language": "PYTHON"
}
````




# License

```
Copyright 2019 Nuix

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
