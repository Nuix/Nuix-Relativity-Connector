package com.nuix.relativityclient.ui;

import com.nuix.relativityclient.relativitytypes.Folder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FolderJTree extends JTree {

    public FolderJTree() {
        super();

        setModel(null);
        setCellRenderer(new IconTreeCellRenderer());
        addMouseListener(new OutOfRowBoundsListener());
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

            Object object = node.getUserObject();
            if (object instanceof Folder) {
                Folder folder = (Folder) object;
                setToolTipText("Artifact ID: " + folder.getArtifactId());
                return folder.getName();
            }
        }
        return "";
    }

    private static class IconTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (leaf) {
                Icon defaultIcon = getClosedIcon();
                setLeafIcon(defaultIcon);
            }

            return this;
        }
    }

    private static class OutOfRowBoundsListener extends MouseAdapter {

        //Clear tree row only if the mouseClick is out of bounds in the y-direction
        @Override
        public void mouseClicked(MouseEvent e) {
            JTree tree = (JTree) e.getSource();

            int mouseY = e.getY();

            TreePath selectionPath = tree.getSelectionPath();
            int row = tree.getRowForPath(selectionPath);
            if (row == -1) {
                return;
            }

            Rectangle rowBounds = tree.getRowBounds(row);
            if (!rowBounds.contains(rowBounds.x, mouseY)) {
                tree.clearSelection();
            }
        }
    }
}
