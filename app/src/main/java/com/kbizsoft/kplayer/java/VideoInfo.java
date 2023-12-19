package com.kbizsoft.KPlayer.java;

public class VideoInfo {
    private String url;
    private String fileName;

    public VideoInfo(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }
}

