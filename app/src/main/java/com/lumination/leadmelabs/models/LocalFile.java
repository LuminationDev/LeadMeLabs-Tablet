package com.lumination.leadmelabs.models;

public class LocalFile {
    private final String fileType;
    private final String name;
    private final String path;

    public LocalFile(String fileType, String name, String path) {
        this.fileType = fileType;
        this.name = name;
        this.path = path;
    }

    public String getFileType() {
        return fileType;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
