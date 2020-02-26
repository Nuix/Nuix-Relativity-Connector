package com.nuix.relativityclient.ui.cellrenderers;

import com.nuix.relativityclient.relativitytypes.Workspace;

import javax.swing.*;
import java.awt.*;

public class WorkspaceCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Workspace) {
            Workspace workspace = (Workspace) value;
            setText(workspace.getRelativityTextIdentifier());
            setToolTipText("Artifact ID: " + workspace.getArtifactId());
        }
        return this;
    }
}
