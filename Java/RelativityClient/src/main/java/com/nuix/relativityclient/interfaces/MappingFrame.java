package com.nuix.relativityclient.interfaces;

public interface MappingFrame {
    boolean isVisible();
    void setVisible(boolean visible);

    void setState(int state);
    void requestFocus();
}
