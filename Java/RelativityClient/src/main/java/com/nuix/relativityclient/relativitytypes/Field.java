package com.nuix.relativityclient.relativitytypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nuix.relativityclient.enums.ArtifactType;
import com.nuix.relativityclient.enums.FieldType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Field {

    public static final String[] fields = {
        "Artifact ID",
        "Relativity Text Identifier",
        "Is Identifier",
        "Field Type ID",
        "Length"
    };
    public static final String condition = "'Object Type' == " + ArtifactType.DOCUMENT.getId();
    public static final Set<String> blackList = new HashSet<>(Arrays.asList(
        "Artifact ID",
        "Batch",
        "Batch::Assigned To",
        "Batch::Batch Set",
        "Batch::Status",
        "Folder Name",
        "FileIcon",
        "Has Images",
        "Has Inline Tags",
        "Has Native",
        "Image QC Status",
        "Markup Set - Primary",
        "Production Errors",
        "Relativity Image Count",
        "Relativity Native Time Zone Offset",
        "Relativity Native Type",
        "Supported By Viewer",
        "System Created By",
        "System Created On",
        "System Last Modified By",
        "System Last Modified On"
    ));

    @JsonProperty("Artifact ID")
    private int artifactId;

    @JsonProperty("Is Identifier")
    private boolean isIdentifier;

    @JsonProperty("Length")
    private int length;

    @JsonIgnore
    private String relativityTextIdentifier;

    @JsonIgnore
    private FieldType type;

    @JsonCreator
    public Field(@JsonProperty("Relativity Text Identifier") String name) {
        relativityTextIdentifier = name;
    }

    @JsonProperty("Field Type ID")
    public void setType(int fieldTypeId) {
        this.type = FieldType.getFromId(fieldTypeId);
    }

    @JsonIgnore
    public int getArtifactId() {
        return artifactId;
    }

    @JsonIgnore
    public String getRelativityTextIdentifier() {
        return relativityTextIdentifier;
    }

    @JsonIgnore
    public FieldType getType() {
        return type;
    }

    @JsonIgnore
    public int getLength() {
        return length;
    }

    @JsonIgnore
    public boolean isIdentifier() {
        return isIdentifier;
    }

    @JsonIgnore
    public void setIsIdentifier(boolean isIdentifier) {
        this.isIdentifier = isIdentifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        Field field = (Field) obj;
        boolean isArtifactIdEqual = Objects.equals(getArtifactId(), field.getArtifactId());
        boolean isNameEqual = Objects.equals(getRelativityTextIdentifier(), field.getRelativityTextIdentifier());

        return isArtifactIdEqual && isNameEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArtifactId(), getRelativityTextIdentifier());
    }
}
