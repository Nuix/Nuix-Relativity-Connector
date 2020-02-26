package com.nuix.relativityclient.ui.cellrenderers;

import com.nuix.relativityclient.model.FieldMapping;
import com.nuix.relativityclient.relativitytypes.Field;
import com.nuix.relativityclient.ui.fieldmappingcell.FieldMappingCell;

import javax.swing.*;
import java.awt.*;

public class FieldMappingCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof FieldMapping) {
            FieldMapping fieldMapping = (FieldMapping) value;

            String metadataItemName = fieldMapping.getMetadataItemName();
            Field field = fieldMapping.getField();

            String fieldText = field.getRelativityTextIdentifier();

            boolean isFieldIdentifier = field.isIdentifier();
            if (isFieldIdentifier) {
                fieldText += " [Identifier]";
            }

            FieldMappingCell cell = new FieldMappingCell(metadataItemName, fieldText);

            //Copy UI values: return MetadataToFieldCell instead of DefaultListCellRenderer (JLabel)
            cell.setBackground(this.getBackground());
            cell.setForeground(this.getForeground());
            cell.setEnabled(this.isEnabled());
            cell.setFont(this.getFont());
            cell.setBorder(this.getBorder());

            return cell;
        }
        return this;
    }
}
