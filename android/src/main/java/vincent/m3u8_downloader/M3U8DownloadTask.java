package vincent.m3u8_downloader;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jeffmony.m3u8library.VideoProcessManager;
import com.jeffmony.m3u8library.listener.IVideoTransformListener;
import com.jeffmony.m3u8library.utils.LogUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import vincent.m3u8_downloader.bean.M3U8;
import vincent.m3u8_downloader.bean.M3U8Ts;
import vincent.m3u8_downloader.loader.LoaderHandler;
import vincent.m3u8_downloader.loader.LoaderInfo;
import vincent.m3u8_downloader.loader.LoaderInfoType;
import vincent.m3u8_downloader.loader.LoaderTaskManager;
import vincent.m3u8_downloader.utils.AES128Utils;
import vincent.m3u8_downloader.utils.FileUtils;
import vincent.m3u8_downloader.utils.M3U8Log;
import vincent.m3u8_downloader.utils.MUtils;
import vincent.m3u8_downloader.utils.StringUtils;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/17
 * 描    述: 单独M3U8下载任务
 * ================================================
 */
class M3U8DownloadTask {
    private static final int WHAT_ON_ERROR = 1001;
    private static final int WHAT_ON_PROGRESS = 1002;
    private static final int WHAT_ON_SUCCESS = 1003;
    private static final int WHAT_ON_START_DOWNLOAD = 1004;
    private static final int WHAT_ON_CONVERT = 1005;

    private OnTaskDownloadListener onTaskDownloadListener;
    //加密Key，默认为空，不加密
    private String encryptKey = null;
    private String m3u8FileName = "local.m3u8";
    private String completeFileName = "complete.json";
    //文件保存的路径
    private String saveDir;
    //当前下载完成的文件个数
    private AtomicInteger curTs = new AtomicInteger(0);
    //总文件的个数
    private volatile int totalTs = 0;
    //单个文件的大小
    private volatile long itemFileSize = 0;
    //所有文件的大小
    private volatile long totalFileSize = 0;

    /**
     * 当前已经在下完成的大小
     */
    private long curLength = 0;
    /**
     * 任务是否正在运行中
     */
    private boolean isRunning = false;
    /**
     * 定时任务
     */
    private Timer netSpeedTimer;
    private M3U8 currentM3U8;

    private boolean writeDirty = false;
    private Map<String, String> loadedUrls;
    private LoaderTaskManager loaderTaskManager;

    private volatile boolean firstStart = false;

