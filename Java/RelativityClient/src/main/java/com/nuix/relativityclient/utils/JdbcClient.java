package com.nuix.relativityclient.utils;

import com.nuix.relativityclient.relativitytypes.FieldColumn;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.Date;
import java.sql.*;
import java.util.*;

public class JdbcClient {
    private static final Logger LOGGER = LogManager.getLogger(JdbcClient.class.getName());
    private Connection connection;
    private String databaseName;
    private String fileShareUrl;
    private String dataGridUrl;
    private String serverName;

    public JdbcClient(String sqlServer, Integer port, String instance, String domain, String username, String password) throws SQLException {

        LOGGER.info("Creating JdbcClient");
        String connectionString = "jdbc:jtds:sqlserver://"+sqlServer;
        if (port != null){
            connectionString+=":"+port;
        }

        if (instance != null && instance.length()>0){
            connectionString+=";instance="+instance;
        }

        if (domain != null && domain.length()>0){
            connectionString+=";domain="+domain;
        }
        connection = DriverManager.getConnection(connectionString,username,password);
    }

    public String getFileShareUrl() {
        return fileShareUrl;
    }

    public String getDataGridUrl() {
        return dataGridUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public int getCaseArtifactId() throws SQLException {
        String selectQuery = "SELECT [ArtifactID]" +
                " FROM "+databaseName+".[Artifact]" +
                " WHERE [ArtifactTypeID]=8";

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(selectQuery);
        while (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Could not find case Artifact ID");
    }

    public void setWorkspaceArtifactId(int artifactId) throws SQLException {

        databaseName = "[EDDS"+artifactId+"].[EDDSDBO]";
        LOGGER.info("Set database "+databaseName);
    }

    public void fetchWorkspaceResourceServer(int artifactId) throws SQLException {
        String selectQuery = "SELECT [ServerID]"+
                ",[DefaultFileLocationCodeArtifactID]" +
                ",[DataGridFileShareResourceServerArtifactID]"+
                " FROM [EDDS].[eddsdbo].[Case]" +
                " WHERE [ArtifactID]=?";

        PreparedStatement statement = connection.prepareStatement(selectQuery);
        statement.setInt(1,artifactId);

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            int serverId=resultSet.getInt(1);
            int fileShareId=resultSet.getInt(2);
            int dataGridId=resultSet.getInt(3);

            serverName = getResourceServerProperty(serverId,"Name");
            fileShareUrl= getResourceServerProperty(fileShareId,"URL");
            dataGridUrl = getResourceServerProperty(dataGridId,"URL");
            break;
        }
    }

    private String getResourceServerProperty(int artifactId,String propertyName) throws SQLException {
        String selectQuery = "SELECT ["+propertyName+"]"+
                " FROM [EDDS].[eddsdbo].[ResourceServer]" +
                " WHERE [ArtifactID]=?";

        PreparedStatement statement = connection.prepareStatement(selectQuery);
        statement.setInt(1,artifactId);

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getString(1);
        }
        return null;
    }

    public Map<String, FieldColumn> getFieldColumns() throws SQLException {
        Map<String, FieldColumn> fieldColumns = new HashMap<>();
        String selectQuery = "SELECT [Field].[ArtifactID]" +    // 1
                ",[Field].[FieldArtifactTypeID]" +              // 2
                ",[Field].[DisplayName]" +                      // 3
                ",[Field].[ArtifactViewFieldID]" +              // 4
                ",[ArtifactViewField].[ColumnName]" +           // 5
                ",[ArtifactViewField].[ItemListType]" +         // 6
                ",[Field].[CodeTypeID]" +                       // 7
                " FROM "+databaseName+".[Field] AS [Field]" +
                " INNER JOIN  "+databaseName+".[ArtifactViewField] as [ArtifactViewField]" +
                " ON [Field].[ArtifactViewFieldID]=[ArtifactViewField].[ArtifactViewFieldID]" +
                " WHERE [Field].[FieldArtifactTypeID] = 10";

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(selectQuery);
        while (resultSet.next()) {
            int fieldArtifactId = resultSet.getInt(1);
            String fieldName = resultSet.getString(3);
            String columnName= resultSet.getString(5);
            String columnType= resultSet.getString(6);
            int codeTypeId= resultSet.getInt(7);
            FieldColumn fieldColumn;
            if (codeTypeId>0) {
                fieldColumn = new FieldColumn(columnName, columnType, fieldArtifactId, codeTypeId);
            }else {
                fieldColumn = new FieldColumn(columnName, columnType, fieldArtifactId);
            }
            fieldColumns.put(fieldName,fieldColumn);
        }
        return fieldColumns;
    }

