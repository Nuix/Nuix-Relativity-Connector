package com.nuix.relativityclient.enums;

public enum ModuleType {
    LOADFILE_IMPORT {
        @Override
        public String toString() {
            return "Relativity Loadfile Client";
        }
    },
    JDBC {
        @Override
        public String toString() {
            return "Relativity SQL Client";
        }
    }
}
