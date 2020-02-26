package com.nuix.relativityclient.relativitytypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {
    @JsonProperty("Version")
    private String version;

    public String getVersion() {
        return version;
    }
}
