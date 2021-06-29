package com.netlest.pptdownload;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import com.netless.pptdownload.DownloadHelper;
import com.netless.pptdownload.DownloadTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.test).setOnClickListener(v -> {
            startTask();
        });

        requestPermission();
    }

    private void startTask() {
        DownloadHelper helper = DownloadHelper.getInstance();
        helper.setDomain("https://white-cn-doc-convert.oss-cn-hangzhou.aliyuncs.com");
        helper.setPPTCacheDir(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/pptdownload");

        DownloadTask task = helper.newTask("a6f9d430d7c211ebae6f1dc0589306eb");
        task.start();
    }


    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS}, 1000);
            }
        }
    }
}