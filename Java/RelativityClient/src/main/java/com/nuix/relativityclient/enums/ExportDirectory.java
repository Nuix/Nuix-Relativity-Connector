package com.nuix.relativityclient.enums;

public enum ExportDirectory {
    LOCAL_DIRECTORY {
        @Override
        public String toString() {
            return "Local Directory";
        }
    },
    RELATIVITY_ACCESSIBLE {
        @Override
        public String toString() {
            return "Relativity Accessible";
        }
    }
}
