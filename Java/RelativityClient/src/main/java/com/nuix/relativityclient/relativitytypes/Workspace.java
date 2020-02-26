package com.nuix.relativityclient.relativitytypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown =  true)
public class Workspace {

    private int artifactId;
    private String relativityTextIdentifier;
    private List<Field> fields;

    @JsonCreator
    public Workspace(@JsonProperty("Artifact ID") int artifactId) {
        this.artifactId = artifactId;
        this.fields = new ArrayList<>();
    }

    @JsonProperty("Relativity Text Identifier")
    public void setRelativityTextIdentifier(String relativityTextIdentifier) {
        this.relativityTextIdentifier = relativityTextIdentifier;
    }

    @JsonProperty("Artifact ID")
    public int getArtifactId() {
        return artifactId;
    }

    @JsonIgnore
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @JsonIgnore
    public List<Field> getFields() {
        return fields;
    }

    @JsonIgnore
    public String getRelativityTextIdentifier() {
        return relativityTextIdentifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        Workspace workspace = (Workspace) obj;

        boolean isArtifactIdEqual = Objects.equals(getArtifactId(), workspace.getArtifactId());
        boolean isRelativityTextIdentifierEqual = Objects.equals(getRelativityTextIdentifier(), workspace.getRelativityTextIdentifier());

        return isArtifactIdEqual && isRelativityTextIdentifierEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArtifactId(), getRelativityTextIdentifier());
    }
}
