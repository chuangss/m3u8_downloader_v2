package vincent.m3u8_downloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import vincent.m3u8_downloader.bean.M3U8;
import vincent.m3u8_downloader.bean.M3U8Task;
import vincent.m3u8_downloader.bean.M3U8TaskState;
import vincent.m3u8_downloader.utils.M3U8Log;
import vincent.m3u8_downloader.utils.MUtils;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/17
 * 描    述: M3U8下载器
 * ================================================
 */
public class M3U8Downloader {
    private static M3U8Downloader instance = null;


    private long currentTime;
    private M3U8Task currentM3U8Task;
    private DownloadQueue downLoadQueue;
    private List<M3U8DownloadTask> downloaderList;
    private M3U8DownloadTask currentM3U8DownLoadTask;
    private OnM3U8DownloadListener onM3U8DownloadListener;

    private M3U8Downloader() {
        downLoadQueue = new DownloadQueue();
        downloaderList = new ArrayList<>();
    }

    public static M3U8Downloader getInstance() {
        if (null == instance) {
            instance = new M3U8Downloader();
        }
        return instance;
    }

    private synchronized M3U8DownloadTask getM3u8DownloadTask(String url){
        int len=downloaderList.size();
        for(int i=0;i<len;i++){
            M3U8DownloadTask task = downloaderList.get(i);
            if(url.equals(task.getMyUrl())){
                return task;
            }
        }

        M3U8DownloadTask downloadTask = new M3U8DownloadTask();
        downloaderList.add(downloadTask);
        if(downloaderList.size() > 30){
            downloaderList.remove(0);
        }
        return downloadTask;
    }

    /**
     * 防止快速点击引起ThreadPoolExecutor频繁创建销毁引起crash
     *
     * @return
     */
    private boolean isQuicklyClick() {
        boolean result = false;
        if (System.currentTimeMillis() - currentTime <= 100) {
            result = true;
            M3U8Log.d("is too quickly click!");
        }
        currentTime = System.currentTimeMillis();
        return result;
    }


    /**
     * 下载下一个任务，直到任务全部完成
     */
    private void downloadNextTask() {
        startDownloadTask(downLoadQueue.poll());
    }

    private void pendingTask(M3U8Task task) {
        task.setState(M3U8TaskState.PENDING);
        if (onM3U8DownloadListener != null) {
            onM3U8DownloadListener.onDownloadPending(task);
        }
    }

    /**
     * 下载任务
     * 如果当前任务在下载列表中则认为是暂停
     * 否则入队等候下载
     *
     * @param url
     */
    public void download(String url, String name) {
        if (TextUtils.isEmpty(url) || isQuicklyClick()) return;
        M3U8Task task = new M3U8Task(url);
        if (downLoadQueue.contains(task)) {
            task = downLoadQueue.getTask(url);
            if (task.getState() == M3U8TaskState.PAUSE || task.getState() == M3U8TaskState.ERROR) {
                startDownloadTask(task);
            } else {
                pause(url);
            }
        } else {
            Log.e("TAG", "download:  添加任务到队列" + task);
            downLoadQueue.offer(task);
            startDownloadTask(task);
        }
    }

