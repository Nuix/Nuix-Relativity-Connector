package com.nuix.relativityclient.ui;

import javax.swing.*;
import java.awt.*;

public class PlaceholderTextField extends JTextField {

    private String placeholder;

    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || placeholder.isEmpty() || !getText().isEmpty()) {
            return;
        }

        final Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(getDisabledTextColor());

        Insets insets = getInsets();
        int left = insets.left;
        int top = insets.top;

        graphics.drawString(placeholder, left, graphics.getFontMetrics().getMaxAscent() + top);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
