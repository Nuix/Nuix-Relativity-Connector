package com.nuix.relativityclient.model;

import com.nuix.relativityclient.enums.ModuleType;

public class ClientLauncherSettings {
    private String relativityVersion;
    private String clientSettingsPath;
    private int workspaceArtifactId;
    private int folderArtifactId;
    private String relativityPassword;
    private String loadfilePath;
    private ModuleType moduleType;
    private String scriptPath;

    public String getRelativityVersion() {
        return relativityVersion;
    }

    public void setRelativityVersion(String relativityVersion) {
        this.relativityVersion = relativityVersion;
    }

    public String getClientSettingsPath() {
        return clientSettingsPath;
    }

    public void setClientSettingsPath(String clientSettingsPath) {
        this.clientSettingsPath = clientSettingsPath;
    }

    public int getWorkspaceArtifactId() {
        return workspaceArtifactId;
    }

    public void setWorkspaceArtifactId(int workspaceArtifactId) {
        this.workspaceArtifactId = workspaceArtifactId;
    }

    public int getFolderArtifactId() {
        return folderArtifactId;
    }

    public void setFolderArtifactId(int folderArtifactId) {
        this.folderArtifactId = folderArtifactId;
    }

    public String getRelativityPassword() {
        return relativityPassword;
    }

    public void setRelativityPassword(String relativityPassword) {
        this.relativityPassword = relativityPassword;
    }

    public String getLoadfilePath() {
        return loadfilePath;
    }

    public void setLoadfilePath(String loadfilePath) {
        this.loadfilePath = loadfilePath;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    public void setModuleType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }
}
