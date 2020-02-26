package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nuix.relativityclient.relativitytypes.Field;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FieldsSettings {
    private static final Logger LOGGER = LogManager.getLogger(FieldsSettings.class.getName());

    @JsonIgnore
    private List<FieldMapping> mappingList;

    @JsonProperty("metadataProfileName")
    private String metadataProfileName;

    FieldsSettings() {
        mappingList = new ArrayList<>();
    }

    @JsonProperty("FieldList")
    public List<FieldMapping> getMappingList() {
        return mappingList;
    }

    @JsonProperty("FieldList")
    void setMappingList(List<FieldMapping> mappingList) {
        this.mappingList = mappingList;
    }

    @JsonIgnore
    public Set<Field> getFields() {
        return mappingList.stream().map(FieldMapping::getField).collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<String> getMetadataItemNames() {
        return mappingList.stream().map(FieldMapping::getMetadataItemName).collect(Collectors.toSet());
    }

    @JsonIgnore
    void addFieldMappings(FieldMapping ...fieldMappings) {
        mappingList.addAll(Arrays.asList(fieldMappings));
    }

    @JsonIgnore
    void removeFieldMappings(FieldMapping ...fieldMappings) {
        mappingList.removeAll(Arrays.asList(fieldMappings));
    }

    @JsonIgnore
    public String getMetadataProfileName() {
        return metadataProfileName;
    }

    @JsonIgnore
    void setMetadataProfileName(String metadataProfileName) {
        this.metadataProfileName = metadataProfileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        FieldsSettings fieldsSettings = (FieldsSettings) obj;

        boolean isMappingsEqual = Objects.equals(getMappingList(), fieldsSettings.getMappingList());
        boolean isMetadataProfileEqual = Objects.equals(getMetadataProfileName(),  fieldsSettings.getMetadataProfileName());

        return isMappingsEqual && isMetadataProfileEqual;
    }

    @Override
    public int hashCode() {
        String stringReducedFieldMappings = getMappingList().stream().map(fieldMapping -> {
            String fieldName = fieldMapping.getField().getRelativityTextIdentifier();
            String metadataItemName = fieldMapping.getMetadataItemName();

            return fieldName + " " + metadataItemName;
        }).reduce("", String::concat);

        return Objects.hash(stringReducedFieldMappings, getMetadataProfileName());
    }
}
