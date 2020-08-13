/*
 * Created by JFormDesigner on Thu Oct 17 16:38:22 EDT 2019
 */

package com.nuix.relativityclient.ui.relativitymapping;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuix.relativityclient.enums.StandardLoadfileColumn;
import com.nuix.relativityclient.interfaces.MappingFrame;
import com.nuix.relativityclient.interfaces.SettingsUpdateListener;
import com.nuix.relativityclient.interfaces.SimpleDocumentListener;
import com.nuix.relativityclient.model.FieldMapping;
import com.nuix.relativityclient.model.FieldsSettings;
import com.nuix.relativityclient.model.ModuleSettings;
import com.nuix.relativityclient.relativitytypes.Field;
import com.nuix.relativityclient.relativitytypes.Workspace;
import com.nuix.relativityclient.ui.JsonFileFilter;
import com.nuix.relativityclient.ui.NotificationDialogs;
import com.nuix.relativityclient.ui.PlaceholderTextField;
import com.nuix.relativityclient.ui.SaveJFileChooser;
import com.nuix.relativityclient.ui.cellrenderers.FieldCellRenderer;
import com.nuix.relativityclient.ui.cellrenderers.FieldMappingCellRenderer;
import net.miginfocom.swing.MigLayout;
import nuix.Case;
import nuix.Utilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class RelativityMappingFrame extends JFrame implements SettingsUpdateListener, MappingFrame {
    private static final Logger LOGGER = LogManager.getLogger(RelativityMappingFrame.class.getName());
    private static final String DEFAULT_SAVE_NAME = "FieldsMapping.json";

    private File lastAccessedMappingFile;

    private ModuleSettings moduleSettings;

    private DefaultListModel<String> metadataItemsModel;
    private DefaultListModel<Field> fieldsModel;
    private DefaultListModel<FieldMapping> mappingModel;

    public RelativityMappingFrame(ModuleSettings settings) {
        initComponents();
        moduleSettings = settings;

        metadataItemsModel = new DefaultListModel<>();
        listLoadFileColumns.setModel(metadataItemsModel);

        fieldsModel = new DefaultListModel<>();
        listWorkspaceColumns.setModel(fieldsModel);

        mappingModel = new DefaultListModel<>();
        listFieldMapping.setModel(mappingModel);

        pack();
        setMinimumSize(getSize());
        addDocumentListeners();
    }

    @Override
    public void setUtilities(Utilities utilities) {
        //NO-OP
    }

    @Override
    public void setNuixCase(Case nuixCase) {
        //NO-OP
    }

    @Override
    public void setScriptFolder(String scriptFolder) {
        //NO-OP
    }

    @Override
    public void readSettings(ModuleSettings settings) {
        //NO-OP
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        LOGGER.info("Setting Workspace Fields");

        String metadataProfileName = moduleSettings.getFieldsSettings().getMetadataProfileName();
        filterModels(metadataProfileName, workspace);

        if (workspace != null) {
            labelWorkspaceName.setText(workspace.getRelativityTextIdentifier());
        } else {
            labelWorkspaceName.setText("");
        }
    }

    @Override
    public void setMetadataProfileName(String metadataProfileName) {
        LOGGER.info("Setting metadata items");

        Workspace workspace = moduleSettings.getWorkspace();
        filterModels(metadataProfileName, workspace);

        labelMetadataProfileName.setText(metadataProfileName);
    }

    @Override
    public void setMappingsList(List<FieldMapping> fieldMappings) {
        LOGGER.info("Setting field mapping items");

        String metadataProfileName = moduleSettings.getFieldsSettings().getMetadataProfileName();
        Workspace workspace = moduleSettings.getWorkspace();
        filterModels(metadataProfileName, workspace);
    }

    //Removes selected metadata and field then adds metadata-field to mapping
    @Override
    public void addMappings(FieldMapping ...fieldMappings) {
        for (FieldMapping fieldMapping : fieldMappings) {
            String metadataItemName = fieldMapping.getMetadataItemName();
            Field field = fieldMapping.getField();

            if (metadataItemsModel.contains(metadataItemName) && fieldsModel.contains(field)) {
                metadataItemsModel.removeElement(metadataItemName);
                fieldsModel.removeElement(field);

                mappingModel.addElement(fieldMapping);
            }
        }

        buttonSave.setEnabled(!mappingModel.isEmpty());
    }

    @Override
    public void removeMappings(FieldMapping ...fieldMappings) {
        for (FieldMapping fieldMapping : fieldMappings) {
            mappingModel.removeElement(fieldMapping);
        }

        filterMetadataItemsModel(moduleSettings.getFieldsSettings().getMetadataProfileName());
        filterFieldsModel(moduleSettings.getWorkspace());

        buttonSave.setEnabled(!mappingModel.isEmpty());
    }

    private void buttonSaveActionPerformed() {
        JFileChooser fileChooser = new SaveJFileChooser(lastAccessedMappingFile);
        fileChooser.setDialogTitle("Save Mapping");
        fileChooser.setSelectedFile(new File(DEFAULT_SAVE_NAME));

        JsonFileFilter jsonFileFilter = new JsonFileFilter();
        fileChooser.setFileFilter(jsonFileFilter);

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            lastAccessedMappingFile = fileChooser.getSelectedFile();

            try {
                LOGGER.info("Saving relativity mapping to file: " + lastAccessedMappingFile.getAbsolutePath());

                FieldsSettings fieldsSettings = moduleSettings.getFieldsSettings();

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(lastAccessedMappingFile, fieldsSettings);

            } catch (IOException e) {
                NotificationDialogs.showErrorMessage(this, e, LOGGER);
            }
        }
    }

    private void buttonLoadActionPerformed() {
        JFileChooser fileChooser = new JFileChooser(lastAccessedMappingFile);
        fileChooser.setDialogTitle("Load Mapping");

        JsonFileFilter jsonFileFilter = new JsonFileFilter();
        fileChooser.setFileFilter(jsonFileFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                lastAccessedMappingFile = fileChooser.getSelectedFile();
                LOGGER.info("Loading relativity mapping from file: " + lastAccessedMappingFile.getAbsolutePath());

                ObjectMapper objectMapper = new ObjectMapper();
                FieldsSettings fieldsSettings = objectMapper.readValue(lastAccessedMappingFile, FieldsSettings.class);

                moduleSettings.setMappingsList(fieldsSettings.getMappingList());
            } catch (JsonMappingException | JsonParseException e) {
                String title = "Invalid Mapping File";
                String message = "This is not a valid Fields Mapping json file.";

                NotificationDialogs.showErrorMessage(this, title, message, e, LOGGER);

            } catch (IOException e) {
                NotificationDialogs.showErrorMessage(this, e, LOGGER);
            }
        }
    }

    private void buttonAddAutoMappedActionPerformed() {
        FieldMapping[] fieldMappings = getAutoMappedFieldMappings().toArray(new FieldMapping[0]);

        moduleSettings.addMappings(fieldMappings);
    }

    private void buttonAddMappingActionPerformed() {
        String metadataItemName = listLoadFileColumns.getSelectedValue();
        Field field = listWorkspaceColumns.getSelectedValue();
        if (field == null || metadataItemName == null) {
            return;
        }

        FieldMapping fieldMapping = new FieldMapping(metadataItemName, field);
        moduleSettings.addMappings(fieldMapping);
    }

    private void buttonRemoveMappingActionPerformed() {
        List<FieldMapping> fieldMappings = listFieldMapping.getSelectedValuesList();
        if (fieldMappings.isEmpty()) {
            return;
        }

        FieldMapping[] mappingsArray = fieldMappings.toArray(new FieldMapping[0]);
        moduleSettings.removeMappings(mappingsArray);
    }

    private void listValueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        boolean shouldEnable = !(listLoadFileColumns.isSelectionEmpty() || listWorkspaceColumns.isSelectionEmpty());
        buttonAddMapping.setEnabled(shouldEnable);
    }

    private void listMetadataToFieldValueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        boolean shouldEnable = !listFieldMapping.isSelectionEmpty();
        buttonRemoveMapping.setEnabled(shouldEnable);
    }

    private List<FieldMapping> getAutoMappedFieldMappings() {
        Map<String, Field> nameToField = Collections.list(fieldsModel.elements()).stream().collect(Collectors.toMap(Field::getRelativityTextIdentifier, field -> field));
        Set<String> metadataItemNames = new HashSet<>(Collections.list(metadataItemsModel.elements()));

        Set<String> fieldNames = nameToField.keySet();
        //Contains only those names shared by both MetadataItems and Fields
        List<FieldMapping> fieldMappings = metadataItemNames.stream()
            .filter(fieldNames::contains)
            .map(metadataItemName -> {
                Field field = nameToField.get(metadataItemName);
                return new FieldMapping(metadataItemName, field);
            }).collect(Collectors.toList());

        Arrays.stream(StandardLoadfileColumn.values())
            .filter(column -> metadataItemNames.contains(column.toString()))
            .filter(column -> {
                if (column.equals(StandardLoadfileColumn.DOCID)) {
                    String docIdMetadataItemName = StandardLoadfileColumn.DOCID.toString();
                    nameToField.values().stream().filter(Field::isIdentifier).findFirst().ifPresent(identifierField -> fieldMappings.add(new FieldMapping(docIdMetadataItemName, identifierField)));
                    return false;
                }
                return true;
            })//Go through remainder StandardLoadfileColumns and find matches to add
            .forEach(column -> {
                String metadataItemName = column.toString();
                String fieldName = column.getFieldEquivalent();

                Field field = nameToField.get(fieldName);
                if (field != null) {
                    fieldMappings.add(new FieldMapping(metadataItemName, field));
                }
            });

        return fieldMappings;
    }

    private void buttonConfirmActionPerformed() {
        dispose();
    }

    private void buttonCancelActionPerformed() {
        dispose();
    }

    private void addDocumentListeners() {
        textFieldSearchWorkspaceColumns.getDocument().addDocumentListener((SimpleDocumentListener) e -> filterFieldsModel(moduleSettings.getWorkspace()));
        textFieldSearchLoadFileColumns.getDocument().addDocumentListener((SimpleDocumentListener) e -> filterMetadataItemsModel(moduleSettings.getFieldsSettings().getMetadataProfileName()));
    }

    private void filterModels(String metadataProfileName, Workspace workspace) {
        filterFieldsModel(workspace);
        filterMetadataItemsModel(metadataProfileName);
        filterMappingModel();
    }

    private void filterFieldsModel(Workspace workspace) {
        fieldsModel.clear();
        if (workspace == null) {
            return;
        }

        String searchText = textFieldSearchWorkspaceColumns.getText().toLowerCase();

        List<Field> availableFields = new ArrayList<>(workspace.getFields());
        availableFields.removeAll(moduleSettings.getFieldsSettings().getFields());

        availableFields.stream()
            .filter(field -> field.getRelativityTextIdentifier().toLowerCase().contains(searchText))
            .forEach(field -> fieldsModel.addElement(field));
    }

    private void filterMetadataItemsModel(String metadataProfileName) {
        metadataItemsModel.clear();
        if (metadataProfileName == null) {
            return;
        }

        String searchText = textFieldSearchLoadFileColumns.getText().toLowerCase();

        List<String> availableMetadataItemNames = moduleSettings.getMetadataProfileItemsWithStandardLoadfileColumns(metadataProfileName);
        availableMetadataItemNames.removeAll(moduleSettings.getFieldsSettings().getMetadataItemNames());

        availableMetadataItemNames.stream()
            .filter(metadataItemName -> metadataItemName.toLowerCase().contains(searchText))
            .forEach(metadataItemName -> metadataItemsModel.addElement(metadataItemName));
    }

    private void filterMappingModel() {
        mappingModel.clear();
        moduleSettings.getFieldsSettings().getMappingList().forEach(fieldMapping -> mappingModel.addElement(fieldMapping));
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panelDetails = new JPanel();
        JLabel labelWorkspace = new JLabel();
        labelWorkspaceName = new JLabel();
        JLabel labelMetadataProfile = new JLabel();
        labelMetadataProfileName = new JLabel();
        JPanel panelMapping = new JPanel();
        JPanel panelLoadFileColumns = new JPanel();
        textFieldSearchLoadFileColumns = new PlaceholderTextField("Search");
        JScrollPane scrollPaneLoadFileColumns = new JScrollPane();
        listLoadFileColumns = new JList<>();
        listLoadFileColumns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panelWorkspaceColumns = new JPanel();
        textFieldSearchWorkspaceColumns = new PlaceholderTextField("Search");
        JScrollPane scrollPaneWorkspaceColumns = new JScrollPane();
        listWorkspaceColumns = new JList<>();
        listWorkspaceColumns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listWorkspaceColumns.setCellRenderer(new FieldCellRenderer());
        JPanel panelAddRemoveButtons = new JPanel();
        buttonAddAutoMapped = new JButton();
        buttonAddMapping = new JButton();
        buttonRemoveMapping = new JButton();
        JPanel panelColumnMapping = new JPanel();
        JScrollPane scrollPaneColumnMapping = new JScrollPane();
        listFieldMapping = new JList<>();
        listFieldMapping.setCellRenderer(new FieldMappingCellRenderer());
        buttonSave = new JButton();
        JButton buttonLoad = new JButton();
        JPanel buttonBar = new JPanel();
        JButton buttonConfirm = new JButton();
        JButton buttonCancel = new JButton();

        //======== this ========
        setIconImage(new ImageIcon(getClass().getResource("/NuixIcon.png")).getImage());
        setTitle("Fields Mapping");
        Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]",
            // rows
            "[]" +
            "[grow,fill]" +
            "[]"));

        //======== panelDetails ========
        {
            panelDetails.setBorder(new TitledBorder("Details"));
            panelDetails.setLayout(new MigLayout(
                "fillx,hidemode 3",
                // columns
                "[fill]para" +
                "[grow,fill]",
                // rows
                "[]" +
                "[]"));

            //---- labelWorkspace ----
            labelWorkspace.setText("Workspace:");
            panelDetails.add(labelWorkspace, "cell 0 0");

            //---- labelWorkspaceName ----
            labelWorkspaceName.setFont(labelWorkspaceName.getFont().deriveFont(labelWorkspaceName.getFont().getStyle() | Font.BOLD));
            panelDetails.add(labelWorkspaceName, "cell 1 0");

            //---- labelMetadataProfile ----
            labelMetadataProfile.setText("Metadata Profile:");
            panelDetails.add(labelMetadataProfile, "cell 0 1");

            //---- labelMetadataProfileName ----
            labelMetadataProfileName.setFont(labelMetadataProfileName.getFont().deriveFont(labelMetadataProfileName.getFont().getStyle() | Font.BOLD));
            panelDetails.add(labelMetadataProfileName, "cell 1 1");
        }
        contentPane.add(panelDetails, "cell 0 0");

        //======== panelMapping ========
        {
            panelMapping.setBorder(new TitledBorder("Load File to Workspace Mapping"));
            panelMapping.setLayout(new MigLayout(
                "fill,hidemode 3",
                // columns
                "[325,grow,fill]" +
                "[325,grow,fill]" +
                "[button,fill]" +
                "[325,grow,fill]",
                // rows
                "[]0" +
                "[grow,fill]" +
                "[]"));

            //======== panelLoadFileColumns ========
            {
                panelLoadFileColumns.setBorder(new TitledBorder("Load File Columns"));
                panelLoadFileColumns.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[grow,fill]",
                    // rows
                    "[]" +
                    "[grow,fill]"));
                panelLoadFileColumns.add(textFieldSearchLoadFileColumns, "cell 0 0");

                //======== scrollPaneLoadFileColumns ========
                {

                    //---- listLoadFileColumns ----
                    listLoadFileColumns.setVisibleRowCount(30);
                    listLoadFileColumns.addListSelectionListener(e -> listValueChanged(e));
                    scrollPaneLoadFileColumns.setViewportView(listLoadFileColumns);
                }
                panelLoadFileColumns.add(scrollPaneLoadFileColumns, "cell 0 1");
            }
            panelMapping.add(panelLoadFileColumns, "cell 0 1");

            //======== panelWorkspaceColumns ========
            {
                panelWorkspaceColumns.setBorder(new TitledBorder("Workspace Columns"));
                panelWorkspaceColumns.setLayout(new MigLayout(
                    "fill,hidemode 3",
                    // columns
                    "[fill]",
                    // rows
                    "[]" +
                    "[grow,fill]"));
                panelWorkspaceColumns.add(textFieldSearchWorkspaceColumns, "cell 0 0");

                //======== scrollPaneWorkspaceColumns ========
                {

                    //---- listWorkspaceColumns ----
                    listWorkspaceColumns.setVisibleRowCount(30);
                    listWorkspaceColumns.addListSelectionListener(e -> listValueChanged(e));
                    scrollPaneWorkspaceColumns.setViewportView(listWorkspaceColumns);
                }
                panelWorkspaceColumns.add(scrollPaneWorkspaceColumns, "cell 0 1");
            }
            panelMapping.add(panelWorkspaceColumns, "cell 1 1");

            //======== panelAddRemoveButtons ========
            {
                panelAddRemoveButtons.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "0[button,fill]0",
                    // rows
                    "[grow]" +
                    "[]" +
                    "[]" +
                    "[grow 75]"));

                //---- buttonAddAutoMapped ----
                buttonAddAutoMapped.setText(">>");
                buttonAddAutoMapped.addActionListener(e -> buttonAddAutoMappedActionPerformed());
                panelAddRemoveButtons.add(buttonAddAutoMapped, "cell 0 0");

                //---- buttonAddMapping ----
                buttonAddMapping.setText(">");
                buttonAddMapping.setEnabled(false);
                buttonAddMapping.addActionListener(e -> buttonAddMappingActionPerformed());
                panelAddRemoveButtons.add(buttonAddMapping, "cell 0 1");

                //---- buttonRemoveMapping ----
                buttonRemoveMapping.setText("<");
                buttonRemoveMapping.setEnabled(false);
                buttonRemoveMapping.addActionListener(e -> buttonRemoveMappingActionPerformed());
                panelAddRemoveButtons.add(buttonRemoveMapping, "cell 0 2");
            }
            panelMapping.add(panelAddRemoveButtons, "cell 2 1");

            //======== panelColumnMapping ========
            {
                panelColumnMapping.setBorder(new TitledBorder("Column Mapping"));
                panelColumnMapping.setLayout(new MigLayout(
                    "fill,hidemode 3",
                    // columns
                    "[grow,fill]" +
                    "[button,fill]" +
                    "[button,fill]",
                    // rows
                    "[grow,fill]" +
                    "[]0" +
                    "[]0" +
                    "[]"));

                //======== scrollPaneColumnMapping ========
                {

                    //---- listFieldMapping ----
                    listFieldMapping.setVisibleRowCount(30);
                    listFieldMapping.addListSelectionListener(e -> listMetadataToFieldValueChanged(e));
                    scrollPaneColumnMapping.setViewportView(listFieldMapping);
                }
                panelColumnMapping.add(scrollPaneColumnMapping, "cell 0 0 3 1");

                //---- buttonSave ----
                buttonSave.setText("Save");
                buttonSave.setEnabled(false);
                buttonSave.addActionListener(e -> buttonSaveActionPerformed());
                panelColumnMapping.add(buttonSave, "cell 1 2");

                //---- buttonLoad ----
                buttonLoad.setText("Load");
                buttonLoad.addActionListener(e -> buttonLoadActionPerformed());
                panelColumnMapping.add(buttonLoad, "cell 2 2");
            }
            panelMapping.add(panelColumnMapping, "cell 3 1");
        }
        contentPane.add(panelMapping, "cell 0 1");

        //======== buttonBar ========
        {
            buttonBar.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[button,fill]" +
                "[button,fill]",
                // rows
                "[]"));

            //---- buttonConfirm ----
            buttonConfirm.setText("OK");
            buttonConfirm.addActionListener(e -> buttonConfirmActionPerformed());
            buttonBar.add(buttonConfirm, "cell 0 0");

            //---- buttonCancel ----
            buttonCancel.setText("Cancel");
            buttonCancel.addActionListener(e -> buttonCancelActionPerformed());
            buttonBar.add(buttonCancel, "cell 1 0");
        }
        contentPane.add(buttonBar, "cell 0 2,align center bottom,grow 0 0");
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelWorkspaceName;
    private JLabel labelMetadataProfileName;
    private JTextField textFieldSearchLoadFileColumns;
    private JList<String> listLoadFileColumns;
    private JTextField textFieldSearchWorkspaceColumns;
    private JList<Field> listWorkspaceColumns;
    private JButton buttonAddAutoMapped;
    private JButton buttonAddMapping;
    private JButton buttonRemoveMapping;
    private JList<FieldMapping> listFieldMapping;
    private JButton buttonSave;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
