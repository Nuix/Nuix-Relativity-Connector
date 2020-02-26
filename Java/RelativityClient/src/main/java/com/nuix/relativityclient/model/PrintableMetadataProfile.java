package com.nuix.relativityclient.model;

import nuix.MetadataProfile;

public class PrintableMetadataProfile {
    private MetadataProfile metadataProfile;
    public PrintableMetadataProfile(MetadataProfile metadataProfile){
        this.metadataProfile=metadataProfile;
    }

    @Override
    public String toString() {
        return metadataProfile.getName();
    }

    public MetadataProfile getMetadataProfile() {
        return metadataProfile;
    }
}