    private String myUrl;
//    private Handler mHandler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message msg) {
//            //Log.e("MSG", String.format("what: %s", msg.what));
//            switch (msg.what) {
//                case WHAT_ON_ERROR:
//                    if (msg.obj != null) {
//                        try {
//                            onTaskDownloadListener.onError((Throwable) msg.obj);
//                        } catch (Exception e) {
//                            Log.e("TAG", e.toString());
//                        }
//                    }
//                    break;
//                case WHAT_ON_CONVERT:
//                    onTaskDownloadListener.onConverting();
//                    break;
//                case WHAT_ON_START_DOWNLOAD:
//                    if(totalTs==0){
//                        onTaskDownloadListener.onStartDownload(1, 0);
//                    }else{
//                        onTaskDownloadListener.onStartDownload(totalTs, curTs);
//                    }
//                    break;
//                case WHAT_ON_PROGRESS:
//                    onTaskDownloadListener.onDownloading(totalFileSize, itemFileSize, totalTs, curTs);
//                    break;
//                case WHAT_ON_SUCCESS:
//                    onTaskDownloadListener.onSuccess(currentM3U8);
//                    break;
//            }
//            return true;
//        }
//    });
    private WeakHandler mHandler = new WeakHandler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            //Log.e("MSG", String.format("what: %s", msg.what));
            switch (msg.what) {
                case WHAT_ON_ERROR:
                    if (msg.obj != null) {
                        try {
                            onTaskDownloadListener.onError((Throwable) msg.obj);
                        } catch (Exception e) {
                            Log.e("TAG", e.toString());
                        }
                    }
                    break;
                case WHAT_ON_CONVERT:
                    onTaskDownloadListener.onConverting();
                    break;
                case WHAT_ON_START_DOWNLOAD:
                    if(totalTs==0){
                        onTaskDownloadListener.onStartDownload(1, 0);
                    }else{
                        onTaskDownloadListener.onStartDownload(totalTs, curTs.get());
                    }
                    break;
                case WHAT_ON_PROGRESS:
                    onTaskDownloadListener.onDownloading(totalFileSize, itemFileSize, totalTs, curTs.get());
                    break;
                case WHAT_ON_SUCCESS:
                    onTaskDownloadListener.onSuccess(currentM3U8);
                    break;
            }
            return true;
        }
    });

    public M3U8DownloadTask() {
    }

    public String getMyUrl() {
        return myUrl;
    }

    public long getCurLength() {
        return curLength;
    }

    /**
     * 开始下载
     *
     * @param url
     * @param onTaskDownloadListener
     */
    public void download(final String url, OnTaskDownloadListener onTaskDownloadListener) {
        myUrl = url;
        saveDir = MUtils.getSaveFileDir(url);
        M3U8Log.d("start download ,SaveDir: " + saveDir);
        mHandler.sendEmptyMessage(WHAT_ON_START_DOWNLOAD);
        this.onTaskDownloadListener = onTaskDownloadListener;
        if (!isRunning()) {
            this.isRunning = true;
            this.firstStart = false;
            if(loaderTaskManager==null){
                loaderTaskManager=new LoaderTaskManager();
            }
            loaderTaskManager.clear();
            loaderTaskManager.start();
            getM3U8Info(url);
        } else {
            handlerError(new Throwable("Task running"));
        }
    }

    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public String getEncryptKey() {
        return encryptKey;
    }

    /**
     * 获取任务是否正在执行
     *
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }

    private void onFirstStart(){
        if(!firstStart){
            firstStart = true;
            this.onTaskDownloadListener.onStart();
        }
    }

    /**
     * 先获取m3u8信息
     *
     * @param url
     */
    private void getM3U8Info(final String url) {
        Log.d("DEBUG", String.format("the m3u8 info: %s", url));
        if(currentM3U8==null||!currentM3U8.getUrl().equals(url))
        {
            LoaderInfo loaderInfo = new LoaderInfo();
            loaderInfo.setType(LoaderInfoType.M3U8_INFO);
            loaderInfo.setUri(url);
            loaderInfo.setHandler(new LoaderHandler() {
                @Override
                public void onStart(LoaderInfo info) {
                    //Log.d("DEBUG", String.format("start get m3u8 info: %s", url));
                    onFirstStart();
                }

                @Override
                public void onProgress(LoaderInfo info) {

                }

                @Override
                public void onErrorWait(LoaderInfo info) {
                    Log.d("DEBUG", String.format("error wait m3u8 info: %s", url));
                }

                @Override
                public void success(LoaderInfo info) {

                }

                @Override
                public void fail(LoaderInfo info) {

                }

                @Override
                public void onM3u8Parse(M3U8 m3U8) {
                    //Log.d("DEBUG", String.format("parsed m3u8 info: %s", url));
                    m3U8.setUrl(url);
                    currentM3U8 = m3U8;
                    if (M3U8DownloaderConfig.isConvert()) {
                        File file = new File(saveDir + ".mp4");
                        if (file.exists()) {
                            mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
                            return;
                        }
                    }
                        // 开始下载
                    if(isRunning){
                        startDownload(m3U8);
                    }
                }
            });
            loaderTaskManager.addTask(loaderInfo);
        }else{
            if (M3U8DownloaderConfig.isConvert()) {
                File file = new File(saveDir + ".mp4");
                if (file.exists()) {
                    mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
                    return;
                }
            }

            if(isRunning){
                startDownload(currentM3U8);
            }
        }
    }


    private void m3u8ToMp4(File m3u8File) {
        final File dir = new File(saveDir);
        final String mp4FilePath = saveDir + ".mp4";
        VideoProcessManager.getInstance().transformM3U8ToMp4(m3u8File.getAbsolutePath(), mp4FilePath, new IVideoTransformListener() {
            int sProgress=0;

            @Override
            public void onTransformProgress(float progress) {
                //LogUtils.i("TAG", "onTransformProgress progress=" + progress);
                int iProgress = (int)progress;
                if(iProgress%10==0&&iProgress!=sProgress&&iProgress<100){
                    sProgress=iProgress;
                    mHandler.sendEmptyMessage(WHAT_ON_CONVERT);
                }
            }

            @Override
            public void onTransformFinished() {
                LogUtils.i("TAG", "onTransformFinished");
                // 设置文件路径
                currentM3U8.setM3u8FilePath(mp4FilePath);
                // 合并成功，删除m3u8和ts文件
                MUtils.clearDir(dir);
                stop();
                mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
            }

            @Override
            public void onTransformFailed(Exception e) {
                LogUtils.i("TAG", "onTransformFailed, e=" + e.getMessage());
                mHandler.sendEmptyMessage(WHAT_ON_ERROR);
            }
        });
    }

    /**
     * 开始下载
     * 关于断点续传，每个任务会根据url进行生成相应Base64目录
     * 如果任务已经停止、开始下载之前，下一次会判断相关任务目录中已经下载完成的ts文件是否已经下载过了，下载了就不再下载
     *
     * @param m3U8
     */
    private void startDownload(final M3U8 m3U8) {
        final File dir = new File(saveDir);
        //没有就创建
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if(netSpeedTimer!=null){
            netSpeedTimer.cancel();
            netSpeedTimer = null;
        }

        loadComplete(dir);

        totalTs = m3U8.getTsList().size();
        curTs.set(0);
        curLength = 0;
        isRunning = true;
        final String basePath = m3U8.getBasePath();
        final String host = m3U8.getHost();

        for(final M3U8Ts ts : m3U8.getTsList()){
            File file;
            try {
                String fileName = M3U8EncryptHelper.encryptFileName(encryptKey, ts.obtainEncodeTsFileName());
                file = new File(dir + File.separator + fileName);
            } catch (Exception e) {
                file = new File(dir + File.separator + ts.obtainEncodeTsFileName());
            }

            String tsUrl = ts.obtainFullUrl(host, basePath);
            //tsUrl = replaceStr(tsUrl);

            LoaderInfo loaderInfo = ts.getLoaderInfo()!=null?ts.getLoaderInfo():new LoaderInfo();
            loaderInfo.setType(LoaderInfoType.FILE);
            loaderInfo.setUri(tsUrl);
            loaderInfo.setTarget(file);
            loaderInfo.setHandler(new LoaderHandler() {
                @Override
                public void onStart(LoaderInfo info) {
                    onFirstStart();
                    //Log.d("DEBUG", String.format("start ts: %s", info.getUri()));
                    mHandler.sendEmptyMessage(WHAT_ON_START_DOWNLOAD);
                }

                @Override
                public void onProgress(LoaderInfo info) {

                }

                @Override
                public void onErrorWait(LoaderInfo info) {
                    Log.d("DEBUG", String.format("error wait ts: %s", info.getUri()));
                }

                @Override
                public void success(LoaderInfo info) {
                    int count = curTs.incrementAndGet();
                    loadedUrls.put(info.getUri(), info.getTotal()+"");
                    writeDirty = true;
                    itemFileSize = info.getTotal();
                    if(count > totalTs - 10){
                        Log.d("DEBUG", String.format("current: %s total: %s", count, totalTs));
                        Log.d("DEBUG", String.format("task size: %s", loaderTaskManager.getSize()));
                    }
                    if(count >= totalTs){
                        onTaskComplete();
                    }else{
                        mHandler.sendEmptyMessage(WHAT_ON_PROGRESS);
                    }
                }

                @Override
                public void fail(LoaderInfo info) {

                }

                @Override
                public void onM3u8Parse(M3U8 m3U8) {

                }
            });
            ts.setLoaderInfo(loaderInfo);
            if(loaderInfo.isCompleted()){
                curTs.incrementAndGet();
            } else if(loadedUrls.containsKey(loaderInfo.getUri())){
                curTs.incrementAndGet();
                int total = 0;
                try {
                    total = Integer.parseInt(loadedUrls.get(loaderInfo.getUri()));
                }catch (Exception e){
                    e.printStackTrace();
                }
                loaderInfo.setTotal(total);
                loaderInfo.setLoaded(total);
            }else{
                loaderTaskManager.addTask(loaderInfo);
            }
        }

        curLength = getLoadedLength(m3U8);
        if(curTs.get()>=totalTs){
            onTaskComplete();
        }else{
            if(netSpeedTimer==null){
                netSpeedTimer = new Timer();
            }
            netSpeedTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    writeComplete(dir);
                    curLength = getLoadedLength(m3U8);
                    onTaskDownloadListener.onProgress(curLength);
                }
            }, 0, 1000);
        }
    }

    private void onTaskComplete(){
        Log.d("DEBUG", String.format("download ok: %s", totalTs));
        this.stopDownload();
        this.writeComplete(new File(saveDir));
        curLength = getLoadedLength(currentM3U8);
        if (M3U8DownloaderConfig.isConvert()) {
            mHandler.sendEmptyMessage(WHAT_ON_CONVERT);
            Log.d("DEBUG", String.format("message ok"));
            currentM3U8.setDirFilePath(saveDir);
            try{
                File m3u8File = MUtils.createLocalM3U8(new File(saveDir), m3u8FileName, currentM3U8, encryptKey);
                Log.d("DEBUG", String.format("createLocal ok"));
                ///转换mp4
                m3u8ToMp4(m3u8File);
                Log.d("DEBUG", String.format("try change ok"));
            }catch (Exception e){
                Log.d("DEBUG", String.format("error"));
                mHandler.sendEmptyMessage(WHAT_ON_ERROR);
                e.printStackTrace();
            }
        }else{
            stop();
            mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
        }
    }

    private void loadComplete(File parent){
        if(loadedUrls==null||loadedUrls.size()==0){
            final File completeFile = new File(parent, completeFileName);
            if(completeFile.exists()){
                String str = FileUtils.readFileContent(completeFile);
                loadedUrls = StringUtils.split2HashMap(str, "\r\n", "=");
            }
        }
        if(loadedUrls==null){
            loadedUrls = new ConcurrentHashMap<>();
        }
    }

    private void writeComplete(File parent){
        if(!writeDirty)return;
        if(loadedUrls!=null&&loadedUrls.size()>0){
            StringBuilder sb = new StringBuilder();
            Iterator it = loadedUrls.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
                sb.append(entry.getKey()).append("=")
                        .append(entry.getValue()).append("\r\n");
            }
            final File completeFile = new File(parent, completeFileName);
            FileUtils.writeFileContent(completeFile, sb.toString());
        }
        if(loadedUrls==null){
            loadedUrls = new ConcurrentHashMap<>();
        }
    }

    private long getLoadedLength(M3U8 m3U8){
        long len = 0;
        for(final M3U8Ts ts : m3U8.getTsList()){
            LoaderInfo loaderInfo = ts.getLoaderInfo();
            if(loaderInfo!=null){
                len += loaderInfo.getLoaded();
            }
        }
        return len;
    }

    /**
     * 替换url重复的内容
     */
    public static String replaceStr(String url) {
        StringBuilder strUrl = new StringBuilder();
        String[] strList = url.split("/");
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList(strList));
        String[] linkedList = linkedHashSet.toArray(new String[100]);
        for (int i = 0; i < linkedList.length; i++) {
//            Log.e("======>", String.valueOf(linkedHashSet));
            if (linkedList[i] != null) {
                if (i == 0) {
                    if (linkedList[i].contains("https:")) {
                        linkedList[i] = "https://";
                    } else if (linkedList[i].contains("http:")) {
                        linkedList[i] = "http://";
                    }
                    strUrl.append(linkedList[i]);
                } else {
                    if (i == 1) {
                    } else {
                        strUrl.append(linkedList[i]).append("/");
                    }
                }
            }
        }
        return strUrl.substring(0, strUrl.length() - 1);
    }

    /**
     * 通知异常
     *
     * @param e
     */
    private void handlerError(Throwable e) {
        e.printStackTrace();
        if (!"Task running".equals(e.getMessage())) {
            stopDownload();
            stop();
        }
        //不提示被中断的情况
        if ("thread interrupted".equals(e.getMessage())) {
            return;
        }
        Message msg = Message.obtain();
        msg.obj = e;
        msg.what = WHAT_ON_ERROR;
        mHandler.sendMessage(msg);
    }

    /**
     * 停止任务
     */
    private void stopDownload() {
        if (netSpeedTimer != null) {
            netSpeedTimer.cancel();
            netSpeedTimer = null;
        }
        if(loaderTaskManager!=null){
            loaderTaskManager.stop();
        }
    }

    /**
     * 停止任务
     */
    public void stop() {
        stopDownload();
        firstStart = false;
        isRunning = false;
    }

    public File getM3u8File(String url) {
        try {
            return new File(MUtils.getSaveFileDir(url), m3u8FileName);
        } catch (Exception e) {
            M3U8Log.e(e.getMessage());
        }
        return null;
    }
}
