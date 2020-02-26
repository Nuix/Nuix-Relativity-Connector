package com.nuix.relativityclient.enums;

public enum UploadMode {
    APPEND {
        @Override
        public String toString() {
            return "Append";
        }
    },
    OVERLAY {
        @Override
        public String toString() {
            return "Overlay";
        }
    }
}