    public int getDocumentArtifactId(String docId, FieldColumn identifier) throws SQLException {
        String selectQuery = "SELECT [ArtifactID]" +        // 1
                " FROM " + databaseName + ".[Document]" +
                " WHERE [" + identifier.getColumnName() + "] = ?";

        PreparedStatement statement = connection.prepareStatement(selectQuery);
        statement.setString(1, docId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Document does not exist in workspace");
    }


    public int createArtifact(int artifactType,int parentArtifactId,int containerId, String textIdentifier) throws SQLException {
        String insertQuery = "INSERT INTO "+databaseName + ".[Artifact]"+
                " ([ArtifactTypeID]"+                // 1
                ",[ParentArtifactID]"+               // 2
                ",[AccessControlListID]"+            // 3
                ",[AccessControlListIsInherited]"+   // 4
                ",[CreatedOn]"+                      // 5
                ",[LastModifiedOn]"+                 // 6
                ",[LastModifiedBy]"+                 // 7
                ",[CreatedBy]"+                      // 8
                ",[ContainerID]"+                    // 9
                ",[Keywords]"+                       // 10
                ",[Notes]"+                          // 11
                ",[DeleteFlag]"+                     // 12
                ",[TextIdentifier])"+                // 13
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(insertQuery,Statement.RETURN_GENERATED_KEYS);

        statement.setInt(1, artifactType);
        statement.setInt(2, parentArtifactId);
        statement.setInt(3, 1);
        statement.setInt(4, 1);
        statement.setDate(5, new Date(System.currentTimeMillis()));
        statement.setDate(6, new Date(System.currentTimeMillis()));
        statement.setInt(7, 777);
        statement.setInt(8, 777);
        statement.setInt(9, containerId);
        statement.setString(10, "");
        statement.setString(11, "");
        statement.setBoolean(12, false);
        statement.setString(13, textIdentifier);
        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating Artifact failed, no rows affected.");
        }

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
            else {
                throw new SQLException("Creating Artifact failed, no ID obtained.");
            }
        }
    }


    public void setDocumentChoiceValues(int documentArtifactId, int fieldArtifactId, int codeTypeId, int containerId, Set<String> values, boolean overlay) throws SQLException {
        if (overlay) {
            String deleteStatement = "DELETE FROM " + databaseName + ".[ZCodeArtifact_" + codeTypeId + "]" +
                    "WHERE [AssociatedArtifactID]=?";
            PreparedStatement deleteAssociationStatement = connection.prepareStatement(deleteStatement);
            deleteAssociationStatement.setInt(1, documentArtifactId);
            deleteAssociationStatement.executeUpdate();
        }

        for (String value : values) {
            String selectQuery = "SELECT [ArtifactID]" +
                    " FROM " + databaseName + ".[Code]" +
                    " WHERE [CodeTypeID]=? AND [Name]=?";

            PreparedStatement selectCodeId = connection.prepareStatement(selectQuery);
            selectCodeId.setInt(1, codeTypeId);
            selectCodeId.setString(2, value);

            ResultSet resultSet = selectCodeId.executeQuery();
            Integer choiceArtifactId = null;
            while (resultSet.next()) {
                choiceArtifactId = resultSet.getInt(1);
            }

            // Track new choice option
            if (choiceArtifactId == null) {
                choiceArtifactId = createArtifact(7, containerId, containerId, value.toString());

                String insertQuery = "INSERT INTO " + databaseName + ".[Code]" +
                        " ([ArtifactID]" +           // 1
                        ",[CodeTypeID]" +            // 2
                        ",[Order]" +                 // 3
                        ",[IsActive]" +              // 4
                        ",[UpdateInSearchEngine]" +  // 5
                        ",[Name])" +                 // 6
                        "VALUES (?, ?, ?, ?, ?, ?)";

                PreparedStatement insertNewChoiceStatement = connection.prepareStatement(insertQuery);
                insertNewChoiceStatement.setInt(1, choiceArtifactId);
                insertNewChoiceStatement.setInt(2, codeTypeId);
                insertNewChoiceStatement.setInt(3, 0);
                insertNewChoiceStatement.setBoolean(4, true);
                insertNewChoiceStatement.setBoolean(5, false);
                insertNewChoiceStatement.setString(6, value);
                insertNewChoiceStatement.executeUpdate();
            }

            // Set choice on document
            String insertQuery = "INSERT INTO " + databaseName + ".[ZCodeArtifact_" + codeTypeId + "]" +
                    " ([CodeArtifactID]" +           // 1
                    ",[AssociatedArtifactID])" +     // 2
                    "VALUES (?, ?)";
            PreparedStatement insertChoiceToDocumentStatement = connection.prepareStatement(insertQuery);
            insertChoiceToDocumentStatement.setInt(1, choiceArtifactId);
            insertChoiceToDocumentStatement.setInt(2, documentArtifactId);
            insertChoiceToDocumentStatement.executeUpdate();
        }
    }

    public Set<String> addDocumentRecord(int artifactId, int parentArtifactId, Map<String, Object> fields, boolean hasNative, String fileIcon, Boolean supportedByViewer, String relativityNativeType) throws SQLException {
        Set<String> incorrectlyFormattedFields = new HashSet<>();
        String insertQuery = "INSERT INTO " + databaseName + ".[Document]" +
                    " ([ArtifactID]" +                   // 1
                    ",[AccessControlListID_D]" +         // 2
                    ",[ParentArtifactID_D]" +            // 3
                    ",[HasNative]" +                     // 4
                    ",[HasAnnotations]" +                // 5
                    ",[HasInlineTags]" +                 // 6
                    ",[FileIcon]" +                      // 7
                    ",[SupportedByViewer]" +             // 8
                    ",[RelativityNativeType]";           // 9

            String valuesSubQuery = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?";
            for (String fieldName : fields.keySet()) {
                insertQuery += ",[" + fieldName + "]";
                valuesSubQuery += ", ?";
            }
            insertQuery += ") " + valuesSubQuery + ")";

        PreparedStatement statement = connection.prepareStatement(insertQuery);

        statement.setInt(1, artifactId);
        statement.setInt(2, 1);
        statement.setInt(3, parentArtifactId);
        statement.setBoolean(4, hasNative);
        statement.setBoolean(5, false);
        statement.setBoolean(6, false);
        statement.setString(7, fileIcon);
        statement.setObject(8, supportedByViewer);
        statement.setObject(9, relativityNativeType);

        int fieldPosition=10;

        for (String fieldName : fields.keySet()){
            Object fieldValue = fields.get(fieldName);
            try {
                statement.setObject(fieldPosition, fieldValue);
            } catch (SQLException e1){
                try{
                    statement.setString(fieldPosition, fieldValue.toString());
                } catch (SQLException e2){
                    // Cannot set value as object
                    incorrectlyFormattedFields.add(fieldName);
                    statement.setObject(fieldPosition, null);
                }
            }
            fieldPosition++;
        }

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Inserting Document failed, no rows affected.");
        }

        return incorrectlyFormattedFields;
    }

    public List<Integer> getItemAncestors(int artifactId)  throws SQLException {
        String selectQuery = "SELECT [AncestorArtifactID]" +
                " FROM " + databaseName + ".[ArtifactAncestry]" +
                " WHERE [ArtifactID]=?";

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, artifactId);

        ResultSet resultSet = selectStatement.executeQuery();
        List<Integer> ancestors = new ArrayList<>();

        while (resultSet.next()) {
            ancestors.add(resultSet.getInt(1));
        }

        return ancestors;
    }

    public void setAncestors(int artifactId, List<Integer> ancestorIds) throws SQLException {
        String insertQuery = "INSERT INTO " + databaseName + ".[ArtifactAncestry]" +
                " ([ArtifactID]" +                   // 1
                ",[AncestorArtifactID])" +           // 2
                " VALUES (?, ?)";

        PreparedStatement statement = connection.prepareStatement(insertQuery);
        for (int ancestorId : ancestorIds) {
            statement.setInt(1, artifactId);
            statement.setInt(2, ancestorId);
            statement.addBatch();
        }

        statement.executeBatch();
    }


    public void setDataGridFileMapping(int artifactId, int fieldArtifactId, String fileLocation, long fileSize, boolean overlay) throws SQLException {
        if (overlay) {
            String dropQuery = "DELETE FROM " + databaseName + ".[DataGridFileMapping]" +
                    "WHERE [ArtifactID]=? AND [FieldArtifactID]=?";
            PreparedStatement deleteMappingstatement = connection.prepareStatement(dropQuery);
            deleteMappingstatement.setInt(1, artifactId);
            deleteMappingstatement.setInt(2, fieldArtifactId);
            deleteMappingstatement.executeUpdate();
        }

        String insertQuery = "INSERT INTO " + databaseName + ".[DataGridFileMapping]" +
                " ([ArtifactID]" +                  // 1
                ",[FieldArtifactID]" +              // 2
                ",[FileLocation]" +                 // 3
                ",[FileSize])" +                    // 4
                " VALUES (?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(insertQuery);
        statement.setInt(1, artifactId);
        statement.setInt(2, fieldArtifactId);
        statement.setString(3, fileLocation);
        statement.setLong(4, fileSize);
        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Setting data grid file mapping failed, no rows affected.");
        }
    }

    public void setFileshareFileMapping(int artifactId, String guid, String filename, String identifier, String fileLocation, long fileSize, boolean overlay) throws SQLException {
        if (overlay) {
            String dropQuery = "DELETE FROM " + databaseName + ".[File]" +
                    "WHERE [DocumentArtifactID]=?";
            PreparedStatement deleteMappingstatement = connection.prepareStatement(dropQuery);
            deleteMappingstatement.setInt(1, artifactId);
            deleteMappingstatement.executeUpdate();
        }

        String insertQuery = "INSERT INTO " + databaseName + ".[File]" +
                " ([Guid]" +                  // 1
                ",[DocumentArtifactID]" +     // 2
                ",[Filename]" +               // 3
                ",[Order]" +                  // 4
                ",[Type]" +                   // 5
                ",[Rotation]" +               // 6
                ",[Identifier]" +             // 7
                ",[Location]" +               // 8
                ",[InRepository]" +           // 9
                ",[Size]" +                   // 10
                ",[Details]" +                // 11
                ",[Billable])" +              // 12
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(insertQuery);
        statement.setString(1, guid);
        statement.setInt(2, artifactId);
        statement.setString(3, filename);
        statement.setInt(4, 0);
        statement.setInt(5, 0);
        statement.setInt(6, -1);
        statement.setString(7, identifier);
        statement.setString(8, fileLocation);
        statement.setBoolean(9, true);
        statement.setLong(10, fileSize);
        statement.setObject(11, null);
        statement.setBoolean(12, true);
        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Setting fileshare file mapping failed, no rows affected.");
        }
    }



    public Set<String> updateDocumentRecord(int artifactId, Map<String, Object> fields, boolean hasNative) throws SQLException {
        Set<String> incorrectlyFormattedFields = new HashSet<>();
        String updateQuery = "UPDATE " + databaseName + ".[Document] SET" +
                    "[HasNative] = ?";                             // 1
            for (String fieldName : fields.keySet()) {
                updateQuery += ",[" + fieldName + "] = ?";

            }
            updateQuery += "WHERE [ArtifactID]=?";

        PreparedStatement statement = connection.prepareStatement(updateQuery);

        statement.setBoolean(1, hasNative);
        int fieldPosition=2;

        for (String fieldName : fields.keySet()){
            Object fieldValue = fields.get(fieldName);
            try {
                statement.setObject(fieldPosition, fieldValue);
            } catch (SQLException e1){
                try{
                    statement.setString(fieldPosition, fieldValue.toString());
                } catch (SQLException e2){
                    // Cannot set value as object
                    incorrectlyFormattedFields.add(fieldName);
                    statement.setObject(fieldPosition, null);
                }
            }
            fieldPosition++;
        }

        statement.setInt(fieldPosition, artifactId);

                int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Updating Document failed, no rows affected.");
        }

        return incorrectlyFormattedFields;
    }

}
