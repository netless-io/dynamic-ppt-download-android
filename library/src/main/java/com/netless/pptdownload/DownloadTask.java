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
    private static final String DEFAULT_LAYOUT_FILENAME = "presentationML.zip";
    private static final String INFO_FILENAME = "info.json";
    private static final String SHARE_FILENAME = "bigFile.json";
    private static final String RESOURCE_JOURNAL_FILENAME = "resource.journal";
    private static final String SHARE_RESOURCE_JOURNAL_FILENAME = "share_resource.journal";

    private static final int NO_SIZE = -1;

    private final String taskUUID;
    private final String domain;
    private final String cacheDir;
    private final Downloader downloader;

    private State state = State.Init;
    // 基础资源文件
    private ResourceState resourceState;
    // 分享及大文件资源状态
    private ResourceState shareResourceState;
    private HashMap<Integer, ArrayList<ShareNode>> shareInfo;
    private ShareResourceDownloader currentShareDownloader;
    private int pptPageSize = NO_SIZE;

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
            DownloadLogger.i("[DownloadTask] layout dir existed");
            afterLayoutDownload();
            return;
        }

        File layoutZip = new File(cacheDir, taskUUID + ".zip");
        downloader.downloadToFile(getLayoutZipUrl(), layoutZip, new Downloader.DownloadCallback() {
            @Override
            public void onSuccess(String url, File desFile) {
                // 创建临时解压目录
                File layoutDirTmp = new File(cacheDir, taskUUID + "_tmp");

                try {
                    Utils.unzip(desFile, layoutDirTmp);
                } catch (Exception e) {
                    DownloadLogger.e("[DownloadTask] unzip layout resource error");
                    state = State.Fail;
                    return;
                }

                if (layoutDirTmp.renameTo(layoutDir) && desFile.delete()) {
                    afterLayoutDownload();
                } else {
                    DownloadLogger.e("[DownloadTask] rename layout tmp error");
                    state = State.Fail;
                }
            }

            @Override
            public void onFailure(String url) {
                DownloadLogger.e("[DownloadTask] download layout zip error, url " + url);
                state = State.Fail;
            }
        });
    }

    public boolean isStarted() {
        return state != State.Init;
    }

    private void afterLayoutDownload() {
        DownloadLogger.d("[DownloadTask] afterLayoutDownload");
        synchronized (this) {
            if (getPptPageSize() == NO_SIZE) {
                DownloadLogger.e("[DownloadTask] pptPageSize error");
                return;
            }
            loadResourceState();
            loadShareResourceInfo();
            loadShareResourceState();
        }

        downloadNextResource();
    }

    private void afterResourceDownload() {
        DownloadLogger.d("[DownloadTask] afterResourceDownload");

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
        resourceState = loadStateFromFile(RESOURCE_JOURNAL_FILENAME);
        resourceState.setOnStateChangeListener(onStateChangeListener);
    }

    private void saveResourceState() {
        saveStateToFile(resourceState, RESOURCE_JOURNAL_FILENAME);
    }

    private void loadShareResourceState() {
        shareResourceState = loadStateFromFile(SHARE_RESOURCE_JOURNAL_FILENAME);
        shareResourceState.setOnStateChangeListener(onStateChangeListener);
    }

    private void saveShareResourceState() {
        saveStateToFile(shareResourceState, SHARE_RESOURCE_JOURNAL_FILENAME);
    }

    private ResourceState loadStateFromFile(String journalFile) {
        ResourceState result = null;
        File resourceJournal = new File(new File(cacheDir, taskUUID), journalFile);
        if (resourceJournal.exists()) {
            try {
                String text = Utils.readFileToString(resourceJournal);
                if (!Utils.isEmpty(text)) {
                    result = new Gson().fromJson(text, ResourceState.class);
                }
            } catch (Exception e) {
                DownloadLogger.e("loadResourceState error");
            }
        }
        if (result == null) {
            result = new ResourceState(getPptPageSize());
        }
        return result;
    }

    private void saveStateToFile(ResourceState state, String journalFile) {
        File resourceJournal = new File(new File(cacheDir, taskUUID), journalFile);
        String text = new Gson().toJson(state);
        try {
            Utils.writeStringToFile(text, resourceJournal);
        } catch (IOException e) {
            DownloadLogger.e("saveResourceState error");
        }
    }

    private int getPptPageSize() {
        if (pptPageSize != NO_SIZE) {
            return pptPageSize;
        }

        File infoFile = new File(new File(cacheDir, taskUUID), INFO_FILENAME);
        try {
            String info = Utils.readFileToString(infoFile);
            JSONObject object = new JSONObject(info);
            pptPageSize = object.optInt("totalPageSize", NO_SIZE);
        } catch (Exception e) {
            DownloadLogger.e("[DownloadTask] parse pptPageSize error");
        }

        return pptPageSize;
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
        downloader.downloadToFile(getResourceZipUrl(index), resourceZip, new Downloader.DownloadCallback() {
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
            DownloadLogger.i("[DownloadTask] All Download Finish");
            state = State.SUCCESS;
            return;
        }

        int index = shareResourceState.nextIndex();
        DownloadLogger.i("[DownloadTask] downloadNextShare start " + index);
        ArrayList<ShareNode> shareNodeList = getShareNodeList(index);
        if (shareNodeList == null) {
            onShareResourceDownloadSuccess(index);
            return;
        }
        currentShareDownloader = new ShareResourceDownloader(index, shareNodeList);
        currentShareDownloader.download();
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
        try {
            synchronized (this) {
                if (index <= pptPageSize) {
                    resourceState.setNextIndex(index + 1);
                    shareResourceState.setNextIndex(index + 1);
                    if (currentShareDownloader != null) {
                        currentShareDownloader.cancel();
                    }
                }
            }
        } catch (Exception e) {
            DownloadLogger.e("onPageChangeTo error");
        }
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

    ArrayList<ShareNode> getShareNodeList(int index) {
        return shareInfo.get(index + 1);
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
        private int index = 0;

        ShareResourceDownloader(int index, ArrayList<ShareNode> shareNodes) {
            this.shareResourceIndex = index;
            this.shareNodes = shareNodes;
        }

        public void download() {
            downloadNext();
        }

        public void cancel() {
            if (index >= shareNodes.size()) {
                return;
            }
            downloader.cancel(getShareResourceUrl(shareNodes.get(index).name));
        }

        private void downloadNext() {
            if (index >= shareNodes.size()) {
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
            downloader.downloadToFile(getShareResourceUrl(shareNodes.get(index).name), targetTmp, new Downloader.DownloadCallback() {
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
