package com.nuix.relativityclient.ui;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Locale;

public class JsonFileFilter extends FileFilter {

    String attachExtension(String path) {
        if (!path.matches("(?i)\\.json$")) {

            int lastDotIndex = path.lastIndexOf(".");
            return path.substring(0, lastDotIndex > 0 ? lastDotIndex : path.length()).concat(".json");
        }
        return path;
    }

    //Custom to also accept files without extensions, since that how Nuix saves Relativity Mappings
    @Override
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        String fileName = f.getName();
        int i = fileName.lastIndexOf('.');
        if (i < 0) {
            return true;
        }

        if (i > 0 && i < fileName.length() - 1) {
            String extension = fileName.substring(i + 1).toLowerCase(Locale.ENGLISH);
            return extension.equals("json");
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "JSON Settings File (*.json)";
    }
}
