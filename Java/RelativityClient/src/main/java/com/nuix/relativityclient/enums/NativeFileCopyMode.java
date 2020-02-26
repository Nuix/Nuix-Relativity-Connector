package com.nuix.relativityclient.enums;

import java.util.HashMap;

public enum NativeFileCopyMode {
    DO_NOT_IMPORT_NATIVE_FILES(0) {
        @Override
        public String toString() {
            return "Do Not Import Native Files";
        }
    },
    COPY_FILES(1) {
        @Override
        public String toString() {
            return "Copy Files";
        }
    },
    SET_FILE_LINKS(2) {
        @Override
        public String toString() {
            return "Set File Links";
        }
    };

    public static final HashMap<Integer, NativeFileCopyMode> valueToType = new HashMap<>();
    static {
        for (NativeFileCopyMode nativeFileCopyMode : values()) {
            valueToType.put(nativeFileCopyMode.getValue(), nativeFileCopyMode);
        }
    }

    public static NativeFileCopyMode getFromValue(int value) {
        return valueToType.get(value);
    }

    private int value;

    NativeFileCopyMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
