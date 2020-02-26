package com.nuix.relativityclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nuix.relativityclient.enums.NativeFileCopyMode;
import com.nuix.relativityclient.enums.OverwriteMode;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UploadSettings {

    private NativeFileCopyMode nativeFileCopyMode;
    private OverwriteMode overwriteMode;

    public UploadSettings() {
        nativeFileCopyMode = NativeFileCopyMode.COPY_FILES;
        overwriteMode = OverwriteMode.APPEND;
    }

    @JsonProperty("nativeCopyMode")
    public int getIntNativeFileCopyMode() {
        return nativeFileCopyMode.getValue();
    }

    @JsonProperty("nativeCopyMode")
    public void setNativeFileCopyMode(int value) {
        nativeFileCopyMode = NativeFileCopyMode.getFromValue(value);
    }

    @JsonProperty("overwriteMode")
    public int getIntOverwriteMode() {
        return overwriteMode.getValue();
    }

    @JsonProperty("overwriteMode")
    public void setOverwriteMode(int value) {
        overwriteMode = OverwriteMode.getFromValue(value);
    }

    @JsonIgnore
    public NativeFileCopyMode getNativeFileCopyMode() {
        return nativeFileCopyMode;
    }

    @JsonIgnore
    public void setNativeFileCopyMode(NativeFileCopyMode nativeFileCopyMode) {
        this.nativeFileCopyMode = nativeFileCopyMode;
    }

    @JsonIgnore
    public OverwriteMode getOverwriteMode() {
        return overwriteMode;
    }

    @JsonIgnore
    public void setOverwriteMode(OverwriteMode overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        UploadSettings uploadSettings = (UploadSettings) obj;

        boolean isNativeFileCopyModeEqual = Objects.equals(getNativeFileCopyMode(), uploadSettings.getNativeFileCopyMode());
        boolean isOverwriteModeEqual = Objects.equals(getOverwriteMode(), uploadSettings.getOverwriteMode());

        return isNativeFileCopyModeEqual && isOverwriteModeEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNativeFileCopyMode(), getOverwriteMode());
    }
}
