package com.nuix.relativityclient.ui;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class NotificationDialogs {

    public static void showErrorMessage(Component parentComponent, Throwable t, Logger logger) {
        showErrorMessage(parentComponent, "Unexpected Error", t, logger);
    }

    public static void showErrorMessage(Component parentComponent, String title, Throwable t, Logger logger) {
        showErrorMessage(parentComponent, title, t.getMessage(), t, logger);
    }

    public static void showErrorMessage(Component parentComponent, String title, String message, Throwable t, Logger logger) {
        JTextArea text = new JTextArea(message);
        text.setBackground(parentComponent.getBackground());
        text.setEditable(false);

        JOptionPane.showMessageDialog(parentComponent, text, title, JOptionPane.ERROR_MESSAGE);
        if (t != null) {
            logger.error(message, t);
        } else {
            logger.error( message);
        }
    }

    public static void showInformationMessage(Component parentComponent, String title, String message) {
        JTextArea text = new JTextArea(message);
        text.setBackground(parentComponent.getBackground());

        JOptionPane.showMessageDialog(parentComponent, text, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static int showWarningQuestion(Component parentComponent, String title, String message, String options[], String defaultOption){
        return JOptionPane.showOptionDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, defaultOption);
    }

}
