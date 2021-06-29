package com.netless.pptdownload;

import java.util.ArrayList;

/**
 * @author fenglibin
 */
class ResourceState {
    private static final int RETRY_TIME = 3;

    private static final int STATE_DOWNLOADING = 0;
    private static final int STATE_DONE = 1;
    private static final int STATE_FAIL = 2;
    private final int size;
    /**
     * 成功或者失败3次计数
     */
    private int doneSize;
    /**
     * 下一个资源索引
     */
    private int next = 0;

    private ArrayList<ResourceItem> resourceItems;

    ResourceState(int size) {
        this.size = size;

        resourceItems = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            resourceItems.add(new ResourceItem());
        }
    }

    void markDone(int index) {
        ResourceItem item = resourceItems.get(index);
        item.state = STATE_DONE;
        doneSize++;
    }

    void markFail(int index) {
        ResourceItem item = resourceItems.get(index);
        item.retryTime++;
        if (item.retryTime >= RETRY_TIME) {
            item.state = STATE_FAIL;
            doneSize++;
            next++;
        }
    }

    void setNextIndex(int next) {
        if (next > size - 1) {
            DownloadLogger.e("next out of bound", null);
            next = 0;
        }
        this.next = next;
    }

    int nextIndex() {
        int result = -1;
        for (int i = 0; i < size; i++) {
            int index = (next + i) % size;
            ResourceItem item = resourceItems.get(index);
            if (item.state == STATE_DOWNLOADING) {
                return index;
            }
        }
        return result;
    }

    boolean isAllDone() {
        return doneSize == size;
    }

    private static class ResourceItem {
        int state = STATE_DOWNLOADING;
        int retryTime;
    }
}
