package com.netless.pptdownload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author fenglibin
 * <p>
 * 下载器
 */
class Downloader {
    private OkHttpClient client = new OkHttpClient();

    Downloader() {

    }

    /**
     * 不处理逻辑，由Task统一处理目录的创建及异常判断
     *
     * @param zipUrl
     * @param desZip
     */
    public void downloadZipTo(String zipUrl, File desZip, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(zipUrl)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                DownloadLogger.e("download error", e);
                if (callback != null) {
                    callback.onFailure(zipUrl);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (FileOutputStream fos = new FileOutputStream(desZip, false)) {
                    fos.write(response.body().bytes());
                } catch (IOException e) {
                    if (callback != null) {
                        callback.onFailure(zipUrl);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(zipUrl, desZip);
                }
            }
        });
    }

    public void cancel(String zipUrl) {

    }

    interface DownloadCallback {
        void onSuccess(String url, File desFile);

        void onFailure(String url);
    }
}