    /**
     * 暂停，如果此任务正在下载则暂停，否则无反应
     * 只支持单一任务暂停，多任务暂停请使用{@link #pause(java.util.List)}
     *
     * @param url
     */
    public void pause(String url) {
        Log.e("TAG", "pause:  暂停进来了" + url);
        //Log.e("TAG", "pause:  currentM3U8Task.getUrl()" + currentM3U8Task.getUrl());
        if (TextUtils.isEmpty(url)) return;
        Log.e("TAG", "pause:  downLoadQueue" + downLoadQueue);
        M3U8Task task = downLoadQueue.getTask(url);
        Log.e("TAG", "pause:  task" + task);
        if (task != null) {
            task.setState(M3U8TaskState.PAUSE);
            Log.e("TAG", "pause:  onM3U8DownloadListener start");
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadPause(task);
            }
            Log.e("TAG", "pause:  onM3U8DownloadListener end");


            //Log.e("TAG", "url.equals(currentM3U8Task.getUrl())" + url.equals(currentM3U8Task.getUrl()));
            if (currentM3U8Task!=null&&url.equals(currentM3U8Task.getUrl())) {
                Log.e("TAG", "pause: " + currentM3U8Task.getUrl());
                currentM3U8DownLoadTask.stop();
                downloadNextTask();
            } else {
                Log.e("TAG", "remove: ");
                downLoadQueue.remove(task);
            }
        }
    }

    /**
     * 批量暂停
     *
     * @param urls
     */
    public void pause(List<String> urls) {
        if (urls == null || urls.size() == 0) return;
        boolean isCurrentTaskPause = false;
        for (String url : urls) {
            if (downLoadQueue.contains(new M3U8Task(url))) {
                M3U8Task task = downLoadQueue.getTask(url);
                if (task != null) {
                    task.setState(M3U8TaskState.PAUSE);
                    if (onM3U8DownloadListener != null) {
                        onM3U8DownloadListener.onDownloadPause(task);
                    }
                    if (task.equals(currentM3U8Task)) {
                        currentM3U8DownLoadTask.stop();
                        isCurrentTaskPause = true;
                    }
                    downLoadQueue.remove(task);
                }
            }
        }
        if (isCurrentTaskPause) startDownloadTask(downLoadQueue.peek());
    }

    /**
     * 检查m3u8文件是否存在
     *
     * @param url
     * @return
     */
    public boolean checkM3U8IsExist(String url) {
        try {
            return currentM3U8DownLoadTask.getM3u8File(url).exists();
        } catch (Exception e) {
            M3U8Log.e(e.getMessage());
        }
        return false;
    }

    /**
     * 得到m3u8文件路径
     *
     * @param url
     * @return
     */
    public String getM3U8Path(String url) {
        String path;
        try {
            path = currentM3U8DownLoadTask.getM3u8File(url).getPath();
        } catch (Exception e) {
            path = MUtils.getSaveFileDir(url) + File.separator + "local.m3u8";
        }
        return path;
    }

    public boolean isRunning() {
        return currentM3U8DownLoadTask.isRunning();
    }

    /**
     * if task is the current task , it will return true
     *
     * @param url
     * @return
     */
    public boolean isCurrentTask(String url) {
        return !TextUtils.isEmpty(url)
                && downLoadQueue.peek() != null
                && downLoadQueue.peek().getUrl().equals(url);
    }


    public void setOnM3U8DownloadListener(OnM3U8DownloadListener onM3U8DownloadListener) {
        this.onM3U8DownloadListener = onM3U8DownloadListener;
    }

    public void setEncryptKey(String encryptKey) {
        currentM3U8DownLoadTask.setEncryptKey(encryptKey);
    }

    public String getEncryptKey() {
        return currentM3U8DownLoadTask.getEncryptKey();
    }

    private void startDownloadTask(M3U8Task task) {
        if (task == null) return;
        pendingTask(task);
        if (!downLoadQueue.isHead(task)) {
            M3U8Log.d("start download task, but task is running: " + task.getUrl());
            return;
        }

        if (task.getState() == M3U8TaskState.PAUSE) {
            M3U8Log.d("start download task, but task has pause: " + task.getUrl());
            return;
        }
        try {
            currentM3U8Task = task;
            M3U8Log.d("====== start downloading ===== " + task.getUrl());
            currentM3U8DownLoadTask = getM3u8DownloadTask(task.getUrl());
            currentM3U8DownLoadTask.download(task.getUrl(), onTaskDownloadListener);
        } catch (Exception e) {
            M3U8Log.e("startDownloadTask Error:" + e.getMessage());
        }
    }

    /**
     * 取消任务
     *
     * @param url
     */
    public void cancel(String url) {
        pause(url);
    }

    /**
     * 批量取消任务
     *
     * @param urls
     */
    public void cancel(List<String> urls) {
        pause(urls);
    }

    /**
     * 取消任务,删除缓存
     *
     * @param url
     */
    public void cancelAndDelete(final String url, @Nullable final OnDeleteTaskListener listener) {
        pause(url);
        if (listener != null) {
            listener.onStart();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                removeDownloadTask(url);
                String saveDir = MUtils.getSaveFileDir(url);
                // 删除文件夹
                boolean isDelete = MUtils.clearDir(new File(saveDir));
                // 删除mp4文件
                if (isDelete) {
                    isDelete = MUtils.clearDir(new File(saveDir + ".mp4"));
                }
                if (listener != null) {
                    if (isDelete) {
                        listener.onSuccess();
                    } else {
                        listener.onFail();
                    }
                }
            }
        }).start();
    }

    /**
     * 批量取消任务,删除缓存
     *
     * @param urls
     * @param listener
     */
    public void cancelAndDelete(final List<String> urls, @Nullable final OnDeleteTaskListener listener) {
        pause(urls);
        if (listener != null) {
            listener.onStart();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isDelete = true;
                for (String url : urls) {
                    removeDownloadTask(url);
                    isDelete = isDelete && MUtils.clearDir(new File(MUtils.getSaveFileDir(url)));
                }
                if (listener != null) {
                    if (isDelete) {
                        listener.onSuccess();
                    } else {
                        listener.onFail();
                    }
                }
            }
        }).start();
    }

    private M3U8DownloadTask removeDownloadTask(String url){
        int len=downloaderList.size();
        for(int i=0;i<len;i++){
            M3U8DownloadTask task = downloaderList.get(i);
            if(url.equals(task.getMyUrl())){
                return downloaderList.remove(i);
            }
        }
        return null;
    }

    private OnTaskDownloadListener onTaskDownloadListener = new OnTaskDownloadListener() {
        private long lastLength;
        private float downloadProgress;
        public volatile boolean isConvert = false;
        public volatile boolean isSuccess = false;

        @Override
        public void onStartDownload(int totalTs, int curTs) {
            M3U8Log.d("onStartDownload: " + totalTs + "|" + curTs);
            currentM3U8Task.setState(M3U8TaskState.DOWNLOADING);
            downloadProgress = 1.0f * curTs / totalTs;
        }

        @Override
        public void onDownloading(long totalFileSize, long itemFileSize, int totalTs, int curTs) {
            if (!currentM3U8DownLoadTask.isRunning()) return;
            M3U8Log.d("onDownloading: " + totalFileSize + "|" + itemFileSize + "|" + totalTs + "|" + curTs);

            downloadProgress = 1.0f * curTs / totalTs;

            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadItem(currentM3U8Task, itemFileSize, totalTs, curTs);
            }
        }

        @Override
        public void onSuccess(M3U8 m3U8) {
            isSuccess = true;
            isConvert = false;
            M3U8DownloadTask task = removeDownloadTask(m3U8.getUrl());
            if(task!=null){
                task.stop();
            }
            currentM3U8DownLoadTask.stop();
            currentM3U8Task.setM3U8(m3U8);
            currentM3U8Task.setState(M3U8TaskState.SUCCESS);
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadSuccess(currentM3U8Task);
            }
            M3U8Log.d("m3u8 Downloader onSuccess: " + m3U8);
            downloadNextTask();
        }

        @Override
        public void onProgress(long curLength) {
            if(isSuccess||isConvert){
                return;
            }
            if(lastLength==0){
                lastLength=curLength;
            }
            if (curLength - lastLength > 0) {
                currentM3U8Task.setProgress(downloadProgress);
                currentM3U8Task.setSpeed(curLength - lastLength);
                if (onM3U8DownloadListener != null) {
                    onM3U8DownloadListener.onDownloadProgress(currentM3U8Task);
                }
                lastLength = curLength;
            }
        }

        @Override
        public void onStart() {
            currentM3U8Task.setState(M3U8TaskState.PREPARE);
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadPrepare(currentM3U8Task);
            }
            isConvert = false;
            isSuccess = false;
            lastLength = currentM3U8DownLoadTask==null?0:currentM3U8DownLoadTask.getCurLength();
            M3U8Log.d("onDownloadPrepare: " + currentM3U8Task.getUrl());
        }

        @Override
        public void onConverting() {
            if(isSuccess){
                return;
            }
            M3U8Log.d("onConverting: " + currentM3U8Task.getUrl());
            isConvert = true;
            currentM3U8Task.setState(M3U8TaskState.CONVERT);
            currentM3U8Task.setProgress(1.0f);
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onConverting(currentM3U8Task);
            }
        }

        @Override
        public void onError(Throwable errorMsg) {
            if (errorMsg.getMessage() != null) {
                if (errorMsg.getMessage().contains("ENOSPC")) {
                    currentM3U8Task.setState(M3U8TaskState.ENOSPC);
                } else if (errorMsg.getMessage().contains("thread interrupted")) {
                    currentM3U8Task.setState(M3U8TaskState.PAUSE);
                } else {
                    currentM3U8Task.setState(M3U8TaskState.ERROR);
                }
            }
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadError(currentM3U8Task, errorMsg);
            }
            M3U8Log.e("onError: " + errorMsg.getMessage());
            isConvert = false;
            isSuccess = false;
            downloadNextTask();
        }

    };

}
