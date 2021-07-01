package com.netless.pptdownload;

import junit.framework.TestCase;

public class DownloadHelperTest extends TestCase {

    public void testGetInstance() {
    }

    public void testSetDomain() {
    }

    public void testSetPPTCacheDir() {
    }

    public void testNewTask() {
    }

    public void testUpdateRoomState() {
    }

    public void testGetDomainFromSrc() {
        String src = "pptx://cover.herewhite.com/dfafdad/dynamicConvert/6a212c90fa5311ea8b9c074232aaccd4/1.slide";

        assertEquals("https://cover.herewhite.com/dfafdad", DownloadHelper.getDomainFromSrc(src));
    }

    public void testGetUUIDFromSrc() {
        String src = "pptx://cover.herewhite.com/dynamicConvert/6a212c90fa5311ea8b9c074232aaccd4/1.slide";

        assertEquals("6a212c90fa5311ea8b9c074232aaccd4", DownloadHelper.getUUIDFromSrc(src));
    }
}