package com.netless.pptdownload;

import junit.framework.TestCase;

public class DownloadTaskTest extends TestCase {
    DownloadTask downloadTask;

    public void setUp() throws Exception {
        super.setUp();
        downloadTask = new DownloadTask("a6f9d430d7c211ebae6f1dc0589306eb", "https://white-cn-doc-convert.oss-cn-hangzhou.aliyuncs.com", "/");
    }


    public void test_getShareResourcePath() {
        String name = "dynamicConvert/a6f9d430d7c211ebae6f1dc0589306eb/resources/ppt/media/media3.mp4";
        assertEquals("resources/ppt/media/media3.mp4", downloadTask.getShareResourcePath(name));
    }

    public void tearDown() throws Exception {
    }
}