package com.nuix.relativityclient.clients;

import com.nuix.relativityclient.enums.NativeFileCopyMode;
import com.nuix.relativityclient.enums.OverwriteMode;
import com.nuix.relativityclient.model.FieldMapping;
import com.nuix.relativityclient.model.ModuleSettings;
import com.nuix.relativityclient.relativitytypes.FieldColumn;
import com.nuix.relativityclient.utils.ApplicationLogger;
import com.nuix.relativityclient.utils.JdbcClient;
import nuix.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import static com.nuix.relativityclient.enums.StandardLoadfileColumn.TEXTPATH;

public class RelativityJdbcUploadClient implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(RelativityJdbcUploadClient.class.getName());

    private ApplicationLogger applicationLogger;
    private ModuleSettings moduleSettings;
    private JdbcClient jdbcClient;
    private ProductionSet productionSet;
    private Map<String, MetadataItem> metadataItemMap;
    private Set<String> documentsWithWarnings;
    private Set<String> documentsWithErrors;
    private int caseArtifactId;

    private Map<String,String> beginGroupIds;
    private Map<String,String> endGroupIds;
    private Map<String,String> docIds;

    private boolean copyNative;
    private boolean hasNative;
    private boolean copyText;
    private int textFieldArtifactId;
    private int workspaceArtifactId;
    private int folderArtifactId;

    public RelativityJdbcUploadClient(ApplicationLogger applicationLogger, JdbcClient jdbcClient, ModuleSettings moduleSettings, ProductionSet productionSet, MetadataProfile metadataProfile, int workspaceArtifactId, int folderArtifactId) {
        this.applicationLogger = applicationLogger;
        this.moduleSettings = moduleSettings;
        this.jdbcClient = jdbcClient;
        this.productionSet = productionSet;
        metadataItemMap=new HashMap<>();
        if (metadataProfile!=null) {
            for (MetadataItem metadataItem : metadataProfile.getMetadata()) {
                metadataItemMap.put(metadataItem.getName(), metadataItem);
            }
        }
        documentsWithWarnings=new HashSet<>();
        documentsWithErrors=new HashSet<>();
        this.workspaceArtifactId = workspaceArtifactId;
        this.folderArtifactId = folderArtifactId;
    }

    private void analyzeFamilyRelationships(){

        docIds = new HashMap<>();
        beginGroupIds = new HashMap<>();
        endGroupIds = new HashMap<>();

        for (ProductionSetItem productionSetItem : productionSet.getProductionSetItems()) {
            String docId = productionSetItem.getDocumentNumber().toString();
            docIds.put(productionSetItem.getItem().getGuid(),docId);

            String topLevelGuid = productionSetItem.getItem().getTopLevelItem().getGuid();
            String beginGroupId = beginGroupIds.get(topLevelGuid);
            if (beginGroupId==null || beginGroupId.compareTo(docId)>0){
                beginGroupIds.put(topLevelGuid,docId);
            }

            String endGroupId = endGroupIds.get(topLevelGuid);
            if (endGroupId==null || endGroupId.compareTo(docId)<0){
                endGroupIds.put(topLevelGuid,docId);
            }

        }
    }


    private Object getFieldValue(ProductionSetItem productionSetItem, String metadataItemName) {
        String docId = "";

        try {
            docId = productionSetItem.getDocumentNumber().toString();
            switch (metadataItemName) {
                case "DOCID":
                    return productionSetItem.getDocumentNumber().toString();
                case "PARENT_DOCID":
                    Item parentItem = productionSetItem.getItem().getParent();
                    while (true) {
                        if (parentItem == null) {
                            break;
                        }
                        String parentDocId = docIds.get(parentItem.getGuid());
                        if (parentDocId != null) {
                            return parentDocId;
                        }
                        parentItem = parentItem.getParent();
                    }
                    return "";

                case "ATTACH_DOCID":
                    List<String> attachDocIds = new ArrayList<>();
                    for (Item descendant : productionSetItem.getItem().getDescendants()) {
                        String descendantDocId = docIds.get(descendant.getGuid());
                        if (descendantDocId != null) {
                            attachDocIds.add(descendantDocId);
                        }
                    }
                    return String.join(", ", attachDocIds);

                case "BEGINBATES":
                    return productionSetItem.getDocumentNumber().toString();
                case "ENDBATES":
                    return productionSetItem.getDocumentNumber().toString();
                case "BEGINGROUP":
                    return beginGroupIds.get(productionSetItem.getItem().getTopLevelItem().getGuid());
                case "ENDGROUP":
                    return endGroupIds.get(productionSetItem.getItem().getTopLevelItem().getGuid());
                case "PAGECOUNT":
                    try {
                        return productionSetItem.getPrintPreview().getPageCount();
                    } catch (Exception e) {
                        // Document does not have page count
                        return 1;
                    }
                case "TEXTPATH":
                    return productionSetItem.getItem().getTextObject().toString();
                default:
                    MetadataItem metadataItem = metadataItemMap.get(metadataItemName);
                    if (metadataItem == null) {
                        applicationLogger.logUniqueError("Field " + metadataItemName + " not found in metadata profile");
                        documentsWithWarnings.add(docId);
                        return null;
                    } else {
                        return metadataItem.evaluateUnformatted(productionSetItem.getItem());
                    }
            }
        } catch (Exception e) {
            applicationLogger.logUniqueError("Document " + docId + " warning, Cannot evaluate metadata field " + metadataItemName + ", " + e.getMessage(), e);
            documentsWithWarnings.add(docId);
        }
        return null;
    }


    @Override
    public void run() {
        try {
            applicationLogger.logInfo("Starting JDBC upload");

            jdbcClient.setWorkspaceArtifactId(workspaceArtifactId);

            Map<String, FieldColumn> fieldColumns = jdbcClient.getFieldColumns();

            copyNative = moduleSettings.getUploadSettings().getNativeFileCopyMode().equals(NativeFileCopyMode.COPY_FILES);
            hasNative = copyNative || moduleSettings.getUploadSettings().getNativeFileCopyMode().equals(NativeFileCopyMode.SET_FILE_LINKS);
            copyText=false;
            for(FieldMapping fieldMapping :moduleSettings.getFieldsSettings().getMappingList()) {
                if (fieldMapping.getMetadataItemName().equals("TEXTPATH")){
                    String extractedTextFieldName = fieldMapping.getField().getRelativityTextIdentifier();
                    copyText=true;
                    textFieldArtifactId = fieldColumns.get(extractedTextFieldName).getArtifactId();
                    break;
                }
            }

            try {
                jdbcClient.fetchWorkspaceResourceServer(workspaceArtifactId);
                applicationLogger.logInfo("Workspace server: "+jdbcClient.getServerName());
                if (copyNative){
                    applicationLogger.logInfo("File share URL: "+jdbcClient.getFileShareUrl());
                }
                if (copyText){
                    applicationLogger.logInfo("Data grid URL: "+jdbcClient.getDataGridUrl());
                }
            }catch (SQLException e) {
                applicationLogger.logUniqueError("Cannot get workspace resource server, Upload of text and native files will be disabled, "+e.getMessage(), e);
                documentsWithWarnings.addAll(docIds.values());
                copyText=false;
                copyNative=false;
            }

            caseArtifactId = jdbcClient.getCaseArtifactId();
            applicationLogger.logInfo("Case ArtifactID: "+caseArtifactId);



            FieldColumn identifierFieldColumn=null;
            for (FieldMapping fieldMapping : moduleSettings.getFieldsSettings().getMappingList()){
                if (fieldMapping.getIsIdentifier()){
                    applicationLogger.logInfo("Field is identifier");
                    identifierFieldColumn=fieldColumns.get(fieldMapping.getField().getRelativityTextIdentifier());
                    if (identifierFieldColumn==null){
                        throw new Exception("Specified identifier field "+fieldMapping.getField().getRelativityTextIdentifier()+" could not be found in Relativity workspace");
                    }
                }
            }

            if (identifierFieldColumn==null){
                throw new Exception("Mapping missing identifier field. Please add identifier field to mapping, for example DocID or Control Number.");
            }

            applicationLogger.logInfo("Detected "+fieldColumns.size()+" document field columns in Relativity");

            List<Integer> folderAncestors = jdbcClient.getItemAncestors(folderArtifactId);
            folderAncestors.add(folderArtifactId);
            applicationLogger.logInfo("Got destination folder");

            analyzeFamilyRelationships();
            if (beginGroupIds.size()==1) {
                applicationLogger.logInfo("Detected " + beginGroupIds.size() + " document family in the production set");
            } else {
                applicationLogger.logInfo("Detected " + beginGroupIds.size() + " document families in the production set");
            }


            int currentCount=0;
            int totalCount = productionSet.getProductionSetItems().size();

            int currentOperationCount=0;
            int totalOperationsCount=2;
            if (copyText){
                totalOperationsCount++;
            }
            if (copyNative){
                totalOperationsCount++;
            }


            Map<ProductionSetItem, Integer> productionSetItemsArtifactIds = new HashMap<>();
            if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.APPEND)) {

                for (ProductionSetItem productionSetItem : productionSet.getProductionSetItems()) {
                    applicationLogger.logProgress("Reserving artifact IDs", currentOperationCount, totalOperationsCount, currentCount, totalCount);
                    currentCount++;
                    String docId = productionSetItem.getDocumentNumber().toString();
                    try {
                        int artifactId = jdbcClient.createArtifact(10, folderArtifactId, caseArtifactId, docId);
                        productionSetItemsArtifactIds.put(productionSetItem, artifactId);
                    } catch (SQLException e) {
                        applicationLogger.logUniqueError("Document " + docId + " error, Cannot create artifact, " + e.getMessage());
                        documentsWithErrors.add(docId);
                    }
                }
            } else if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)){
                for (ProductionSetItem productionSetItem : productionSet.getProductionSetItems()) {
                    applicationLogger.logProgress("Mapping documents to artifact IDs",currentOperationCount, totalOperationsCount, currentCount,totalCount);
                    currentCount++;
                    String docId = productionSetItem.getDocumentNumber().toString();
                    try {
                        int artifactId = jdbcClient.getDocumentArtifactId(docId, identifierFieldColumn);
                        productionSetItemsArtifactIds.put(productionSetItem, artifactId);
                    } catch (SQLException e) {
                        applicationLogger.logUniqueError("Document " + docId + " error, Cannot find artifact, " + e.getMessage());
                        documentsWithErrors.add(docId);
                    }
                }
            } else {
                throw new Exception("Unsupported overwrite mode "+moduleSettings.getUploadSettings().getOverwriteMode());
            }

            String operationName="Unknown";
            if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.APPEND)) {
                operationName="Inserting documents";
            } else if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)){
                operationName="Updating documents";
            }
            currentCount=0;
            totalCount=productionSetItemsArtifactIds.size();

            for (ProductionSetItem productionSetItem : productionSetItemsArtifactIds.keySet()){
                applicationLogger.logProgress(operationName,currentOperationCount, totalOperationsCount, currentCount,totalCount);
                currentCount++;

                String docId = productionSetItem.getDocumentNumber().toString();
                Item item = productionSetItem.getItem();
                int artifactId = productionSetItemsArtifactIds.get(productionSetItem);
                Map<String,Object> fields=new HashMap<>();

                for (FieldMapping fieldMapping : moduleSettings.getFieldsSettings().getMappingList()){
                    String fieldName = fieldMapping.getField().getRelativityTextIdentifier();
                    FieldColumn fieldColumn = fieldColumns.get(fieldName);

                    if (fieldColumn!=null) {
                        if (fieldMapping.getMetadataItemName().equals(TEXTPATH.toString())){
                            // Text extraction is handled separately
                            continue;
                        }

                        Object fieldValue = getFieldValue(productionSetItem, fieldMapping.getMetadataItemName());

                        // Skip single-/multi- choice fields from document table
                        if (fieldColumn.getColumnType().equals("CodeText") || fieldColumn.getColumnType().equals("MultiText")) {
                            continue;
                        }
                        fields.put(fieldColumn.getColumnName(), fieldValue);
                    } else {
                        applicationLogger.logUniqueError("Field name "+fieldName+" could not be found in SQL database.");
                        documentsWithWarnings.add(docId);
                    }
                }
                try {
                    Set<String> incorrectlyFormattedFields=null;

                    if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.APPEND)) {
                        incorrectlyFormattedFields = jdbcClient.addDocumentRecord(artifactId, folderArtifactId, fields,hasNative,docId+"."+item.getType().getPreferredExtension(),(hasNative?true:null),(hasNative?item.getType().getLocalisedName(Locale.ENGLISH):null));
                        jdbcClient.setAncestors(artifactId,folderAncestors);

                    } else if (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)){
                        incorrectlyFormattedFields = jdbcClient.updateDocumentRecord(artifactId, fields ,hasNative);
                    }

                    if (incorrectlyFormattedFields.size() != 0) {
                        applicationLogger.logUniqueError("Document " + docId + " warning, Incorrect format for field(s) " + String.join(", ", incorrectlyFormattedFields));
                        documentsWithWarnings.add(docId);
                    }

                    // Handle single-/multi- choice fields
                    for (FieldMapping fieldMapping : moduleSettings.getFieldsSettings().getMappingList()){
                        String fieldName = fieldMapping.getField().getRelativityTextIdentifier();
                        FieldColumn fieldColumn = fieldColumns.get(fieldName);

                        if (fieldColumn!=null) {
                            if (fieldMapping.getMetadataItemName().equals(TEXTPATH.toString())){
                                // Text extraction is handled separately
                                continue;
                            }

                            Object fieldValue = getFieldValue(productionSetItem, fieldMapping.getMetadataItemName());

                            if (fieldColumn.getColumnType().equals("CodeText")) {
                                try {
                                    Set<String> values = new HashSet<>();
                                    if (fieldValue!=null){
                                        values.add(fieldValue.toString());
                                    }

                                    jdbcClient.setDocumentChoiceValues(artifactId, fieldColumn.getArtifactId(), fieldColumn.getCodeTypeId(), caseArtifactId, values, (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)));
                                } catch (SQLException e) {
                                    applicationLogger.logUniqueError("Document " + docId + " warning, Cannot set single choice field " + fieldName + ", " + e.getMessage(), e);
                                    documentsWithWarnings.add(docId);
                                }
                            } else if (fieldColumn.getColumnType().equals("MultiText")) {
                                try {
                                    Set<String> values = new HashSet<>();
                                    if (fieldValue!=null){
                                        for(String value : fieldValue.toString().split(", ")) {
                                            if (value.trim().length()>0){
                                                values.add(value);
                                            }
                                        }
                                    }

                                    jdbcClient.setDocumentChoiceValues(artifactId, fieldColumn.getArtifactId(), fieldColumn.getCodeTypeId(), caseArtifactId, values, (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)));
                                } catch (SQLException e) {
                                    applicationLogger.logUniqueError("Document " + docId + " warning, Cannot set multi choice field " + fieldName + ", " + e.getMessage(), e);
                                    documentsWithWarnings.add(docId);
                                }
                            }
                        }
                    }

                }
                catch (SQLException e){
                    applicationLogger.logUniqueError("Document "+docId +" error, "+e.getMessage(),e);
                    documentsWithErrors.add(docId);
                }
            }


            if (copyText) {

                String databaseName = "EDDS"+workspaceArtifactId;

                Path textBasePath = Paths.get(jdbcClient.getDataGridUrl(),
                        databaseName,
                        ("relativity_" + jdbcClient.getServerName() + "_" + databaseName + "_10").toLowerCase(),
                        "Fields.ExtractedText",
                        java.util.UUID.randomUUID().toString().substring(0,3));
                try {
                    applicationLogger.logInfo("Got text folder "+textBasePath.toFile().getAbsolutePath());
                    Files.createDirectories(textBasePath);

                    int itemsInFolderCount=0;
                    Path textBaseSubPath=null;

                    currentCount=0;
                    for (ProductionSetItem productionSetItem : productionSetItemsArtifactIds.keySet()) {
                        applicationLogger.logProgress("Uploading text",currentOperationCount, totalOperationsCount, currentCount,totalCount);
                        currentCount++;

                        if (itemsInFolderCount%10000 == 0){
                            // Add a maximum of 10k files to a folder
                            textBaseSubPath=textBasePath.resolve(java.util.UUID.randomUUID().toString().substring(0,3));
                            Files.createDirectories(textBaseSubPath);
                        }
                        itemsInFolderCount++;

                        String docId = productionSetItem.getDocumentNumber().toString();
                        int artifactId = productionSetItemsArtifactIds.get(productionSetItem);
                        Item item = productionSetItem.getItem();

                        Path textFilePath = textBaseSubPath.resolve(item.getGuid()+".txt");
                        try {
                            List<CharSequence> charSequences = new ArrayList<>();
                            charSequences.add(item.getTextObject());
                            Files.write(textFilePath, charSequences, StandardCharsets.UTF_16LE);
                            long fileSize = Files.size(textFilePath);
                            jdbcClient.setDataGridFileMapping(artifactId,textFieldArtifactId,textFilePath.toUri().toString(),fileSize,(moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)));
                        } catch (IOException e){
                            applicationLogger.logUniqueError("Document " + docId + " warning, Cannot upload extracted text, " + e.getMessage(), e);
                            documentsWithWarnings.add(docId);
                        }
                    }

                } catch (IOException e){
                    applicationLogger.logException("Cannot upload extracted text, " + e.getMessage(),e);
                }
            }



            if (copyNative) {

                String databaseName = "EDDS"+workspaceArtifactId;

                Path nativeBasePath = Paths.get(jdbcClient.getFileShareUrl(),
                        databaseName,
                        "RV_"+java.util.UUID.randomUUID().toString().toLowerCase());
                try {
                    applicationLogger.logInfo("Got natives folder " + nativeBasePath.toFile().getAbsolutePath());
                    Files.createDirectories(nativeBasePath);

                    int itemsInFolderCount=0;
                    Path nativeBaseSubpath=null;

                    currentCount=0;
                    for (ProductionSetItem productionSetItem : productionSetItemsArtifactIds.keySet()) {
                        applicationLogger.logProgress("Uploading natives",currentOperationCount, totalOperationsCount, currentCount,totalCount);
                        currentCount++;

                        if (itemsInFolderCount%10000 == 0){
                            // Add a maximum of 10k files to a folder
                            nativeBaseSubpath=nativeBasePath.resolve(java.util.UUID.randomUUID().toString().substring(0,3));
                            Files.createDirectories(nativeBaseSubpath);
                        }
                        itemsInFolderCount++;

                        String docId = productionSetItem.getDocumentNumber().toString();
                        int artifactId = productionSetItemsArtifactIds.get(productionSetItem);
                        Item item = productionSetItem.getItem();

                        Path nativeFilePath = nativeBaseSubpath.resolve(item.getGuid());
                        try {
                            item.getBinary().getBinaryData().copyTo(nativeFilePath);
                            long fileSize = item.getBinary().getBinaryData().getLength();

                            jdbcClient.setFileshareFileMapping(artifactId,item.getGuid(),docId+"."+item.getType().getPreferredExtension(),"DOC"+artifactId+"_NATIVE",nativeFilePath.toString(),fileSize,(moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)) );
                        } catch (Exception e){
                            applicationLogger.logUniqueError("Document " + docId + " warning, Cannot upload native, " + e.getMessage(), e);
                            documentsWithWarnings.add(docId);
                        }
                    }

                } catch (IOException e){
                    applicationLogger.logException("Cannot upload natives, " + e.getMessage(),e);
                    documentsWithWarnings.addAll(docIds.values());
                }
            } else if (hasNative) {

                currentCount=0;
                for (ProductionSetItem productionSetItem : productionSetItemsArtifactIds.keySet()) {
                    applicationLogger.logProgress("Setting native links",currentOperationCount, totalOperationsCount, currentCount,totalCount);
                    currentCount++;

                    String docId = productionSetItem.getDocumentNumber().toString();
                    int artifactId = productionSetItemsArtifactIds.get(productionSetItem);
                    Item item = productionSetItem.getItem();

                    try {
                        if (!item.getBinary().isStored()) {
                            applicationLogger.logUniqueError("Document " + docId + " warning, Cannot upload native, Native is not stored");
                            documentsWithWarnings.add(docId);
                            continue;
                        }

                        Path storedPath = item.getBinary().getStoredPath();
                        if (storedPath == null) {
                            applicationLogger.logUniqueError("Document " + docId + " warning, Cannot upload native, Native is not stored outside case");
                            documentsWithWarnings.add(docId);
                            continue;
                        }
                        long fileSize = item.getBinary().getBinaryData().getLength();
                        jdbcClient.setFileshareFileMapping(artifactId, item.getGuid(), docId + "." + item.getType().getPreferredExtension(), "DOC" + artifactId + "_NATIVE", storedPath.toString(), fileSize, (moduleSettings.getUploadSettings().getOverwriteMode().equals(OverwriteMode.OVERLAY)));
                    } catch (Exception e) {
                        applicationLogger.logUniqueError("Document " + docId + " warning, Cannot upload native, " + e.getMessage(), e);
                        documentsWithWarnings.add(docId);
                    }
                }
            }
            applicationLogger.logInfo("----------------------");
            applicationLogger.logInfo("Total documents: "+productionSet.getItems().size());
            if (documentsWithWarnings.size()>0) {
                applicationLogger.logInfo("Documents with warnings: " + documentsWithWarnings.size());
            }

            if (documentsWithErrors.size()>0) {
                applicationLogger.logInfo("Total with errors: " + documentsWithErrors.size());
            }

            applicationLogger.notifyComplete();

        } catch (Exception e) {
            applicationLogger.logException("Unexpected error occurred: " + e.getMessage(),e);
            applicationLogger.notifyComplete();
        }
    }
}
