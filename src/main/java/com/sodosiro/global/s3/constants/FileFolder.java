package com.sodosiro.global.s3.constants;


public enum FileFolder {

    REVIEWS("reviews"),
    PROFILES("profiles"),
    TEST("test");

    private final String folderName;

    FileFolder(String folderName) {
        this.folderName = folderName;
    }

    public String getPath() {
        return folderName;
    }

}
