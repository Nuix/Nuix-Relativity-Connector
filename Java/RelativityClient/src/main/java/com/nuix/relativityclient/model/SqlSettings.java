package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SqlSettings {

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("serverPort")
    private int serverPort;

    @JsonProperty("instanceName")
    private String instanceName;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("username")
    private String username;

    @JsonIgnore
    private char[] password;

    SqlSettings() {
        serverName = "";
        serverPort = 1443;
        instanceName = "";
        domain = "";
        username = "";
        password = new char[] {};
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        SqlSettings sqlSettings = (SqlSettings) obj;

        boolean isServerNameEquals = Objects.equals(getServerName(), sqlSettings.getServerName());
        boolean isServerPortEquals = Objects.equals(getServerPort(), sqlSettings.getServerPort());
        boolean isInstanceNameEquals = Objects.equals(getInstanceName(), sqlSettings.getInstanceName());
        boolean isDomainNameEquals = Objects.equals(getDomain(), sqlSettings.getDomain());
        boolean isUsernameEquals = Objects.equals(getUsername(), sqlSettings.getUsername());

        return isServerNameEquals && isServerPortEquals && isInstanceNameEquals && isDomainNameEquals && isUsernameEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerName(), getServerPort(), getInstanceName(), getDomain(), getUsername());
    }
}
