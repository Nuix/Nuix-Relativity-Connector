package com.nuix.relativityclient.enums;

import java.util.HashMap;
import java.util.Map;

//Relativity FieldType Enumeration
public enum FieldType {
    EMPTY(-1) {
        @Override
        public String toString() {
            return "Empty Field";
        }
    },
    FIXED_LENGTH_TEXT(0) {
        @Override
        public String toString() {
            return "Fixed Length Text";
        }
    },
    WHOLE_NUMBER(1) {
        @Override
        public String toString() {
            return "Whole Number";
        }
    },
    DATE(2) {
        @Override
        public String toString() {
            return "Date";
        }
    },
    YES_NO(3) {
        @Override
        public String toString() {
            return "Yes/No";
        }
    },
    LONG_TEXT(4) {
        @Override
        public String toString() {
            return "Long Text";
        }
    },
    SINGLE_CHOICE(5) {
        @Override
        public String toString() {
            return "Single Choice";
        }
    },
    DECIMAL(6) {
        @Override
        public String toString() {
            return "Decimal";
        }
    },
    CURRENCY(7) {
        @Override
        public String toString() {
            return "Currency";
        }
    },
    MULTIPLE_CHOICE(8) {
        @Override
        public String toString() {
            return "Multiple Choice";
        }
    },
    FILE(9) {
        @Override
        public String toString() {
            return "File";
        }
    },
    SINGLE_OBJECT(10) {
        @Override
        public String toString() {
            return "Single Object";
        }
    },
    USER(11) {
        @Override
        public String toString() {
            return "User";
        }
    },
    MULTIPLE_OBJECT(13) {
        @Override
        public String toString() {
            return "Multiple Object";
        }
    };

    private static final Map<Integer, FieldType> idToType = new HashMap<>();
    static {
        for (FieldType fieldType : values()) {
            idToType.put(fieldType.getId(), fieldType);
        }
    }

    public static FieldType getFromId(int fieldTypeId) {
        return idToType.get(fieldTypeId);
    }

    private int id;

    FieldType(int id) {
        this.id = id;
    }

    private int getId() {
        return id;
    }
}
