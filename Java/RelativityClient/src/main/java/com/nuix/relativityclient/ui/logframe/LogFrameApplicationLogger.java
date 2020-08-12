package com.nuix.relativityclient.ui.logframe;

import com.nuix.relativityclient.ui.NotificationDialogs;
import com.nuix.relativityclient.utils.ApplicationLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LogFrameApplicationLogger extends ApplicationLogger {
    private static final Logger LOGGER = LogManager.getLogger(LogFrameApplicationLogger.class.getName());
    private LogFrame logFrame;

    public LogFrameApplicationLogger(LogFrame logFrame){
        this.logFrame = logFrame;
        logFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                notifyClosed();
            }
        });
    }

    @Override
    public void notifyComplete() {
        logInfo("Upload finished");

        logFrame.setUploadFinished(true);
        SwingUtilities.invokeLater(() -> {
            if (errorMessages.size()>0) {
                NotificationDialogs.showErrorMessage(logFrame, "Upload finished with errors", "Review upload log for details.", null, LOGGER);
            } else {
                NotificationDialogs.showInformationMessage(logFrame, "Upload finished", "Review upload log for details.");
            }
        });
    }

    @Override
    public void setStatus(String status, long currentValue, long estimatedFinalValue) {
        // NO-OP
    }

    @Override
    public void logException(String s, Exception e) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                errorMessages.add(s);
                if (e!=null) {
                    LOGGER.error(s, e);
                } else {
                    LOGGER.error(s);
                }
                StyledDocument styledDocument = logFrame.getTextPaneLog().getStyledDocument();
                SimpleAttributeSet keyWord = new SimpleAttributeSet();
                StyleConstants.setForeground(keyWord, new Color(153, 0, 0));
                StyleConstants.setBold(keyWord, true);
                try {
                    if (styledDocument.getLength() > 0) {
                        styledDocument.insertString(styledDocument.getLength(), "\n", null);
                    }
                    styledDocument.insertString(styledDocument.getLength(), s, keyWord);
                } catch (Exception ex) {
                    LOGGER.error("Cannot log error",ex);
                }
                logFrame.scrollToEnd();
            }
        });
    }

    @Override
    public void logInfo(String s) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                LOGGER.info(s);
                Document document = logFrame.getTextPaneLog().getDocument();
                try {
                    if (document.getLength() > 0) {
                        document.insertString(document.getLength(), "\n", null);
                    }
                    document.insertString(document.getLength(), s, null);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                logFrame.scrollToEnd();
            }
        });
    }
}
