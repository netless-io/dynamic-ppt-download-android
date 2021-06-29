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

    private ArrayList<Item> items;

    private OnStateChangeListener onStateChangeListener;

    ResourceState(int size) {
        this.size = size;

        items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(new Item());
        }
    }

    void markDone(int index) {
        Item item = items.get(index);
        item.state = STATE_DONE;
        doneSize++;

        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChange(this);
        }
    }

    void markFail(int index) {
        Item item = items.get(index);
        item.retryTime++;
        if (item.retryTime >= RETRY_TIME) {
            item.state = STATE_FAIL;
            doneSize++;
            next++;
        }

        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChange(this);
        }
    }

    void setNextIndex(int index) {
        if (index > size - 1) {
            DownloadLogger.e("next out of bound", null);
            index = 0;
        }
        this.next = index;

        if (onStateChangeListener == null) {
            onStateChangeListener.onStateChange(this);
        }
    }

    int nextIndex() {
        for (int i = 0; i < size; i++) {
            int index = (next + i) % size;
            Item item = items.get(index);
            if (item.state == STATE_DOWNLOADING) {
                return index;
            }
        }
        return -1;
    }

    boolean isAllDone() {
        return doneSize == size;
    }

    private static class Item {
        int state = STATE_DOWNLOADING;
        int retryTime;
    }


    void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }


    interface OnStateChangeListener {
        void onStateChange(ResourceState state);
    }
}
