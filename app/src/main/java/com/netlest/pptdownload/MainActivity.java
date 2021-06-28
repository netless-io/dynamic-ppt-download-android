package com.netlest.pptdownload;

import android.os.Bundle;

import com.netless.pptdownload.DownloadHelper;
import com.netless.pptdownload.DownloadTask;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.test).setOnClickListener(v -> {
            startTask();
        });
    }

    private void startTask() {
        // https://white-cn-doc-convert-dev.oss-cn-hangzhou.aliyuncs.com/dynamicConvert/1ef5d020c52511eba118edb06987914d/resources/resource1.zip
        // https://white-cn-doc-convert-dev.oss-cn-hangzhou.aliyuncs.com/dynamicConvert/1ef5d020c52511eba118edb06987914d/layout.zip
        DownloadHelper helper = DownloadHelper.getInstance();
        helper.setDomain("https://white-cn-doc-convert.oss-cn-hangzhou.aliyuncs.com");
        helper.setPPTCacheDir(getCacheDir().getAbsolutePath() + File.pathSeparator + "pptdownload");

        DownloadTask task = helper.newTask("a6f9d430d7c211ebae6f1dc0589306eb");
        task.start();
    }
}