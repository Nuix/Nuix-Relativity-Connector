/*
 * Created by JFormDesigner on Tue Oct 22 15:31:32 EDT 2019
 */

package com.nuix.relativityclient.ui.logframe;

import com.nuix.relativityclient.ui.NotificationDialogs;
import com.nuix.relativityclient.ui.settingsframe.SettingsFrame;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

public class LogFrame extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(LogFrame.class.getName());
    private boolean disableLogAutoScroll = false;
    private boolean errorsEncountered = false;
    private boolean uploadFinished=false;
    private Set<String> errorMessages = new HashSet<>();
    private SettingsFrame settingsFrame;



    public LogFrame(SettingsFrame settingsFrame) {
        initComponents();
        this.settingsFrame=settingsFrame;

        scrollPaneLog.getVerticalScrollBar().addAdjustmentListener(event -> {
            JScrollBar vbar = (JScrollBar) event.getSource();

            if (!event.getValueIsAdjusting()) return;

            if ((vbar.getValue() + vbar.getVisibleAmount()) >= vbar.getMaximum())
                disableLogAutoScroll = false;
            else if (!disableLogAutoScroll)
                disableLogAutoScroll = true;
        });
    }

    public void notifyComplete() {
        logInfo("Upload finished");
        uploadFinished=true;
        SwingUtilities.invokeLater(() -> {
            if (errorsEncountered) {
                NotificationDialogs.showErrorMessage(this, "Upload finished with errors", "Review upload log for details.", null, LOGGER);
            } else {
                NotificationDialogs.showInformationMessage(this, "Upload finished", "Review upload log for details.");
            }
        });
    }

    public void logUniqueError(String s) {
        logUniqueError(s,null);
    }

    public void logUniqueError(String s, Exception e) {
        boolean messageUnique = errorMessages.add(s);
        if (messageUnique) {
            logError(s, e);
        } else {
            LOGGER.error(s, e);
        }
    }

    public void logError(String s) {
        logError(s,null);
    }

    public void logError(String s, Exception e) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                errorsEncountered = true;
                if (e!=null) {
                    LOGGER.error(s, e);
                } else {
                    LOGGER.error(s);
                }
                StyledDocument styledDocument = textPaneLog.getStyledDocument();
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
                scrollToEnd();
            }
        });
    }

    public void logInfo(String s) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                LOGGER.info(s);
                Document document = textPaneLog.getDocument();
                try {
                    if (document.getLength() > 0) {
                        document.insertString(document.getLength(), "\n", null);
                    }
                    document.insertString(document.getLength(), s, null);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                scrollToEnd();
            }
        });
    }

    public void scrollToEnd() {
        if (!disableLogAutoScroll) {
            JScrollBar verticalBar = scrollPaneLog.getVerticalScrollBar();
            AdjustmentListener downScroller = new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    Adjustable adjustable = e.getAdjustable();
                    adjustable.setValue(adjustable.getMaximum());
                    verticalBar.removeAdjustmentListener(this);
                }
            };
            verticalBar.addAdjustmentListener(downScroller);
        }
    }

    public boolean getUploadFinished() {
        return uploadFinished;
    }

    public void setUploadFinished(boolean uploadFinished) {
        this.uploadFinished = uploadFinished;
    }

    public JTextPane getTextPaneLog() {
        return textPaneLog;
    }

    private void thisWindowClosing(WindowEvent e) {
        if (!uploadFinished) {
            String[] options = new String[]{"Abort", "Cancel"};
            int userResponse = NotificationDialogs.showWarningQuestion(this, "Upload in progress...","Are you sure you want to abort the upload?", options, options[1]);
            if (userResponse != 0) {
                return;
            }
        }
        this.setVisible(false);
        this.dispose();
        settingsFrame.stopWaitingForUpload();
        settingsFrame.dispose();

    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel settingsPane = new JPanel();
        scrollPaneLog = new JScrollPane();
        textPaneLog = new JTextPane();

        //======== this ========
        setTitle("Relativity Upload Log");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(new ImageIcon(getClass().getResource("/NuixIcon.png")).getImage());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== settingsPane ========
        {
            settingsPane.setLayout(new MigLayout(
                "insets 0,hidemode 3,gap 0 0",
                // columns
                "[grow,fill]",
                // rows
                "[fill]" +
                "[grow,fill]" +
                "[fill]"));

            //======== scrollPaneLog ========
            {

                //---- textPaneLog ----
                textPaneLog.setEditable(false);
                scrollPaneLog.setViewportView(textPaneLog);
            }
            settingsPane.add(scrollPaneLog, "cell 0 1");
        }
        contentPane.add(settingsPane, BorderLayout.CENTER);
        setSize(740, 625);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPaneLog;
    private JTextPane textPaneLog;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
