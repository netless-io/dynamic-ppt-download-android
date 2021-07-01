package com.netless.pptdownload;

import com.herewhite.sdk.domain.RoomState;
import com.herewhite.sdk.domain.Scene;
import com.herewhite.sdk.domain.SceneState;

import java.io.File;
import java.util.HashMap;

/**
 * @author fenglibin
 */
public class DownloadHelper {
    private static DownloadHelper instance;

    Downloader downloader;
    private String domain;
    private String cacheDir;

    private HashMap<String, DownloadTask> tasksMap = new HashMap<>();

    private DownloadHelper() {
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
                DownloadLogger.d("[DownloadHelper] mkdirs success");
            } else {
                DownloadLogger.e("[DownloadHelper] mkdirs error");
            }
        }

        if (!file.isDirectory()) {
            throw new DownloadException("[DownloadHelper] cacheDir should be a directory");
        }

        this.cacheDir = cacheDir;
        return this;
    }

    public DownloadTask newTask(String uuid) {
        if (domain == null) {
            throw new DownloadException("[DownloadHelper] domain should not be null");
        }
        return newTask(uuid, domain);
    }

    public DownloadTask newTask(String uuid, String domain) {
        DownloadTask task = tasksMap.get(uuid);
        if (task == null) {
            task = new DownloadTask(uuid, domain, cacheDir);
            tasksMap.put(uuid, task);
        }
        return task;
    }

    public static void updateRoomState(RoomState state) {
        SceneState sceneState = state.getSceneState();
        if (sceneState != null && sceneState.getScenes() != null) {
            Scene s = sceneState.getScenes()[sceneState.getIndex()];
            if (s.getPpt() != null && s.getPpt().getSrc().startsWith("pptx")) {
                String src = s.getPpt().getSrc();
                String uuid = getUUIDFromSrc(src);
                String domain = getDomainFromSrc(src);

                DownloadTask task = DownloadHelper.getInstance().newTask(uuid, domain);
                if (!task.isStarted()) {
                    task.start();
                }
                task.onPageChangeTo(sceneState.getIndex());
            }
        }
    }

    /**
     * @param src startWith pptx etc."pptx://cover.herewhite.com/dfafdad/dynamicConvert/6a212c90fa5311ea8b9c074232aaccd4/1.slide"
     * @return
     */
    static String getDomainFromSrc(String src) {
        int index = src.indexOf("/dynamicConvert/");
        if (index != -1) {
            return "https://" + src.substring("pptx://".length(), index);
        } else {
            return null;
        }
    }

    static String getUUIDFromSrc(String src) {
        int index = src.indexOf("/dynamicConvert/");
        int start = index + "/dynamicConvert/".length();
        int end = src.lastIndexOf('/');
        if (index != -1) {
            return src.substring(start, end);
        } else {
            return null;
        }
    }
}
