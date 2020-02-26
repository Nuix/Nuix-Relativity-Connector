/*
 * Created by JFormDesigner on Sat Oct 19 01:00:48 EDT 2019
 */

package com.nuix.relativityclient.ui.fieldmappingcell;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class FieldMappingCell extends JPanel {

    public FieldMappingCell(String metadata, String field) {
        initComponents();
        setToolTipText(metadata + " - " + field);

        labelMetadata.setText(metadata);
        labelField.setText(field);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        labelMetadata = new JLabel();
        labelField = new JLabel();

        //======== this ========
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "0[grow,left]" +
            "[grow,right]0",
            // rows
            "0[]0"));
        add(labelMetadata, "cell 0 0");
        add(labelField, "cell 1 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelMetadata;
    private JLabel labelField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
