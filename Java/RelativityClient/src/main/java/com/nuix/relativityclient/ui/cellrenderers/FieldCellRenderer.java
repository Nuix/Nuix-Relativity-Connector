package com.nuix.relativityclient.ui.cellrenderers;

import com.nuix.relativityclient.relativitytypes.Field;

import javax.swing.*;
import java.awt.*;

public class FieldCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Field) {
            Field field = (Field) value;
            setToolTipText("Artifact ID: " + field.getArtifactId());

            //Use HTML to make portions of text bold
            String fieldText = "<html><strong>" + field.getRelativityTextIdentifier();

            boolean isIdentifier = field.isIdentifier();
            if (isIdentifier) {
                fieldText += " [Identifier]";
            }

            fieldText += " </strong> - " + field.getType();

            int length = field.getLength();
            if (length > 0) {
                fieldText += " (" + length + ")";
            }

            fieldText += "</html>";
            setText(fieldText);
        }
        return this;
    }
}
