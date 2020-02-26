package com.nuix.relativityclient.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DeselectJList<E> extends JList<E> {

    public DeselectJList() {
        super();

        addMouseListener(new OutOfCellBoundsListener(true));
    }

    @Override
    public int locationToIndex(Point location) {
        int index = super.locationToIndex(location);
        //Index is in JList and it is not in the bounds of a list cell
        if (index != -1 && !getCellBounds(index, index).contains(location)) {
            return -1;
        }
        return index;
    }

    private static class OutOfCellBoundsListener extends MouseAdapter {
        private boolean ignoreKeys;

        private OutOfCellBoundsListener(boolean ignoreKeys) {
            this.ignoreKeys = ignoreKeys;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JList list = (JList) e.getSource();

            int index = list.locationToIndex(e.getPoint());
            //Clear selection if point is out of list bounds, shift key or menuShortcutKey is down
            boolean keysDown = ignoreKeys || (!e.isShiftDown() && !isMenuShortcutKeyDown(e));
            if (index == -1 && keysDown) {
                list.clearSelection();
            }
        }

        private boolean isMenuShortcutKeyDown(InputEvent e) {
            return (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
        }
    }
}
