package com.nuix.relativityclient.relativitytypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Folder {

    private int artifactId;
    private String name;

    @JsonCreator
    public Folder(@JsonProperty("ArtifactID") int artifactId) {
        this.artifactId = artifactId;
    }

    @JsonProperty("Name")
    public void setName(String name) {
        this.name = name;
    }

    public int getArtifactId() {
        return artifactId;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        Folder folder = (Folder) obj;

        boolean isArtifactIdEqual = Objects.equals(getArtifactId(), folder.getArtifactId());
        boolean isNameEqual = Objects.equals(getName(), folder.getName());

        return isArtifactIdEqual && isNameEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArtifactId(), getName());
    }
}
