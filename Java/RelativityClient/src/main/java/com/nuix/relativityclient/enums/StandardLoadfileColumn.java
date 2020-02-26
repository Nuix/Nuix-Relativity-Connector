package com.nuix.relativityclient.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum StandardLoadfileColumn {
    DOCID(""),
    PARENT_DOCID(""),
    ATTACH_DOCID(""),
    BEGINBATES("Begin Bates"),
    ENDBATES("End Bates"),
    BEGINGROUP("Group Identifier"),
    ENDGROUP(""),
    PAGECOUNT("Page Count"),
    TEXTPATH("Extracted Text");

    public static final List<String> valuesToString = Arrays.stream(values()).map(Enum::toString).collect(Collectors.toList());

    private String fieldEquivalent;

    StandardLoadfileColumn(String fieldEquivalent) {
        this.fieldEquivalent = fieldEquivalent;
    }

    public String getFieldEquivalent() {
        return fieldEquivalent;
    }
}
