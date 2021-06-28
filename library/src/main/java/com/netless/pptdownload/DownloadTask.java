package com.netless.pptdownload;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;

/**
 * @author fenglibin
 * <p>
 * taskUUID
 */
public class DownloadTask {
    private static final String DEFAULT_LAYOUT_FILENAME = "layout.zip";
    private static final String DEFAULT_INFO_FILENAME = "info.json";

    private final String taskUUID;
    private final String domain;
    private final String dirPath;
    private final Downloader downloader;

    private State state = State.Init;
    private ResourceState resourceState;

    public DownloadTask(String taskUUID, String domain, String dirPath) {
        this.taskUUID = taskUUID;
        this.domain = domain;
        this.dirPath = dirPath;

        downloader = DownloadHelper.getInstance().downloader;
    }

    public void start() {
        File layoutDirTmp = new File(dirPath, taskUUID + "_tmp");
        if (!layoutDirTmp.mkdir()) {
            throw new DownloadException("create uuid dir tmp error");
        }

        downloader.downloadZip(getLayoutZipUrl(), layoutDirTmp, new Downloader.DownloadCallback() {
            @Override
            public void onSuccess(String url, File desFile) {
                unzipToTmp(desFile, layoutDirTmp);
                if (state != State.Fail) {
                    File layoutDir = new File(dirPath, taskUUID);
                    layoutDirTmp.renameTo(layoutDir);

                    resourceState = new ResourceState(getPptPageSize());
                    downloadNextResource();
                }
            }

            @Override
            public void onFailure(String url) {

            }
        });
    }

    private int getPptPageSize() {
        File infoFile = new File(new File(dirPath, taskUUID), DEFAULT_INFO_FILENAME);
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
            return;
        }

        int index = resourceState.nextIndex();
        if (resourceDownloaded(index)) {
            downloadNextResource();
            return;
        }

        File resourceZip = new File(new File(dirPath, taskUUID), getResourceZipName(index));
        if (resourceZip.exists()) {
            resourceZip.delete();
        }
        downloader.downloadZip(getResourceZipUrl(index), resourceZip, new Downloader.DownloadCallback() {
            @Override
            public void onSuccess(String url, File desFile) {
                unzipToTmp(desFile, resourceZip);
                if (state != State.Fail) {
                    File layoutDir = new File(dirPath, taskUUID);
                    resourceZip.renameTo(layoutDir);

                    resourceState = new ResourceState(getPptPageSize());
                    downloadNextResource();
                }
            }

            @Override
            public void onFailure(String url) {

            }
        });
    }

    private boolean resourceDownloaded(int index) {
        return false;
    }

    @NotNull
    private String getResourceZipName(int index) {
        return "resource" + (index + 1) + ".zip";
    }

    private void unzipToTmp(File desFile, File layoutDirTmp) {
        try {
            Utils.unzip(desFile, layoutDirTmp);
        } catch (Exception e) {
            state = State.Fail;
        }
    }

    /**
     * @param index 从0开始的所有
     */
    public void onPageChangeTo(int index) {

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
    String getResourceZipUrl(int index) {
        if (taskUUID == null) {
            throw new DownloadException("taskUUID should not be null !");
        }
        return domain + "/dynamicConvert/" + taskUUID + "/" + "resource" + (index + 1) + ".zip";
    }

    enum State {
        Init,

        DOWNLOADING,

        Fail,

        SUCCESS,
    }
}
