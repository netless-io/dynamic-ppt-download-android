package com.netless.pptdownload;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author fenglibin
 * <p>
 * 下载文件到指定文件，备份文件逻辑由外部业务处理
 */
class Downloader {
    private OkHttpClient client = new OkHttpClient();
    HashMap<String, WeakReference<Call>> callMap = new HashMap<>();

    Downloader() {

    }

    public void downloadToFile(String url, File target, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                DownloadLogger.e("[Downloader] download error", e);
                if (callback != null) {
                    callback.onFailure(url);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    Utils.copyToFile(response.body().byteStream(), target);
                    if (callback != null) {
                        callback.onSuccess(url, target);
                    }
                } catch (IOException e) {
                    if (callback != null) {
                        callback.onFailure(url);
                    }
                }
            }
        });
        callMap.put(url, new WeakReference<>(call));
    }

    public void cancel(String url) {
        WeakReference<Call> callWf = callMap.get(url);
        if (callWf.get() != null) {
            callWf.get().cancel();
        }
    }

    interface DownloadCallback {
        void onSuccess(String url, File desFile);

        void onFailure(String url);
    }
}
