package com.netless.pptdownload;

import java.io.File;

/**
 * @author fenglibin
 */
public class DownloadHelper {

    private static final String DEFAULT_DOMAIN = "https://white-cn-doc-convert-dev.oss-cn-hangzhou.aliyuncs.com";

    private static DownloadHelper instance;

    Downloader downloader;
    private String domain;
    private String cacheDir;

    private DownloadHelper() {
        this.domain = DEFAULT_DOMAIN;
        this.downloader = new Downloader();
    }

    public static synchronized DownloadHelper getInstance() {
        if (instance == null) {
            instance = new DownloadHelper();
        }
        return instance;
    }

    public DownloadHelper setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public DownloadHelper setPPTCacheDir(String cacheDir) {
        File file = new File(cacheDir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                DownloadLogger.d("mkdirs success");
            } else {
                DownloadLogger.e("mkdirs error", null);
            }
        }

        if (!file.isDirectory()) {
            throw new DownloadException("file should be directory");
        }

        this.cacheDir = cacheDir;
        return this;
    }

    public DownloadTask newTask(String uuid) {
        return new DownloadTask(uuid, domain, cacheDir);
    }
}
