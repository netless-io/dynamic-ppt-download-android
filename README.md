## 介绍
DynamicPptDownload 用于加载缓存动态Ppt资源，提升移动端动态Ppt体验。

## 集成
DynamicPptDownload 使用jitpack ，集成时需添加以下仓库依赖
```
allprojects {
    repositories {
        // Add jitpack repository
        maven { url 'https://jitpack.io' }
    }
}
```

添加依赖
```
dependencies {
    // Get the latest version number through the release notes.
    implementation 'com.github.netless-io:dynamic-ppt-download-android:1.0.0'
}
```

### 方式一、预加载
为保证首页加载体验，建议提前缓存Ppt资源
```
// 初始化
DownloadHelper helper = DownloadHelper.getInstance();
// 设置缓存文件夹
helper.setPPTCacheDir(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/pptdownload");

// 启动下载任务
DownloadTask task = helper.newTask("a6f9d430d7c211ebae6f1dc0589306eb", "https://white-cn-doc-convert.oss-cn-hangzhou.aliyuncs.com");
task.start();
```

### 方式二、RoomState变更时调用

```
// 初始化
DownloadHelper helper = DownloadHelper.getInstance();
helper.setPPTCacheDir(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/pptdownload");

// 加入房间成功后调用
DownloadHelper.updateRoomState(mRoom.getRoomState())

// room状态更新时
@Override
public void onRoomStateChanged(RoomState modifyState) {
    // ...other logic
    DownloadHelper.updateRoomState(modifyState);
}
```

### WebViewClient拦截
缓存的资源需要用于请求拦截，具体拦截方式可参考 [PptCacheWebViewClient](https://github.com/duty-os/white-sdk-android/blob/master/app/src/main/java/com/herewhite/demo/test/PptCacheWebViewClient.java)