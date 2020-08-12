package com.nuix.relativityclient;

import com.nuix.relativityclient.interfaces.ModuleStartListener;
import com.nuix.relativityclient.model.ModuleSettings;
import com.nuix.relativityclient.ui.relativitymapping.RelativityMappingFrame;
import com.nuix.relativityclient.ui.settingsframe.SettingsFrame;
import nuix.Case;
import nuix.Utilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RelativityClientUI {
    private static final Logger LOGGER = LogManager.getLogger(RelativityClientUI.class.getName());
    private static RelativityClientUI instance;

    private SettingsFrame settingsFrame;
    private RelativityMappingFrame relativityMappingFrame;
    private ModuleSettings moduleSettings;

    //For dev purposes
//    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//
//        getInstance(null, null, null);
//    }

    //To prevent duplication in a single Nuix context, use singleton pattern
    //Otherwise multiple calls will create multiple instances
    public static RelativityClientUI getInstance(Utilities utilities, Case nuixCase, String scriptFolder) {
        if (instance == null) {
            instance = new RelativityClientUI(utilities, nuixCase, scriptFolder);
        } else {
            //Set userDataStore, allows live switching between utilities <-> nuixCase
            instance.moduleSettings.setUtilities(utilities);
            instance.moduleSettings.setNuixCase(nuixCase);
            instance.moduleSettings.setScriptFolder(scriptFolder);
        }
        //Bring frame to attention
        instance.settingsFrame.setState(Frame.NORMAL);
        instance.settingsFrame.requestFocus();

        return instance;
    }

    private RelativityClientUI(Utilities utilities, Case nuixCase, String scriptFolder) {
        moduleSettings = new ModuleSettings(utilities, nuixCase,scriptFolder);

        settingsFrame = new SettingsFrame(moduleSettings);
        settingsFrame.addWindowListener(new OnCloseListener());
        settingsFrame.setUtilities(utilities);

        relativityMappingFrame = new RelativityMappingFrame(moduleSettings);

        settingsFrame.setMappingFrame(relativityMappingFrame);
        moduleSettings.addObserver(settingsFrame);
        moduleSettings.addObserver(relativityMappingFrame);

        settingsFrame.setVisible(true);
    }

    //To be used by JRuby script to listen for module start action
    public void setModuleStartListener(ModuleStartListener moduleStartFunction) {
        settingsFrame.stopWaitingForUpload();
        settingsFrame.setModuleStartListener(moduleStartFunction);
        settingsFrame.waitForStartUpload();
    }

    private static class OnCloseListener extends WindowAdapter {
        @Override
        public void windowClosed(WindowEvent e) {
            instance.settingsFrame.stopWaitingForUpload();
            instance.settingsFrame.getTaskDelegator().forceShutdown();

            instance.relativityMappingFrame.dispose();
            instance = null;
        }
    }

    public SettingsFrame getSettingsFrame(){
        return settingsFrame;
    }
}
