package com.netless.pptdownload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author fenglibin
 * <p>
 * taskUUID
 */
public class DownloadTask {
    private static final String DEFAULT_LAYOUT_FILENAME = "layout.zip";
    private static final String INFO_FILENAME = "info.json";
    private static final String SHARE_FILENAME = "share.json";
    private static final String RESOURCE_JOURNAL_FILENAME = "resource.journal";
    private static final String SHARE_RESOURCE_JOURNAL_FILENAME = "share_resource.journal";

    private final String taskUUID;
    private final String domain;
    private final String cacheDir;
    private final Downloader downloader;

    private State state = State.Init;
    private ResourceState resourceState;
    private ResourceState shareResourceState;
    private HashMap<Integer, ArrayList<ShareNode>> shareInfo;

    private ResourceState.OnStateChangeListener onStateChangeListener = state -> {
        if (state == resourceState) {
            saveResourceState();
        }

        if (state == shareResourceState) {
            saveShareResourceState();
        }
    };

    public DownloadTask(String taskUUID, String domain, String cacheDir) {
        this.taskUUID = taskUUID;
        this.domain = domain;
        this.cacheDir = cacheDir;

        downloader = DownloadHelper.getInstance().downloader;
    }

    public void start() {
        if (state == State.Started) {
            return;
        }
        state = State.Started;

        File layoutDir = new File(cacheDir, taskUUID);
        if (layoutDir.exists()) {
            afterLayoutDownload();
            return;
        }

        File layoutZip = new File(cacheDir, taskUUID + ".zip");
        downloader.downloadZipTo(getLayoutZipUrl(), layoutZip, new Downloader.DownloadCallback() {
            @Override
            public void onSuccess(String url, File desFile) {
                // 创建临时解压目录
                File layoutDirTmp = new File(cacheDir, taskUUID + "_tmp");

                try {
                    Utils.unzip(desFile, layoutDirTmp);
                } catch (Exception e) {
                    DownloadLogger.e("unzip layoutResource error");
                    return;
                }

                if (layoutDirTmp.renameTo(layoutDir) && desFile.delete()) {
                    afterLayoutDownload();
                } else {
                    DownloadLogger.e("rename layout tmp error");
                }
            }

            @Override
            public void onFailure(String url) {
                DownloadLogger.e("download layoutZip error, url " + url);
            }
        });
    }

    public boolean isStarted() {
        return state != State.Init;
    }

    private void afterLayoutDownload() {
        DownloadLogger.d("[DownloadTask] afterLayoutDownload start");
        loadResourceState();
        loadShareResourceInfo();
        loadShareResourceState();

        downloadNextResource();
    }

    private void afterResourceDownload() {
        DownloadLogger.d("[DownloadTask] afterResourceDownload start");

        downloadNextShare();
    }

    /**
     * 加载 share.json 文件
     */
    private void loadShareResourceInfo() {
        shareInfo = new HashMap<>();
        try {
            File sf = new File(new File(cacheDir, taskUUID), SHARE_FILENAME);
            String text = Utils.readFileToString(sf);
            Type empMapType = new TypeToken<HashMap<Integer, ArrayList<ShareNode>>>() {
            }.getType();
            shareInfo = new Gson().fromJson(text, empMapType);
        } catch (Exception e) {
        }
    }

    private void loadResourceState() {
        File resourceJournal = new File(new File(cacheDir, taskUUID), RESOURCE_JOURNAL_FILENAME);
        if (resourceJournal.exists()) {
            try {
                String text = Utils.readFileToString(resourceJournal);
                if (!Utils.isEmpty(text)) {
                    resourceState = new Gson().fromJson(text, ResourceState.class);
                }
            } catch (Exception e) {
                DownloadLogger.e("loadResourceState error");
            }
        }
        if (resourceState == null) {
            resourceState = new ResourceState(getPptPageSize());
        }
        resourceState.setOnStateChangeListener(onStateChangeListener);
    }

    private void saveResourceState() {
        File resourceJournal = new File(new File(cacheDir, taskUUID), RESOURCE_JOURNAL_FILENAME);
        String text = new Gson().toJson(resourceState);
        try {
            Utils.writeStringToFile(text, resourceJournal);
        } catch (IOException e) {
            DownloadLogger.e("saveResourceState error");
        }
    }

    private void loadShareResourceState() {
        File resourceJournal = new File(new File(cacheDir, taskUUID), SHARE_RESOURCE_JOURNAL_FILENAME);
        if (resourceJournal.exists()) {
            try {
                String text = Utils.readFileToString(resourceJournal);
                if (!Utils.isEmpty(text)) {
                    shareResourceState = new Gson().fromJson(text, ResourceState.class);
                }
            } catch (Exception e) {
                DownloadLogger.e("loadShareResourceState error");
            }
        }
        if (shareResourceState == null) {
            shareResourceState = new ResourceState(getPptPageSize());
        }
        shareResourceState.setOnStateChangeListener(onStateChangeListener);
    }

