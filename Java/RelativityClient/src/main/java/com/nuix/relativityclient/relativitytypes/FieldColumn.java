package com.nuix.relativityclient.relativitytypes;

public class FieldColumn {

    private String columnName;
    private String columnType;
    private int artifactId;
    private Integer codeTypeId;

    public FieldColumn(String columnName, String columnType, int artifactId){
        this.columnName = columnName;
        this.columnType = columnType;
        this.artifactId = artifactId;
    }

    public FieldColumn(String columnName, String columnType, int artifactId, int codeTypeId){
        this(columnName,columnType,artifactId);
        this.codeTypeId = codeTypeId;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public int getArtifactId() {
        return artifactId;
    }

    public Integer getCodeTypeId(){
        return codeTypeId;
    }
}
