package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.*;
import com.nuix.relativityclient.relativitytypes.Field;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldMapping {

    @JsonIgnore
    private String metadataItemName;
    @JsonIgnore
    private Field field;

    @JsonCreator
    FieldMapping(@JsonProperty("loadfileColumn") String metadataItemName, @JsonProperty("workspaceColumn") String field) {
        this.metadataItemName = metadataItemName;
        this.field = new Field(field);
    }

    public FieldMapping(String metadataItemName, Field field) {
        this.metadataItemName = metadataItemName;
        this.field = field;
    }

    @JsonProperty("loadfileColumn")
    public String getMetadataItemName() {
        return metadataItemName;
    }

    @JsonProperty("workspaceColumn")
    private String getFieldName() {
        return field.getRelativityTextIdentifier();
    }

    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    @JsonProperty("identifier")
    public boolean getIsIdentifier() {
        return field.isIdentifier();
    }

    @JsonProperty("identifier")
    public void setIsIdentifier(boolean identifier) {
        field.setIsIdentifier(identifier);
    }

    @JsonIgnore
    public Field getField() {
        return field;
    }

    @JsonIgnore
    void setField(Field field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        FieldMapping fieldMapping = (FieldMapping) obj;

        boolean metadataNamesEqual = Objects.equals(getMetadataItemName(), fieldMapping.getMetadataItemName());
        boolean fieldNamesEqual = Objects.equals(getFieldName(), fieldMapping.getFieldName());

        return metadataNamesEqual && fieldNamesEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadataItemName(), getFieldName());
    }
}
