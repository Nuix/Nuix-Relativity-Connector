package com.nuix.relativityclient.interfaces;

import com.nuix.relativityclient.model.FieldMapping;
import com.nuix.relativityclient.model.ModuleSettings;
import com.nuix.relativityclient.relativitytypes.Workspace;
import nuix.Case;
import nuix.Utilities;

import java.util.List;


public interface SettingsUpdateListener {
    void setUtilities(Utilities utilities);
    void setNuixCase(Case nuixCase);
    void setScriptFolder(String scriptFolder);

    void readSettings(ModuleSettings settings);

    void setWorkspace(Workspace workspace);
    void setMetadataProfileName(String metadataProfileName);

    void setMappingsList(List<FieldMapping> fieldMappings);
    void addMappings(FieldMapping...fieldMappings);
    void removeMappings(FieldMapping ...fieldMappings);
}