package com.nuix.relativityclient.enums;

import java.util.HashMap;

public enum OverwriteMode {
    APPEND(0) {
        @Override
        public String toString() {
            return "Append";
        }
    },
    OVERLAY(1) {
        @Override
        public String toString() {
            return "Overlay";
        }
    };

    public static final HashMap<Integer, OverwriteMode> valueToType = new HashMap<>();
    static {
        for (OverwriteMode overwriteMode : values()) {
            valueToType.put(overwriteMode.getValue(), overwriteMode);
        }
    }

    public static OverwriteMode getFromValue(int value) {
        return valueToType.get(value);
    }

    private int value;

    OverwriteMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
