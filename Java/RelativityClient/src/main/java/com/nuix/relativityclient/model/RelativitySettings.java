package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RelativitySettings {

    @JsonProperty("webServiceUrl")
    private String webServiceUrl;

    @JsonProperty("username")
    private String username;

    @JsonIgnore
    private char[] password;

    @JsonIgnore
    private String version;

    RelativitySettings() {
        webServiceUrl = "";
        username = "";
        password = new char[] {};
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public void setWebServiceUrl(String webServiceUrl) {
        this.webServiceUrl = webServiceUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        RelativitySettings relativitySettings = (RelativitySettings) obj;

        boolean isWebServiceUrlEquals = Objects.equals(getWebServiceUrl(), relativitySettings.getWebServiceUrl());
        boolean isUsernameEquals = Objects.equals(getUsername(), relativitySettings.getUsername());

        return isWebServiceUrlEquals && isUsernameEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWebServiceUrl(), getUsername());
    }
}
