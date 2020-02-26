package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nuix.relativityclient.enums.StandardLoadfileColumn;
import com.nuix.relativityclient.interfaces.SettingsUpdateListener;
import com.nuix.relativityclient.relativitytypes.Field;
import com.nuix.relativityclient.relativitytypes.Folder;
import com.nuix.relativityclient.relativitytypes.Workspace;
import nuix.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModuleSettings implements SettingsUpdateListener {
    private static final Logger LOGGER = LogManager.getLogger(ModuleSettings.class.getName());

    @JsonIgnore
    private List<SettingsUpdateListener> observers;
    @JsonIgnore
    private String loadfileLocation;

    @JsonProperty("fieldsSettings")
    private FieldsSettings fieldsSettings;
    @JsonProperty("uploadSettings")
    private UploadSettings uploadSettings;
    @JsonProperty("relativitySettings")
    private RelativitySettings relativitySettings;
    @JsonProperty("sqlSettings")
    private SqlSettings sqlSettings;

    @JsonIgnore
    private Utilities utilities;
    @JsonIgnore
    private Workspace workspace;
    @JsonIgnore
    private Folder folder;
    @JsonIgnore
    private Case nuixCase;
    @JsonIgnore
    private String scriptFolder;


    public ModuleSettings(Utilities utilities, Case nuixCase, String scriptFolder) {
        this.utilities = utilities;
        this.nuixCase = nuixCase;
        this.scriptFolder=scriptFolder;

        observers = new ArrayList<>();
        uploadSettings = new UploadSettings();
        sqlSettings = new SqlSettings();
        relativitySettings = new RelativitySettings();
        fieldsSettings = new FieldsSettings();
    }

    @JsonCreator
    public ModuleSettings() {
        observers = new ArrayList<>();
        uploadSettings = new UploadSettings();
        sqlSettings = new SqlSettings();
        relativitySettings = new RelativitySettings();
        fieldsSettings = new FieldsSettings();
    }

    @Override
    public void setUtilities(Utilities utilities) {
        LOGGER.info("Setting utilities: " + utilities);

        this.utilities = utilities;
        observers.forEach(observer -> observer.setUtilities(utilities));
    }

    @Override
    public void setNuixCase(Case nuixCase) {
        if (nuixCase!=null) {
            LOGGER.info("Setting nuixCase: " + nuixCase.getName());
        }

        this.nuixCase = nuixCase;
        observers.forEach(observer -> observer.setNuixCase(nuixCase));
    }
    public Case getNuixCase(){
        return nuixCase;
    }


    @Override
    public void setScriptFolder(String scriptFolder) {
        LOGGER.info("Setting scriptFolder: " + scriptFolder);

        this.scriptFolder = scriptFolder;
        observers.forEach(observer -> observer.setScriptFolder(scriptFolder));

    }

    public String getScriptFolder() {
        return scriptFolder;
    }


    @Override
    public void readSettings(ModuleSettings settings) {
        uploadSettings = settings.uploadSettings;
        sqlSettings = settings.sqlSettings;
        relativitySettings = settings.relativitySettings;
        fieldsSettings = settings.fieldsSettings;

        //To make observers run through their logic for each case
        setMetadataProfileName(fieldsSettings.getMetadataProfileName());
        setMappingsList(fieldsSettings.getMappingList());

        observers.forEach(observer -> observer.readSettings(settings));
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        LOGGER.info("Setting workspace: " + (workspace != null ? workspace.getArtifactId() : "NULL") + " and notifying observers");

        //Whenever selecting a workspace, filter the list of fieldMappings against it
        List<FieldMapping> fieldMappings = getFieldsSettings().getMappingList();
        filterFieldMappings(fieldMappings, workspace);

        this.workspace = workspace;
        observers.forEach(observer -> observer.setWorkspace(workspace));
    }

    @Override
    public void setMetadataProfileName(String metadataProfileName) {
        LOGGER.info("Setting metadataProfile: " + metadataProfileName + " and notifying observers");

        //Whenever selecting a metadataProfile, filter the list of fieldMappings against it
        List<FieldMapping> fieldMappings = getFieldsSettings().getMappingList();
        filterFieldMappings(fieldMappings, metadataProfileName);

        fieldsSettings.setMetadataProfileName(metadataProfileName);
        observers.forEach(observer -> observer.setMetadataProfileName(metadataProfileName));
    }

    @Override
    public void setMappingsList(List<FieldMapping> fieldMappings) {
        LOGGER.info("Setting mapping list and notifying observers");
        if (fieldMappings.isEmpty()) {
            return;
        }

        //Whenever setting a mappingList (from fieldMapping JSON or settingsJSON), filter out those that do not belong to
        //currently selected metadataProfile/Workspace and replace MetadataItems/Fields of those that do exist
        String metadataProfileName = fieldsSettings.getMetadataProfileName();
        filterFieldMappings(fieldMappings, metadataProfileName);
        filterFieldMappings(fieldMappings, workspace);

        fieldsSettings.setMappingList(fieldMappings);
        observers.forEach(observer -> observer.setMappingsList(fieldMappings));
    }

    @Override
    public void addMappings(FieldMapping ...fieldMappings) {
        LOGGER.info("Adding mappings and notifying observers");

        fieldsSettings.addFieldMappings(fieldMappings);
        observers.forEach(observer -> observer.addMappings(fieldMappings));
    }

    @Override
    public void removeMappings(FieldMapping ...fieldMappings) {
        LOGGER.info("Removing mappings and notifying observers");

        fieldsSettings.removeFieldMappings(fieldMappings);
        observers.forEach(observer -> observer.removeMappings(fieldMappings));
    }

    public List<String> getMetadataProfileItemsWithStandardLoadfileColumns(String metadataProfileName) {
        List<String> metadataItemNames = new ArrayList<>();

        if (metadataProfileName != null) {
            //Add all StandardLoadFileColumn names
            metadataItemNames.addAll(StandardLoadfileColumn.valuesToString);

            //Add all metadataItems name from metadataProfile
            MetadataProfile metadataProfile = utilities.getMetadataProfileStore().getMetadataProfile(metadataProfileName);
            if (metadataProfile!=null) {
                List<String> itemNames = metadataProfile.getMetadata().stream().map(MetadataItem::getName).sorted().collect(Collectors.toList());
                metadataItemNames.addAll(itemNames);
            }
        }

        return metadataItemNames;
    }

    public UserDataStore getUtilities() {
        return utilities;
    }

    public String getLoadfileLocation() {
        return loadfileLocation;
    }

    public void setLoadfileLocation(String loadfileLocation) {
        this.loadfileLocation = loadfileLocation;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        LOGGER.info("Setting folder: " + (folder != null ? folder.getArtifactId() : "NULL"));
        this.folder = folder;
    }

    public SqlSettings getSqlSettings() {
        return sqlSettings;
    }

    public RelativitySettings getRelativitySettings() {
        return relativitySettings;
    }

    public FieldsSettings getFieldsSettings() {
        return fieldsSettings;
    }

    public UploadSettings getUploadSettings() {
        return uploadSettings;
    }

    public void addObserver(SettingsUpdateListener observer) {
        observers.add(observer);
    }

    private void filterFieldMappings(List<FieldMapping> fieldMappings, String metadataProfileName) {
        //If metadataProfile == null, NO-OP (Allow FieldMapping to exist without being filtered when no MetadataProfile selected)
        if (metadataProfileName != null) {
            Set<String> metadataItemNames = new HashSet<>(getMetadataProfileItemsWithStandardLoadfileColumns(metadataProfileName));

            //Remove any fieldMapping which does not contain a metadataItem in currently selected MetadataProfile
            fieldMappings.removeIf(fieldMapping -> {
                String metadataName = fieldMapping.getMetadataItemName();
                return !metadataItemNames.contains(metadataName);
            });
        }
    }

    private void filterFieldMappings(List<FieldMapping> fieldMappings, Workspace workspace) {
        //If workspace == null, NO-OP (Allow FieldMapping to exist without being filtered when no Workspace selected)
        if (workspace != null) {
            List<Field> fields = workspace.getFields();
            Map<String, Field> nameToField = fields.stream().collect(Collectors.toMap(Field::getRelativityTextIdentifier, field -> field, (field1, field2) -> field1));

            //Remove any fieldMapping which does not contain a field in currently selected Workspace
            //Replace Field of old fieldMapping with new Field (old Field may be half-object (from JSON) or have different contents)
            fieldMappings.removeIf(fieldMapping -> {
                String fieldName = fieldMapping.getField().getRelativityTextIdentifier();
                Field field = nameToField.get(fieldName);
                if (field != null) {
                    fieldMapping.setField(field);
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        ModuleSettings moduleSettings = (ModuleSettings) obj;

        boolean isRelativitySettingsEquals = Objects.equals(getRelativitySettings(), moduleSettings.getRelativitySettings());
        boolean isFieldsSettingsEquals = Objects.equals(getFieldsSettings(), moduleSettings.getFieldsSettings());
        boolean isSqlSettingsEquals = Objects.equals(getSqlSettings(), moduleSettings.getSqlSettings());
        boolean isUploadSettingsEquals = Objects.equals(getUploadSettings(), moduleSettings.getUploadSettings());

        return isRelativitySettingsEquals && isFieldsSettingsEquals && isSqlSettingsEquals && isUploadSettingsEquals;
    }

    @Override
    public int hashCode() {
        int relativitySettingsHash = getRelativitySettings().hashCode();
        int fieldsSettingsHash = getFieldsSettings().hashCode();
        int sqlSettingsHash = getSqlSettings().hashCode();
        int uploadSettingsHash = getUploadSettings().hashCode();

        return Objects.hash(relativitySettingsHash, fieldsSettingsHash, sqlSettingsHash, uploadSettingsHash);
    }
}
