/*
 * Created by JFormDesigner on Tue Oct 22 15:31:32 EDT 2019
 */

package com.nuix.relativityclient.ui.settingsframe;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuix.relativityclient.clients.RelativityJdbcUploadClient;
import com.nuix.relativityclient.clients.RelativityLoadfileUploadClient;
import com.nuix.relativityclient.enums.ModuleType;
import com.nuix.relativityclient.enums.NativeFileCopyMode;
import com.nuix.relativityclient.enums.OverwriteMode;
import com.nuix.relativityclient.interfaces.MappingFrame;
import com.nuix.relativityclient.interfaces.ModuleStartListener;
import com.nuix.relativityclient.interfaces.SettingsUpdateListener;
import com.nuix.relativityclient.interfaces.SimpleDocumentListener;
import com.nuix.relativityclient.model.*;
import com.nuix.relativityclient.relativitytypes.Field;
import com.nuix.relativityclient.relativitytypes.Folder;
import com.nuix.relativityclient.relativitytypes.Workspace;
import com.nuix.relativityclient.ui.*;
import com.nuix.relativityclient.ui.cellrenderers.WorkspaceCellRenderer;
import com.nuix.relativityclient.ui.logframe.LogFrame;
import com.nuix.relativityclient.ui.logframe.LogFrameApplicationLogger;
import com.nuix.relativityclient.utils.JdbcClient;
import com.nuix.relativityclient.utils.RelativityRestClient;
import com.nuix.relativityclient.utils.TaskDelegator;
import net.miginfocom.swing.MigLayout;
import nuix.Case;
import nuix.MetadataProfile;
import nuix.ProductionSet;
import nuix.Utilities;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umich.ms.batmass.gui.core.api.util.glasspane.DisabledPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.ws.rs.NotAuthorizedException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class SettingsFrame extends JFrame implements SettingsUpdateListener {
    private static final Logger LOGGER = LogManager.getLogger(SettingsFrame.class.getName());
    private static final String DEFAULT_SAVE_NAME = "RelativityClientSettings.json";

    private File lastAccessedLoadfileFile;
    private File lastAccessedSettingsFile;

    private ModuleSettings moduleSettings;
    private RelativityRestClient relativityClient;
    private TaskDelegator taskDelegator;

    private ModuleStartListener moduleStartListener;
    private MappingFrame mappingFrame;
    private Object startUploadLock = new Object();

    private DisabledPanel disabledPanel;
    private PrintableMetadataProfile[] metadataProfiles;

    public SettingsFrame(ModuleSettings settings) {
        initComponents();

        Container contentPane = getContentPane();
        Component settingComponent = contentPane.getComponents()[0];
        contentPane.remove(0);
        disabledPanel = new DisabledPanel((Container) settingComponent);
        Color disabledColor = new Color(this.getBackground().getRed(),this.getBackground().getGreen(),this.getBackground().getBlue(),127);
        disabledPanel.setDisabledColor(disabledColor);
        contentPane.add(disabledPanel, BorderLayout.CENTER);

        attachListWorkspaceButtonEnableDocumentListener();
        attachButtonEnableDocumentListener();

        initializeProductionSet(settings.getNuixCase());

        moduleSettings = settings;
        relativityClient = new RelativityRestClient();
        taskDelegator = new TaskDelegator();

        ModuleType moduleType = (ModuleType) comboBoxModuleType.getSelectedItem();
        toggleVisible(moduleType);
        setMinimumSize();
    }

    private void initializeProductionSet(Case nuixCase){
        if (nuixCase!=null){
            try {
                comboBoxProductionSet.setModel(new DefaultComboBoxModel(nuixCase.getProductionSets().toArray()));
            } catch (IOException e) {
                NotificationDialogs.showErrorMessage(this, "Cannot List Production Sets", e.getMessage(), e, LOGGER);
            }
        }
    }

    public void setRelativityPassword(String password){
        SwingUtilities.invokeLater(() -> passwordFieldRelativityPassword.setText(password));
    }

    public void setSqlPassword(String password){
        SwingUtilities.invokeLater(() -> passwordFieldSqlPassword.setText(password));

    }

    @Override
    public void setUtilities(Utilities utilities) {

        List<MetadataProfile> availableMetadataProfiles = utilities.getMetadataProfileStore().getMetadataProfiles();
        metadataProfiles = new PrintableMetadataProfile[availableMetadataProfiles.size()];
        for (int i=0;i<availableMetadataProfiles.size();i++){
            PrintableMetadataProfile printableMetadataProfile = new PrintableMetadataProfile(availableMetadataProfiles.get(i));
            metadataProfiles[i]=printableMetadataProfile;
        }
        comboBoxMetadataProfile.setModel(new DefaultComboBoxModel(metadataProfiles));
        comboBoxMetadataProfile.setSelectedItem(null);
}

    @Override
    public void setNuixCase(Case nuixCase) {
        initializeProductionSet(nuixCase);
    }

    @Override
    public void setScriptFolder(String scriptFolder) {
        //NO-OP
    }

    @Override
    public void readSettings(ModuleSettings settings) {
        SqlSettings sqlSettings = settings.getSqlSettings();
        textFieldSqlServerName.setText(sqlSettings.getServerName());
        spinnerSqlServerPort.setValue(sqlSettings.getServerPort());
        textFieldSqlInstanceName.setText(sqlSettings.getInstanceName());
        textFieldSqlDomain.setText(sqlSettings.getDomain());
        textFieldSqlUsername.setText(sqlSettings.getUsername());

        RelativitySettings relativitySettings = settings.getRelativitySettings();
        textFieldRelativityWebServiceUrl.setText(relativitySettings.getWebServiceUrl());
        textFieldRelativityUsername.setText(relativitySettings.getUsername());

        FieldsSettings fieldsSettings = settings.getFieldsSettings();

        boolean metadataProfileMatched=false;
        for (int i=0;i<metadataProfiles.length;i++){
            PrintableMetadataProfile printableMetadataProfile = metadataProfiles[i];
            if (printableMetadataProfile.getMetadataProfile().getName().equals(fieldsSettings.getMetadataProfileName())){
                comboBoxMetadataProfile.setSelectedIndex(i);
                metadataProfileMatched=true;
                break;
            }
        }

        if (!metadataProfileMatched){
            comboBoxMetadataProfile.setSelectedItem(null);
        }

        UploadSettings uploadSettings = settings.getUploadSettings();
        comboBoxNativeFileCopyMode.setSelectedItem(uploadSettings.getNativeFileCopyMode());
        comboBoxOverwriteMode.setSelectedItem(uploadSettings.getOverwriteMode());

        //Check if sqlSettings && productionSet have values, IFF they do then set moduleType to ModuleType.JDBC, else ModuleType.LOADFILE_IMPORT
        boolean isJdbcModuleTypeSelected = !(textFieldSqlServerName.getText().isEmpty()
            && textFieldSqlInstanceName.getText().isEmpty() && textFieldSqlDomain.getText().isEmpty() && textFieldSqlUsername.getText().isEmpty()
        );

        if (isJdbcModuleTypeSelected) {
            comboBoxModuleType.setSelectedItem(ModuleType.JDBC);
        } else {
            comboBoxModuleType.setSelectedItem(ModuleType.LOADFILE_IMPORT);
        }
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        setMappingCountText();

        if (workspace == null) {
            treeFolders.setModel(null);
        } else {
            taskDelegator.submit(new ListRootFolderTask(workspace), true);
        }
    }

    @Override
    public void setMetadataProfileName(String metadataProfileName) {
        setMappingCountText();

        enableSaveButtonIfShould();
        enableStartButtonIfShould();
    }

    @Override
    public void setMappingsList(List<FieldMapping> fieldMappings) {
        setMappingCountText();

        enableSaveButtonIfShould();
        enableStartButtonIfShould();
    }

    @Override
    public void addMappings(FieldMapping ...fieldMappings) {
        setMappingCountText();

        enableSaveButtonIfShould();
        enableStartButtonIfShould();
    }

    @Override
    public void removeMappings(FieldMapping ...fieldMappings) {
        setMappingCountText();

        enableSaveButtonIfShould();
        enableStartButtonIfShould();
    }

    public void waitForStartUpload(){
        try {
            synchronized(startUploadLock) {
                startUploadLock.wait();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Cannot wait for start upload");
        }
    }

    public void stopWaitingForUpload(){
        synchronized(startUploadLock) {
            startUploadLock.notifyAll();
        }
    }

    public void setModuleStartListener(ModuleStartListener moduleStartListener) {
        this.moduleStartListener = moduleStartListener;
    }

    public void setMappingFrame(MappingFrame mappingFrame) {
        this.mappingFrame = mappingFrame;
    }

    public TaskDelegator getTaskDelegator() {
        return taskDelegator;
    }

    private void buttonSaveActionPerformed() {
        JFileChooser fileChooser = new SaveJFileChooser(lastAccessedSettingsFile);
        fileChooser.setDialogTitle("Save Settings");
        fileChooser.setSelectedFile(new File(DEFAULT_SAVE_NAME));

        JsonFileFilter jsonFileFilter = new JsonFileFilter();
        fileChooser.setFileFilter(jsonFileFilter);

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            lastAccessedSettingsFile = fileChooser.getSelectedFile();
            commitModuleSettings();
            saveSettingsToFile(lastAccessedSettingsFile);
        }
    }

    private void buttonLoadActionPerformed() {
        JFileChooser fileChooser = new JFileChooser(lastAccessedSettingsFile);
        fileChooser.setDialogTitle("Load Settings");

        JsonFileFilter jsonFileFilter = new JsonFileFilter();
        fileChooser.setFileFilter(jsonFileFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            readSettingsFromFile(fileChooser.getSelectedFile());
        }
    }

    public void readSettingsFromFile(String filePath){
        readSettingsFromFile(new File(filePath));
    }

    public void readSettingsFromFile(File file){
        try {
            lastAccessedSettingsFile = file;
            LOGGER.info("Loading module settings from file: " + lastAccessedSettingsFile.getAbsolutePath());

            ObjectMapper objectMapper = new ObjectMapper();
            ModuleSettings settings = objectMapper.readValue(lastAccessedSettingsFile, ModuleSettings.class);

            moduleSettings.readSettings(settings);
            listWorkspaces.setListData(new Workspace[]{});

        } catch (JsonMappingException | JsonParseException e) {
            String title = "Invalid Settings File";
            String message = "This is not a valid Relativity Client Settings json file.";

            NotificationDialogs.showErrorMessage(this, title, message, e, LOGGER);

        } catch (IOException e) {
            NotificationDialogs.showErrorMessage(this, e, LOGGER);
        }
    }

    private void buttonLoadfileLocationActionPerformed() {
        JFileChooser fileChooser = new JFileChooser(lastAccessedLoadfileFile);
        fileChooser.setDialogTitle("Select Loadfile");

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Loadfile (.dat)", "dat");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            LOGGER.info("Setting loadfile file location");
            lastAccessedLoadfileFile = fileChooser.getSelectedFile();

            String filePath = lastAccessedLoadfileFile.getAbsolutePath();
            textFieldLoadfileLocation.setText(filePath);
        }
    }

    private void comboBoxModuleTypeActionPerformed() {
        ModuleType moduleType = (ModuleType) comboBoxModuleType.getSelectedItem();

        if (moduleType == ModuleType.JDBC) {
            //Check if started with a nuixCase, if not prompt no action
            if (moduleSettings.getNuixCase()==null) {
                String message = "The SQL module requires a Nuix case. Please open a case and try again.";
                JOptionPane.showMessageDialog(null, message, "Relativity Client", JOptionPane.INFORMATION_MESSAGE);

                comboBoxModuleType.setSelectedItem(ModuleType.LOADFILE_IMPORT);
                return;
            }
        }

        toggleVisible(moduleType);

        enableSaveButtonIfShould();
        enableStartButtonIfShould();
    }

    private void buttonListWorkspacesActionPerformed() {
        disabledPanel.setEnabled(false);
        listWorkspaces.setListData(new Workspace[]{});
        taskDelegator.submit(new ListWorkspacesTask(), true);
    }

    private void listWorkspacesValueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        Workspace workspace = listWorkspaces.getSelectedValue();
        if (workspace == null) {
            moduleSettings.setWorkspace(null);
        } else {
            DefaultTreeModel treeModel = new DefaultTreeModel(null);
            treeFolders.setModel(treeModel);
            taskDelegator.submit(new SetWorkspaceAndFieldsTask(workspace), true);
        }
    }

    private void treeFoldersValueChanged() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeFolders.getLastSelectedPathComponent();

        Folder folder = null;
        if (node != null) {
            folder = (Folder) node.getUserObject();
        }

        moduleSettings.setFolder(folder);

        enableStartButtonIfShould();
    }

    private void treeFoldersTreeWillExpand(TreeExpansionEvent e) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
        if (parentNode.children().hasMoreElements()) {
            return;
        }

        long workspaceArtifactId = moduleSettings.getWorkspace().getArtifactId();
        taskDelegator.submit(new ListSubFoldersTask(parentNode, workspaceArtifactId));
    }

    private void comboBoxMetadataProfileActionPerformed() {
        PrintableMetadataProfile printableMetadataProfile = (PrintableMetadataProfile) comboBoxMetadataProfile.getSelectedItem();
        String selectedMetadataProfileName = moduleSettings.getFieldsSettings().getMetadataProfileName();

        if (selectedMetadataProfileName == null ||
                (printableMetadataProfile != null && !selectedMetadataProfileName.equals(printableMetadataProfile.getMetadataProfile().getName()))) {

            if (printableMetadataProfile == null) {
                moduleSettings.setMetadataProfileName(null);
            } else {
                moduleSettings.setMetadataProfileName(printableMetadataProfile.getMetadataProfile().getName());
            }
        }
    }

    //Show MappingFrame
    private void buttonMappingActionPerformed() {
        if (!mappingFrame.isVisible()) {
            LOGGER.info("Showing Relativity Mapping");
            mappingFrame.setVisible(true);
        } else {
            //Bring frame to attention
            mappingFrame.setState(Frame.NORMAL);
            mappingFrame.requestFocus();
        }
    }

    private void startButtonActionPerformed() {
        boolean isSaved = true;
        commitModuleSettings();
        if (lastAccessedSettingsFile == null) {
            isSaved = false;
        } else {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ModuleSettings settings = objectMapper.readValue(lastAccessedSettingsFile, ModuleSettings.class);

                isSaved = settings.equals(moduleSettings);

            } catch (IOException e) {
                NotificationDialogs.showErrorMessage(this, e, LOGGER);
            }
        }

        if (!isSaved) {
            JFileChooser fileChooser = new SaveJFileChooser(lastAccessedSettingsFile);
            fileChooser.setDialogTitle("Save Settings");
            fileChooser.setSelectedFile(new File(DEFAULT_SAVE_NAME));

            JsonFileFilter jsonFileFilter = new JsonFileFilter();
            fileChooser.setFileFilter(jsonFileFilter);

            int result = fileChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                //If they need to save and didn't save, then don't start modules and exit method
                return;
            }

            lastAccessedSettingsFile = fileChooser.getSelectedFile();

            saveSettingsToFile(lastAccessedSettingsFile);
        }


        TaskDelegator taskDelegator = new TaskDelegator();
        ModuleType moduleType = (ModuleType) comboBoxModuleType.getSelectedItem();
        ClientLauncherSettings clientLauncherSettings = new ClientLauncherSettings();
        clientLauncherSettings.setModuleType(moduleType);

        if (moduleType.equals(ModuleType.LOADFILE_IMPORT)) {
            File loadfile = new File(textFieldLoadfileLocation.getText());
            if (!loadfile.exists()) {
                String message = "The loadfile could not be found under:\n" + loadfile.getAbsolutePath();
                IOException ioException = new IOException(message);
                NotificationDialogs.showErrorMessage(this, "Cannot Find Loadfile", message, ioException, LOGGER);
                return;
            }

            clientLauncherSettings.setLoadfilePath(textFieldLoadfileLocation.getText());
            clientLauncherSettings.setClientSettingsPath(lastAccessedSettingsFile.getAbsolutePath());
            clientLauncherSettings.setWorkspaceArtifactId(moduleSettings.getWorkspace().getArtifactId());
            clientLauncherSettings.setFolderArtifactId(moduleSettings.getFolder().getArtifactId());
            clientLauncherSettings.setRelativityPassword(String.valueOf(passwordFieldRelativityPassword.getPassword()));
            clientLauncherSettings.setRelativityVersion(moduleSettings.getRelativitySettings().getVersion());
            clientLauncherSettings.setScriptPath(moduleSettings.getScriptFolder());

            LogFrame logFrame = new LogFrame(this);
            LogFrameApplicationLogger logFrameApplicationLogger = new LogFrameApplicationLogger(logFrame);
            RelativityLoadfileUploadClient relativityLoadfileUploadClient = new RelativityLoadfileUploadClient(logFrameApplicationLogger, clientLauncherSettings);

            Path clientPath = relativityLoadfileUploadClient.getClientPath();
            if (!clientPath.toFile().exists()) {
                String message = "The Relativity Upload Client for Relativity version " + clientLauncherSettings.getRelativityVersion() + " could not be found under:\n" + clientPath.toString() + "\n\nPlease see documentation instructions on how to build the client and retry.";
                IOException ioException = new IOException(message);
                NotificationDialogs.showErrorMessage(this, "Cannot Find Upload Client", message, ioException, LOGGER);
                return;
            }
            logFrame.setVisible(true);
            taskDelegator.submit(relativityLoadfileUploadClient);
            setVisible(false);
        } else if (moduleType.equals(ModuleType.JDBC)) {
            disabledPanel.setEnabled(false);

            MetadataProfile metadataProfile = null;
            if (comboBoxMetadataProfile.getSelectedItem()!=null) {
                metadataProfile = ((PrintableMetadataProfile) comboBoxMetadataProfile.getSelectedItem()).getMetadataProfile();
            }
            taskDelegator.submit(new StartJdbcUpload(this, moduleSettings, (ProductionSet)comboBoxProductionSet.getSelectedItem(),metadataProfile), false);
        } else {
            throw new UnsupportedOperationException("Unexpected module type " + moduleType);
        }

    }

    private void cancelButtonActionPerformed() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void commitModuleSettings(){
        RelativitySettings relativitySettings = moduleSettings.getRelativitySettings();
        relativitySettings.setWebServiceUrl(textFieldRelativityWebServiceUrl.getText());
        relativitySettings.setUsername(textFieldRelativityUsername.getText());

        SqlSettings sqlSettings = moduleSettings.getSqlSettings();
        sqlSettings.setServerName(textFieldSqlServerName.getText());
        sqlSettings.setServerPort((Integer)spinnerSqlServerPort.getValue());
        sqlSettings.setInstanceName(textFieldSqlInstanceName.getText());
        sqlSettings.setDomain(textFieldSqlDomain.getText());
        sqlSettings.setUsername(textFieldSqlUsername.getText());

        UploadSettings uploadSettings = moduleSettings.getUploadSettings();

        NativeFileCopyMode nativeFileCopyMode = (NativeFileCopyMode) comboBoxNativeFileCopyMode.getSelectedItem();
        uploadSettings.setNativeFileCopyMode(nativeFileCopyMode);

        OverwriteMode overwriteMode = (OverwriteMode) comboBoxOverwriteMode.getSelectedItem();
        uploadSettings.setOverwriteMode(overwriteMode);

    }

    private void saveSettingsToFile(File saveToFile) {
        try {
            LOGGER.info("Saving module settings to file: " + saveToFile.getAbsolutePath());

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(saveToFile, moduleSettings);

        } catch (IOException e) {
            NotificationDialogs.showErrorMessage(this, e, LOGGER);
        }
    }

    private void setMappingCountText() {
        int mappingsCount = moduleSettings.getFieldsSettings().getMappingList().size();
        if (mappingsCount == 0) {
            labelMappingCount.setText("");
            buttonMapping.setText("Create");
            return;
        }

        if (!buttonMapping.getText().equals("Edit")) {
            buttonMapping.setText("Edit");
        }

        String countText = mappingsCount + " Field" + (mappingsCount > 1  ? "s" : "") + " Mapped";
        labelMappingCount.setText(countText);
    }

    //DocumentListener to determine when listWorkspacesButton should be enabled/disabled
    private void attachListWorkspaceButtonEnableDocumentListener() {
        DocumentListener listWorkspaceButtonEnableListener = (SimpleDocumentListener) e -> {
            boolean shouldEnableListWorkspacesButton = !(textFieldRelativityWebServiceUrl.getText().isEmpty() || textFieldRelativityUsername.getText().isEmpty() || passwordFieldRelativityPassword.getPassword().length == 0);
            buttonListWorkspaces.setEnabled(shouldEnableListWorkspacesButton);
        };

        textFieldRelativityWebServiceUrl.getDocument().addDocumentListener(listWorkspaceButtonEnableListener);
        textFieldRelativityUsername.getDocument().addDocumentListener(listWorkspaceButtonEnableListener);
        passwordFieldRelativityPassword.getDocument().addDocumentListener(listWorkspaceButtonEnableListener);
    }

    private void attachButtonEnableDocumentListener() {
        DocumentListener buttonEnableListener = (SimpleDocumentListener) e -> {
            enableSaveButtonIfShould();
            enableStartButtonIfShould();
        };

        textFieldLoadfileLocation.getDocument().addDocumentListener(buttonEnableListener);
        textFieldSqlServerName.getDocument().addDocumentListener(buttonEnableListener);
        textFieldSqlInstanceName.getDocument().addDocumentListener(buttonEnableListener);
        textFieldSqlDomain.getDocument().addDocumentListener(buttonEnableListener);
        textFieldSqlUsername.getDocument().addDocumentListener(buttonEnableListener);
        passwordFieldSqlPassword.getDocument().addDocumentListener(buttonEnableListener);

        textFieldRelativityWebServiceUrl.getDocument().addDocumentListener(buttonEnableListener);
        textFieldRelativityUsername.getDocument().addDocumentListener(buttonEnableListener);
        passwordFieldRelativityPassword.getDocument().addDocumentListener(buttonEnableListener);
    }

    private void enableStartButtonIfShould() {
        ModuleType moduleType = (ModuleType) comboBoxModuleType.getSelectedItem();
        if (moduleType == null) {
            return;
        }

        boolean isModuleSettingsIncomplete;
        switch (moduleType) {
            case LOADFILE_IMPORT:
                isModuleSettingsIncomplete = textFieldLoadfileLocation.getText().isEmpty();
                break;
            case JDBC:
                boolean isSqlValuesIncomplete = (textFieldSqlServerName.getText().isEmpty() || textFieldSqlUsername.getText().isEmpty() || passwordFieldSqlPassword.getPassword().length == 0);
                buttonTestConnection.setEnabled(!isSqlValuesIncomplete);
                isModuleSettingsIncomplete = comboBoxProductionSet.getSelectedItem()==null || isSqlValuesIncomplete;
                break;
            default:
                throw new IllegalArgumentException("Module Type " + moduleType + " not implemented");
        }

        Folder folder = moduleSettings.getFolder();
        FieldsSettings fieldsSettings = moduleSettings.getFieldsSettings();
        String metadataProfileName = fieldsSettings.getMetadataProfileName();

        boolean commonSettingsIncomplete = (folder == null || metadataProfileName == null || fieldsSettings.getMappingList().isEmpty());

        boolean isRelativitySettingsIncomplete = textFieldRelativityWebServiceUrl.getText().isEmpty() || textFieldRelativityUsername.getText().isEmpty()
            || passwordFieldRelativityPassword.getPassword().length == 0;

        boolean shouldEnable = !(commonSettingsIncomplete || isModuleSettingsIncomplete || isRelativitySettingsIncomplete);
        startButton.setEnabled(shouldEnable);
    }

    private void enableSaveButtonIfShould() {
        ModuleType moduleType = (ModuleType) comboBoxModuleType.getSelectedItem();

        boolean isModuleSettingsEmpty = true;
        if (moduleType == ModuleType.JDBC) {
            boolean isSqlValuesEmpty = textFieldSqlServerName.getText().isEmpty() && textFieldSqlInstanceName.getText().isEmpty();
            boolean isSqlAuthenticationEmpty = textFieldSqlDomain.getText().isEmpty() && textFieldSqlUsername.getText().isEmpty();

            isModuleSettingsEmpty = isSqlValuesEmpty && isSqlAuthenticationEmpty;
        }

        FieldsSettings fieldsSettings = moduleSettings.getFieldsSettings();
        List<FieldMapping> fieldMappings = fieldsSettings.getMappingList();
        String metadataProfileName = fieldsSettings.getMetadataProfileName();

        boolean isFieldSettingsEmpty = fieldMappings.isEmpty() && metadataProfileName == null;

        boolean isRelativitySettingsEmpty = textFieldRelativityWebServiceUrl.getText().isEmpty() && textFieldRelativityUsername.getText().isEmpty();

        boolean shouldEnable = !(isFieldSettingsEmpty && isModuleSettingsEmpty && isRelativitySettingsEmpty);
        buttonSave.setEnabled(shouldEnable);
    }

    private void toggleVisible(ModuleType moduleType) {
        clearFields(moduleType);

        boolean isImportSettingsVisible = moduleType == ModuleType.LOADFILE_IMPORT;
        labelLoadfileLocation.setVisible(isImportSettingsVisible);
        textFieldLoadfileLocation.setVisible(isImportSettingsVisible);
        buttonLoadfileLocation.setVisible(isImportSettingsVisible);

        boolean isJdbcSettingsVisible = moduleType == ModuleType.JDBC;
        labelProductionSet.setVisible(isJdbcSettingsVisible);
        comboBoxProductionSet.setVisible(isJdbcSettingsVisible);
        panelSqlDatbase.setVisible(isJdbcSettingsVisible);
        setMinimumSize();
    }

    private void clearFields(ModuleType moduleType) {
        switch (moduleType) {
            case LOADFILE_IMPORT:
                textFieldSqlServerName.setText("");
                spinnerSqlServerPort.setValue(1433);
                textFieldSqlInstanceName.setText("");
                textFieldSqlDomain.setText("");
                textFieldSqlUsername.setText("");
                passwordFieldSqlPassword.setText("");
                break;
            case JDBC:
                textFieldLoadfileLocation.setText("");
                break;
        }
    }

    //For when screen content changes (added/removed/visibility) and want to reset minimum sizes
    private void setMinimumSize() {
        setMinimumSize(new Dimension(getWidth(), 0));
        pack();
        setMinimumSize(new Dimension(getPreferredSize().width, getHeight()));
    }

    private void comboBoxNativeFileCopyModeActionPerformed(ActionEvent e) {
        moduleSettings.getUploadSettings().setNativeFileCopyMode((NativeFileCopyMode) comboBoxNativeFileCopyMode.getSelectedItem());
    }

    private void comboBoxOverwriteModeActionPerformed(ActionEvent e) {
        moduleSettings.getUploadSettings().setOverwriteMode((OverwriteMode) comboBoxOverwriteMode.getSelectedItem());
    }

    private void buttonTestConnectionActionPerformed(ActionEvent e) {
        TaskDelegator taskDelegator = new TaskDelegator();
        disabledPanel.setEnabled(false);
        taskDelegator.submit(new TestJdbcTask(this), false);
    }

    private JdbcClient getJdbcClient() throws SQLException {
        return new JdbcClient(textFieldSqlServerName.getText(),
                (Integer)spinnerSqlServerPort.getValue(),
                textFieldSqlInstanceName.getText(),
                textFieldSqlDomain.getText(),
                textFieldSqlUsername.getText(),
                String.valueOf(passwordFieldSqlPassword.getPassword())
        );
    }

    private class StartJdbcUpload implements Runnable {
        private SettingsFrame settingsFrame;
        private ModuleSettings moduleSettings;
        private ProductionSet productionSet;
        private MetadataProfile metadataProfile;

        public StartJdbcUpload(SettingsFrame settingsFrame, ModuleSettings moduleSettings, ProductionSet productionSet, MetadataProfile metadataProfile){
            this.settingsFrame = settingsFrame;
            this.moduleSettings = moduleSettings;
            this.productionSet = productionSet;
            this.metadataProfile = metadataProfile;
        }

        @Override
        public void run() {
            try {
                JdbcClient jdbcClient = getJdbcClient();
                LogFrame logFrame = new LogFrame(settingsFrame);
                LogFrameApplicationLogger logFrameApplicationLogger = new LogFrameApplicationLogger(logFrame);
                RelativityJdbcUploadClient jdbcUploadClient = new RelativityJdbcUploadClient(logFrameApplicationLogger, jdbcClient, moduleSettings, productionSet, metadataProfile, moduleSettings.getWorkspace().getArtifactId(), moduleSettings.getFolder().getArtifactId());
                logFrame.setVisible(true);
                taskDelegator.submit(jdbcUploadClient);
                setVisible(false);
            } catch (SQLException ex) {
                NotificationDialogs.showErrorMessage(settingsFrame, "Test Connection", "Cannot connect to SQL Server: " + ex.getMessage(), ex, LOGGER);
            } finally {
                SwingUtilities.invokeLater(() -> disabledPanel.setEnabled(true));
            }
        }
    }

    private class TestJdbcTask implements Runnable {
        private Component parent;
        public TestJdbcTask(Component parent){
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                getJdbcClient();
                NotificationDialogs.showInformationMessage(parent, "Test Connection", "Connection to SQL Server successful.");
            } catch (SQLException ex) {
                NotificationDialogs.showErrorMessage(parent, "Test Connection", "Cannot connect to SQL Server: " + ex.getMessage(), ex, LOGGER);
            } finally {
                SwingUtilities.invokeLater(() -> disabledPanel.setEnabled(true));
            }
        }
    }

    private class ListWorkspacesTask implements Runnable {
        @Override
        public void run() {
            LOGGER.info("Getting Relativity version and available workspaces");
            String webServiceUrl = textFieldRelativityWebServiceUrl.getText();
            String username = textFieldRelativityUsername.getText();
            char[] password = passwordFieldRelativityPassword.getPassword();

            RelativitySettings relativitySettings = moduleSettings.getRelativitySettings();
            relativitySettings.setWebServiceUrl(webServiceUrl);
            relativitySettings.setUsername(username);
            relativitySettings.setPassword(password);

            try {
                int cutoffIndex = webServiceUrl.lastIndexOf("/");
                String hostUrl = webServiceUrl.substring(0, cutoffIndex);

                relativityClient.setHostUrl(hostUrl);
                relativityClient.buildAndSetAuthCredentials(username, password);

                String version = relativityClient.getVersion();
                LOGGER.info("Relativity version: " + version);
                String[] versionComponents = version.split("\\.");
                String majorMinorVersion = versionComponents [0]+"."+versionComponents [1];
                LOGGER.info("Relativity major.minor version: " + majorMinorVersion);
                relativitySettings.setVersion(majorMinorVersion);

                List<Workspace> workspaces = relativityClient.getAvailableWorkspaces().getResults();

                SwingUtilities.invokeLater(() -> listWorkspaces.setListData(workspaces.toArray(new Workspace[0])));

            } catch (StringIndexOutOfBoundsException e) {
                String title = "Invalid Web Service URL";
                String message = "Web Service URL must be in the format: http(s)://hostname/endpoint\n" +
                    "Please try again.";

                NotificationDialogs.showErrorMessage(SettingsFrame.this, title, message, e, LOGGER);

            } catch (NotAuthorizedException e) {
                String title = e.getMessage();
                String message = "";

                Object obj = e.getChallenges().get(0);
                if (obj instanceof String) {
                    message = (String) obj;
                }

                NotificationDialogs.showErrorMessage(SettingsFrame.this, title, message, e, LOGGER);

            } catch (Exception e) {
                NotificationDialogs.showErrorMessage(SettingsFrame.this, "Connection Error", e, LOGGER);

            } finally {
                SwingUtilities.invokeLater(() -> disabledPanel.setEnabled(true));
            }
        }
    }

    private class SetWorkspaceAndFieldsTask implements Runnable {
        private final Workspace workspace;

        private SetWorkspaceAndFieldsTask(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Getting workspace fields");
                List<Field> fields = relativityClient.getWorkspaceFields(workspace.getArtifactId());
                workspace.setFields(fields);

                SwingUtilities.invokeLater(() -> moduleSettings.setWorkspace(workspace));
            } catch (NotAuthorizedException e) {
                String title = e.getMessage();
                String message = "";

                Object obj = e.getChallenges().get(0);
                if (obj instanceof String) {
                    message = (String) obj;
                }

                NotificationDialogs.showErrorMessage(SettingsFrame.this, title, message, e, LOGGER);
            } catch (Exception e) {
                NotificationDialogs.showErrorMessage(SettingsFrame.this, e, LOGGER);
            }
         }
    }

    private class ListRootFolderTask implements Runnable {
        private Workspace workspace;

        private ListRootFolderTask(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Getting workspace folders");
                long workspaceArtifactId = workspace.getArtifactId();

                Folder rootFolder = relativityClient.getWorkspaceRootFolder(workspaceArtifactId);
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootFolder);
                //Populate root folder children nodes
                taskDelegator.submit(new ListSubFoldersTask(rootNode, workspaceArtifactId));

                SwingUtilities.invokeLater(() -> {
                    DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
                    treeModel.setAsksAllowsChildren(true);
                    treeFolders.setModel(treeModel);


                });
            } catch (NotAuthorizedException e) {
                String title = e.getMessage();
                String message = "";

                Object obj = e.getChallenges().get(0);
                if (obj instanceof String) {
                    message = (String) obj;
                }

                NotificationDialogs.showErrorMessage(SettingsFrame.this, title, message, e, LOGGER);
            } catch (Exception e) {
                NotificationDialogs.showErrorMessage(SettingsFrame.this, e, LOGGER);
            }
        }
    }

    private class ListSubFoldersTask implements Runnable {
        private DefaultMutableTreeNode parentNode;
        private long workspaceArtifactId;

        private ListSubFoldersTask(DefaultMutableTreeNode parentNode, long workspaceArtifactId) {
            this.parentNode = parentNode;
            this.workspaceArtifactId = workspaceArtifactId;
        }

        @Override
        public void run() {
            try {
                Folder parentFolder = (Folder) parentNode.getUserObject();


                LOGGER.info("Getting " + parentFolder.getName() + " subfolders");
                List<Folder> childFolders = relativityClient.getSubFolders(workspaceArtifactId, parentFolder.getArtifactId());

                SwingUtilities.invokeLater(() -> {
                    //Need to add nodes through treeModel or else JTree won't get updated properly
                    DefaultTreeModel treeModel = (DefaultTreeModel) treeFolders.getModel();
                    //Add all children
                    for (Folder childFolder : childFolders) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childFolder);
                        treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());
                    }
                });
            } catch (IOException e) {
                NotificationDialogs.showErrorMessage(SettingsFrame.this, e, LOGGER);
            }
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel settingsPane = new JPanel();
        JPanel contentPanel = new JPanel();
        JPanel panelModule = new JPanel();
        JLabel labelModuleType = new JLabel();
        comboBoxModuleType = new JComboBox<>(ModuleType.values());
        buttonSave = new JButton();
        JButton buttonLoad = new JButton();
        JPanel panelRelativitySettings = new JPanel();
        JLabel labelRelativityWebServiceUrl = new JLabel();
        textFieldRelativityWebServiceUrl = new PlaceholderTextField("https://relativity.example.com/relativitywebapi");
        JLabel labelRelativityUsername = new JLabel();
        textFieldRelativityUsername = new JTextField();
        JLabel labelRelativityPassword = new JLabel();
        passwordFieldRelativityPassword = new JPasswordField();
        buttonListWorkspaces = new JButton();
        labelWorkspaces = new JLabel();
        panelWorkspaces = new JPanel();
        JScrollPane scrollPaneWorkspaces = new JScrollPane();
        listWorkspaces = new DeselectJList<>();
        listWorkspaces.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listWorkspaces.setCellRenderer(new WorkspaceCellRenderer());
        scrollPaneFolders = new JScrollPane();
        treeFolders = new FolderJTree();
        treeFolders.setShowsRootHandles(true);
        JPanel panelFieldsSettings = new JPanel();
        JLabel labelMetadataProfile = new JLabel();
        comboBoxMetadataProfile = new JComboBox<>();
        JLabel labelMapping = new JLabel();
        buttonMapping = new JButton();
        labelMappingCount = new JLabel();
        JPanel panelUploadSettings = new JPanel();
        JLabel labelNativeFiles = new JLabel();
        comboBoxNativeFileCopyMode = new JComboBox<>(NativeFileCopyMode.values());
        JLabel labelOverwriteMode = new JLabel();
        comboBoxOverwriteMode = new JComboBox<>(OverwriteMode.values());
        panel1 = new JPanel();
        labelLoadfileLocation = new JLabel();
        textFieldLoadfileLocation = new JTextField();
        buttonLoadfileLocation = new JButton();
        labelProductionSet = new JLabel();
        comboBoxProductionSet = new JComboBox();
        panelSqlDatbase = new JPanel();
        JLabel labelSqlServerName = new JLabel();
        textFieldSqlServerName = new JTextField();
        JLabel labelSqlServerPort = new JLabel();
        spinnerSqlServerPort = new JSpinner();
        JLabel labelSqlInstanceName = new JLabel();
        textFieldSqlInstanceName = new JTextField();
        JLabel labelSqlDomain = new JLabel();
        textFieldSqlDomain = new JTextField();
        JLabel labelSqlUsername = new JLabel();
        textFieldSqlUsername = new JTextField();
        JLabel labelSqlPassword = new JLabel();
        passwordFieldSqlPassword = new JPasswordField();
        buttonTestConnection = new JButton();
        JPanel buttonBar = new JPanel();
        startButton = new JButton();
        JButton cancelButton = new JButton();

        //======== this ========
        setIconImage(new ImageIcon(getClass().getResource("/NuixIcon.png")).getImage());
        setTitle("Relativity Client");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== settingsPane ========
        {
            settingsPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new MigLayout(
                    "insets dialog,hidemode 3",
                    // columns
                    "[fill]unrel" +
                    "[grow,fill]" +
                    "[button,fill]" +
                    "[button,fill]",
                    // rows
                    "[]para" +
                    "[grow]" +
                    "[]" +
                    "[]" +
                    "[]"));

                //======== panelModule ========
                {
                    panelModule.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "0[fill]" +
                        "[fill]" +
                        "[grow,fill]" +
                        "[sizegroup button,fill]" +
                        "[sizegroup button,fill]0",
                        // rows
                        "0[]0"));

                    //---- labelModuleType ----
                    labelModuleType.setText("Import Module:");
                    panelModule.add(labelModuleType, "cell 0 0");

                    //---- comboBoxModuleType ----
                    comboBoxModuleType.addActionListener(e -> comboBoxModuleTypeActionPerformed());
                    panelModule.add(comboBoxModuleType, "cell 1 0");

                    //---- buttonSave ----
                    buttonSave.setText("Save");
                    buttonSave.setEnabled(false);
                    buttonSave.addActionListener(e -> buttonSaveActionPerformed());
                    panelModule.add(buttonSave, "cell 3 0");

                    //---- buttonLoad ----
                    buttonLoad.setText("Load");
                    buttonLoad.addActionListener(e -> buttonLoadActionPerformed());
                    panelModule.add(buttonLoad, "cell 4 0");
                }
                contentPanel.add(panelModule, "cell 0 0 4 1");

                //======== panelRelativitySettings ========
                {
                    panelRelativitySettings.setBorder(new TitledBorder("Relativity Settings"));
                    panelRelativitySettings.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[90,fill]" +
                        "[20,fill]0" +
                        "[330,fill]" +
                        "[grow,fill]",
                        // rows
                        "[]" +
                        "[]" +
                        "[]para" +
                        "[]unrel" +
                        "[]unrel" +
                        "[grow]para" +
                        "[]"));

                    //---- labelRelativityWebServiceUrl ----
                    labelRelativityWebServiceUrl.setText("Web Service URL:");
                    panelRelativitySettings.add(labelRelativityWebServiceUrl, "cell 0 0");
                    panelRelativitySettings.add(textFieldRelativityWebServiceUrl, "cell 1 0 2 1");

                    //---- labelRelativityUsername ----
                    labelRelativityUsername.setText("Username:");
                    panelRelativitySettings.add(labelRelativityUsername, "cell 0 1");
                    panelRelativitySettings.add(textFieldRelativityUsername, "cell 1 1 2 1");

                    //---- labelRelativityPassword ----
                    labelRelativityPassword.setText("Password:");
                    panelRelativitySettings.add(labelRelativityPassword, "cell 0 2");
                    panelRelativitySettings.add(passwordFieldRelativityPassword, "cell 1 2 2 1");

                    //---- buttonListWorkspaces ----
                    buttonListWorkspaces.setText("List Workspaces");
                    buttonListWorkspaces.setEnabled(false);
                    buttonListWorkspaces.addActionListener(e -> buttonListWorkspacesActionPerformed());
                    panelRelativitySettings.add(buttonListWorkspaces, "cell 0 3 2 1");

                    //---- labelWorkspaces ----
                    labelWorkspaces.setText("Workspaces:");
                    panelRelativitySettings.add(labelWorkspaces, "cell 0 4");

                    //======== panelWorkspaces ========
                    {
                        panelWorkspaces.setLayout(new MigLayout(
                            "hidemode 3",
                            // columns
                            "0[225,grow,fill]" +
                            "[225,grow,fill]0",
                            // rows
                            "0[grow]0"));

                        //======== scrollPaneWorkspaces ========
                        {

                            //---- listWorkspaces ----
                            listWorkspaces.setVisibleRowCount(15);
                            listWorkspaces.addListSelectionListener(e -> listWorkspacesValueChanged(e));
                            scrollPaneWorkspaces.setViewportView(listWorkspaces);
                        }
                        panelWorkspaces.add(scrollPaneWorkspaces, "cell 0 0");

                        //======== scrollPaneFolders ========
                        {

                            //---- treeFolders ----
                            treeFolders.setVisibleRowCount(15);
                            treeFolders.addTreeSelectionListener(e -> treeFoldersValueChanged());
                            treeFolders.addTreeWillExpandListener(new TreeWillExpandListener() {
                                @Override
                                public void treeWillCollapse(TreeExpansionEvent e)
                                    throws ExpandVetoException
                                {}
                                @Override
                                public void treeWillExpand(TreeExpansionEvent e)
                                    throws ExpandVetoException
                                {
                                    treeFoldersTreeWillExpand(e);
                                }
                            });
                            scrollPaneFolders.setViewportView(treeFolders);
                        }
                        panelWorkspaces.add(scrollPaneFolders, "cell 1 0");
                    }
                    panelRelativitySettings.add(panelWorkspaces, "cell 1 4 3 2");

                    //======== panelFieldsSettings ========
                    {
                        panelFieldsSettings.setLayout(new MigLayout(
                            "hidemode 3",
                            // columns
                            "0[90,fill]" +
                            "[button,fill]" +
                            "[fill]" +
                            "[grow,fill]0",
                            // rows
                            "0[]" +
                            "[]0"));

                        //---- labelMetadataProfile ----
                        labelMetadataProfile.setText("Metadata Profile:");
                        panelFieldsSettings.add(labelMetadataProfile, "cell 0 0");

                        //---- comboBoxMetadataProfile ----
                        comboBoxMetadataProfile.addActionListener(e -> comboBoxMetadataProfileActionPerformed());
                        panelFieldsSettings.add(comboBoxMetadataProfile, "cell 1 0 2 1");

                        //---- labelMapping ----
                        labelMapping.setText("Mapping:");
                        panelFieldsSettings.add(labelMapping, "cell 0 1");

                        //---- buttonMapping ----
                        buttonMapping.setText("Create");
                        buttonMapping.addActionListener(e -> buttonMappingActionPerformed());
                        panelFieldsSettings.add(buttonMapping, "cell 1 1");
                        panelFieldsSettings.add(labelMappingCount, "cell 2 1 2 1");
                    }
                    panelRelativitySettings.add(panelFieldsSettings, "cell 0 6 4 1");
                }
                contentPanel.add(panelRelativitySettings, "cell 0 1 4 1");

                //======== panelUploadSettings ========
                {
                    panelUploadSettings.setBorder(new TitledBorder("Upload Settings"));
                    panelUploadSettings.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[90,fill]" +
                        "[fill]",
                        // rows
                        "[]" +
                        "[]"));

                    //---- labelNativeFiles ----
                    labelNativeFiles.setText("Native Files:");
                    panelUploadSettings.add(labelNativeFiles, "cell 0 0");

                    //---- comboBoxNativeFileCopyMode ----
                    comboBoxNativeFileCopyMode.addActionListener(e -> comboBoxNativeFileCopyModeActionPerformed(e));
                    panelUploadSettings.add(comboBoxNativeFileCopyMode, "cell 1 0");

                    //---- labelOverwriteMode ----
                    labelOverwriteMode.setText("Overwrite Mode:");
                    panelUploadSettings.add(labelOverwriteMode, "cell 0 1");

                    //---- comboBoxOverwriteMode ----
                    comboBoxOverwriteMode.addActionListener(e -> comboBoxOverwriteModeActionPerformed(e));
                    panelUploadSettings.add(comboBoxOverwriteMode, "cell 1 1");
                }
                contentPanel.add(panelUploadSettings, "cell 0 2 4 1");

                //======== panel1 ========
                {
                    panel1.setBorder(new TitledBorder("Module Settings"));
                    panel1.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[90,fill]" +
                        "[fill]" +
                        "[grow,fill]" +
                        "[button,fill]",
                        // rows
                        "[]0" +
                        "[]"));

                    //---- labelLoadfileLocation ----
                    labelLoadfileLocation.setText("Loadfile Location:");
                    panel1.add(labelLoadfileLocation, "cell 0 0");
                    panel1.add(textFieldLoadfileLocation, "cell 1 0 2 1");

                    //---- buttonLoadfileLocation ----
                    buttonLoadfileLocation.setText("Browse");
                    buttonLoadfileLocation.addActionListener(e -> buttonLoadfileLocationActionPerformed());
                    panel1.add(buttonLoadfileLocation, "cell 3 0");

                    //---- labelProductionSet ----
                    labelProductionSet.setText("Production Set:");
                    panel1.add(labelProductionSet, "cell 0 1");
                    panel1.add(comboBoxProductionSet, "cell 1 1");
                }
                contentPanel.add(panel1, "cell 0 3 4 1");

                //======== panelSqlDatbase ========
                {
                    panelSqlDatbase.setBorder(new TitledBorder("SQL Database"));
                    panelSqlDatbase.setAutoscrolls(true);
                    panelSqlDatbase.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[90,fill]" +
                        "[350,fill]30" +
                        "[right]" +
                        "[10,grow 50,shrink 50,fill]",
                        // rows
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]"));

                    //---- labelSqlServerName ----
                    labelSqlServerName.setText("Server Name:");
                    panelSqlDatbase.add(labelSqlServerName, "cell 0 0");
                    panelSqlDatbase.add(textFieldSqlServerName, "cell 1 0");

                    //---- labelSqlServerPort ----
                    labelSqlServerPort.setText("Server Port:");
                    panelSqlDatbase.add(labelSqlServerPort, "cell 2 0");

                    //---- spinnerSqlServerPort ----
                    spinnerSqlServerPort.setModel(new SpinnerNumberModel(1433, 1, 65535, 1));
                    panelSqlDatbase.add(spinnerSqlServerPort, "cell 3 0");

                    //---- labelSqlInstanceName ----
                    labelSqlInstanceName.setText("Instance Name:");
                    panelSqlDatbase.add(labelSqlInstanceName, "cell 0 1");
                    panelSqlDatbase.add(textFieldSqlInstanceName, "cell 1 1");

                    //---- labelSqlDomain ----
                    labelSqlDomain.setText("Domain:");
                    panelSqlDatbase.add(labelSqlDomain, "cell 0 2");
                    panelSqlDatbase.add(textFieldSqlDomain, "cell 1 2");

                    //---- labelSqlUsername ----
                    labelSqlUsername.setText("Username:");
                    panelSqlDatbase.add(labelSqlUsername, "cell 0 3");
                    panelSqlDatbase.add(textFieldSqlUsername, "cell 1 3");

                    //---- labelSqlPassword ----
                    labelSqlPassword.setText("Password:");
                    panelSqlDatbase.add(labelSqlPassword, "cell 0 4");
                    panelSqlDatbase.add(passwordFieldSqlPassword, "cell 1 4");

                    //---- buttonTestConnection ----
                    buttonTestConnection.setText("Test Connection");
                    buttonTestConnection.addActionListener(e -> buttonTestConnectionActionPerformed(e));
                    panelSqlDatbase.add(buttonTestConnection, "cell 2 4 2 1");
                }
                contentPanel.add(panelSqlDatbase, "cell 0 4 4 1");
            }
            settingsPane.add(contentPanel, BorderLayout.NORTH);

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                    "insets dialog,alignx right",
                    // columns
                    "[button,fill]" +
                    "[button,fill]",
                    // rows
                    "[]"));

                //---- startButton ----
                startButton.setText("Start");
                startButton.setEnabled(false);
                startButton.addActionListener(e -> startButtonActionPerformed());
                buttonBar.add(startButton, "cell 0 0");

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, "cell 1 0");
            }
            settingsPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(settingsPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<ModuleType> comboBoxModuleType;
    private JButton buttonSave;
    private JTextField textFieldRelativityWebServiceUrl;
    private JTextField textFieldRelativityUsername;
    private JPasswordField passwordFieldRelativityPassword;
    private JButton buttonListWorkspaces;
    private JLabel labelWorkspaces;
    private JPanel panelWorkspaces;
    private JList<Workspace> listWorkspaces;
    private JScrollPane scrollPaneFolders;
    private JTree treeFolders;
    private JComboBox<String> comboBoxMetadataProfile;
    private JButton buttonMapping;
    private JLabel labelMappingCount;
    private JComboBox<NativeFileCopyMode> comboBoxNativeFileCopyMode;
    private JComboBox<OverwriteMode> comboBoxOverwriteMode;
    private JPanel panel1;
    private JLabel labelLoadfileLocation;
    private JTextField textFieldLoadfileLocation;
    private JButton buttonLoadfileLocation;
    private JLabel labelProductionSet;
    private JComboBox comboBoxProductionSet;
    private JPanel panelSqlDatbase;
    private JTextField textFieldSqlServerName;
    private JSpinner spinnerSqlServerPort;
    private JTextField textFieldSqlInstanceName;
    private JTextField textFieldSqlDomain;
    private JTextField textFieldSqlUsername;
    private JPasswordField passwordFieldSqlPassword;
    private JButton buttonTestConnection;
    private JButton startButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
