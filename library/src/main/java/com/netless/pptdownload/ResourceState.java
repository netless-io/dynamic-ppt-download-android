package com.netless.pptdownload;

import java.util.ArrayList;

/**
 * @author fenglibin
 */
class ResourceState {
    private static final int STATE_DOWNLOADING = 0;
    private static final int STATE_DONE = 1;
    private static final int STATE_FAIL = 2;
    private final int size;
    private int doneSize;
    private int next = 0;

    private ArrayList<Item> list;

    ResourceState(int size) {
        this.size = size;
        this.doneSize = 0;

        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Item());
        }
    }

    void markDone(int index) {
        Item item = list.get(index);
        item.state = STATE_DONE;
        doneSize--;
    }

    void markFail(int index) {
        Item item = list.get(index);
        if (item.retryTime > 3) {
            // TODO
        } else {
            item.retryTime++;
        }
    }

    void setNextIndex(int next) {
        this.next = next;
    }

    int nextIndex() {
        int result = next;

        for (int i = 0; i < size; i++) {
            int index = (next + size) % size;
            Item item = list.get(index);
            if (item.state != STATE_DONE) {
                next = index;
            }
        }

        return result;
    }

    boolean isAllDone() {
        return doneSize == size;
    }

    private static class Item {
        int state = STATE_DOWNLOADING;
        int retryTime;

        Item() {

        }
    }
}