    private void saveShareResourceState() {
        File resourceJournal = new File(new File(cacheDir, taskUUID), SHARE_RESOURCE_JOURNAL_FILENAME);
        String state = new Gson().toJson(shareResourceState);
        try {
            Utils.writeStringToFile(state, resourceJournal);
        } catch (IOException e) {
            DownloadLogger.e("saveShareResourceState error");
        }
    }

    private int getPptPageSize() {
        File infoFile = new File(new File(cacheDir, taskUUID), INFO_FILENAME);
        try {
            String info = Utils.readFileToString(infoFile);
            JSONObject object = new JSONObject(info);
            return object.optInt("totalPageSize", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    private void downloadNextResource() {
        if (resourceState.isAllDone()) {
            afterResourceDownload();
            return;
        }

        int index = resourceState.nextIndex();
        File resourceZip = new File(new File(cacheDir, taskUUID), getResourceZipName(index));
        if (resourceZip.exists()) {
            resourceZip.delete();
        }
        downloader.downloadZipTo(getResourceZipUrl(index), resourceZip, new Downloader.DownloadCallback() {
            @Override
            public void onSuccess(String url, File desFile) {
                try {
                    File toDir = new File(cacheDir, taskUUID);
                    Utils.unzip(desFile, toDir);
                    desFile.delete();
                    resourceState.markDone(index);
                    DownloadLogger.d("downloadNextResource resource " + index + " done");
                } catch (Exception e) {
                    resourceState.markFail(index);
                    DownloadLogger.d("downloadNextResource resource " + index + " fail");
                }
                downloadNextResource();
            }

            @Override
            public void onFailure(String url) {
                resourceState.markFail(index);
                downloadNextResource();
            }
        });
    }

    private void downloadNextShare() {
        if (shareResourceState.isAllDone()) {
            state = State.SUCCESS;
            return;
        }

        int index = shareResourceState.nextIndex();
        DownloadLogger.i("[DownloadTask] downloadNextShare start " + index);
        if (shareInfo.get(index + 1) == null) {
            onShareResourceDownloadSuccess(index);
            return;
        }
        new ShareResourceDownloader(index, shareInfo.get(index + 1)).download();
    }

    private void onShareResourceDownloadSuccess(int index) {
        shareResourceState.markDone(index);
        downloadNextShare();
    }

    private void onShareResourceDownloadFailure(int index) {
        shareResourceState.markFail(index);
        downloadNextShare();
    }

    /**
     * @param index 从0开始的所有
     */
    public void onPageChangeTo(int index) {
        resourceState.setNextIndex(index + 1);

        shareResourceState.setNextIndex(index + 1);
    }

    String getLayoutZipUrl() {
        if (taskUUID == null) {
            throw new DownloadException("taskUUID should not be null !");
        }
        return domain + "/dynamicConvert/" + taskUUID + "/" + DEFAULT_LAYOUT_FILENAME;
    }

    /**
     * 资源索引值为索引 + 1
     *
     * @param index
     * @return
     */
    @NotNull
    private String getResourceZipName(int index) {
        return "resource" + (index + 1) + ".zip";
    }

    String getResourceZipUrl(int index) {
        if (taskUUID == null) {
            throw new DownloadException("taskUUID should not be null !");
        }
        return domain + "/dynamicConvert/" + taskUUID + "/resources/" + getResourceZipName(index);
    }

    String getShareResourcePath(String name) {
        int index = name.indexOf("resources");
        return name.substring(index);
    }

    String getShareResourceUrl(String name) {
        return domain + "/" + name;
    }

    enum State {
        Init,

        Started,

        Fail,

        SUCCESS,
    }

    class ShareResourceDownloader {
        private final ArrayList<ShareNode> shareNodes;
        private final int shareResourceIndex;
        private final int size;
        private int index = 0;

        ShareResourceDownloader(int index, ArrayList<ShareNode> shareNodes) {
            this.shareResourceIndex = index;
            this.shareNodes = shareNodes;
            size = shareNodes.size();
        }

        public void download() {
            downloadNext();
        }

        private void downloadNext() {
            if (index >= size) {
                onShareResourceDownloadSuccess(shareResourceIndex);
                return;
            }
            File target = new File(new File(cacheDir, taskUUID), getShareResourcePath(shareNodes.get(index).name));
            if (target.isFile()) {
                index++;
                downloadNext();
                return;
            }
            File targetTmp = new File(new File(cacheDir, taskUUID), getShareResourcePath(shareNodes.get(index).name) + ".tmp");
            downloader.downloadBigFile(getShareResourceUrl(shareNodes.get(index).name), targetTmp, new Downloader.DownloadCallback() {
                @Override
                public void onSuccess(String url, File desFile) {
                    if (desFile.renameTo(target)) {
                        DownloadLogger.d("[ShareResourceDownloader] downloadNext resource " + index);
                        index++;
                        downloadNext();
                    } else {
                        onShareResourceDownloadFailure(shareResourceIndex);
                    }
                }

                @Override
                public void onFailure(String url) {
                    onShareResourceDownloadFailure(shareResourceIndex);
                }
            });
        }
    }
}
