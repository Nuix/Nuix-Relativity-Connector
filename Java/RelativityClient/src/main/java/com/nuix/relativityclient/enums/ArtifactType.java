package com.nuix.relativityclient.enums;

//Relativity ArtifactType Enumeration
public enum ArtifactType {
    DOCUMENT(10),
    CUSTOM_PAGE(1000023);

    private int id;

    ArtifactType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
