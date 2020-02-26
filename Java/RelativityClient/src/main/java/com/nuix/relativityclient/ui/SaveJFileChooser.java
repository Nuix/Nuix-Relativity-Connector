package com.nuix.relativityclient.ui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SaveJFileChooser extends JFileChooser {

    public SaveJFileChooser(File dir) {
        super(dir);
    }

    @Override
    public void approveSelection() {

        FileFilter fileFilter = getFileFilter();
        if (fileFilter instanceof JsonFileFilter) {
            JsonFileFilter jsonFileFilter = (JsonFileFilter) fileFilter;

            String fileLocationWithExtension = jsonFileFilter.attachExtension(getSelectedFile().getAbsolutePath());
            File fileWithExtension = new File(fileLocationWithExtension);

            setSelectedFile(fileWithExtension);
        }

        File selectedFile = getSelectedFile();
        String fileName = selectedFile.getName();

        if (selectedFile.exists() && getDialogType() == SAVE_DIALOG) {
            int result = JOptionPane.showConfirmDialog(this, fileName + " already exists. Do you want to replace it?", "Confirm Save As", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        super.approveSelection();
    }
}