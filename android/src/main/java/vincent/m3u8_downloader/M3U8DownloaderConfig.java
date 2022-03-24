package vincent.m3u8_downloader;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import vincent.m3u8_downloader.utils.SPHelper;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/24
 * 描    述: M3U8Downloader 配置类
 * ================================================
 */
public class M3U8DownloaderConfig {

    private static final String TAG_SAVE_DIR = "TAG_SAVE_DIR_M3U8";
    private static final String TAG_THREAD_COUNT = "TAG_THREAD_COUNT_M3U8";
    private static final String TAG_CONN_TIMEOUT = "TAG_CONN_TIMEOUT_M3U8";
    private static final String TAG_READ_TIMEOUT = "TAG_READ_TIMEOUT_M3U8";
    private static final String TAG_DEBUG = "TAG_DEBUG_M3U8";
    private static final String TAG_SHOW_NOTIFICATION = "TAG_SHOW_NOTIFICATION_M3U8";
    private static final String TAG_IS_CONVERT = "TAG_IS_CONVERT";

    public static M3U8DownloaderConfig build(Context context){
        SPHelper.init(context);
        return new M3U8DownloaderConfig();
    }

    public M3U8DownloaderConfig setSaveDir(String saveDir){
        SPHelper.putString(TAG_SAVE_DIR, saveDir);
        return this;
    }

    public static String getSaveDir(){
        return SPHelper.getString(TAG_SAVE_DIR, Environment.getExternalStorageDirectory().getPath() + File.separator + "M3u8Downloader");
    }

    public M3U8DownloaderConfig setThreadCount(int threadCount){
        if (threadCount > 100) threadCount = 100;
        if (threadCount <= 0) threadCount = 1;
        SPHelper.putInt(TAG_THREAD_COUNT, threadCount);
        return this;
    }

    public static int getThreadCount(){
       return SPHelper.getInt(TAG_THREAD_COUNT, 30);
    }

    public M3U8DownloaderConfig setConnTimeout(int connTimeout){
        SPHelper.putInt(TAG_CONN_TIMEOUT, connTimeout);
        return this;
    }

    public static int getConnTimeout(){
        return SPHelper.getInt(TAG_CONN_TIMEOUT, 10 * 1000);
    }

    public M3U8DownloaderConfig setReadTimeout(int readTimeout){
        SPHelper.putInt(TAG_READ_TIMEOUT, readTimeout);
        return this;
    }

    public static int getReadTimeout(){
        return SPHelper.getInt(TAG_READ_TIMEOUT, 10 * 1000);
    }


    public M3U8DownloaderConfig setDebugMode(boolean debug){
        SPHelper.putBoolean(TAG_DEBUG, debug);
        return this;
    }

    public static boolean isDebugMode(){
        return SPHelper.getBoolean(TAG_DEBUG, false);
    }

    public M3U8DownloaderConfig setShowNotification(boolean show){
        SPHelper.putBoolean(TAG_SHOW_NOTIFICATION, show);
        return this;
    }

    public static boolean isShowNotification(){
        return SPHelper.getBoolean(TAG_SHOW_NOTIFICATION, true);
    }

    public M3U8DownloaderConfig setIsConvert(boolean isConvert){
        SPHelper.putBoolean(TAG_IS_CONVERT, isConvert);
        return this;
    }

    public static boolean isConvert(){
        return SPHelper.getBoolean(TAG_IS_CONVERT, true);
    }
}
