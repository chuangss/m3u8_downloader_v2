package vincent.m3u8_downloader.loader;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import vincent.m3u8_downloader.M3U8DownloaderConfig;
import vincent.m3u8_downloader.bean.M3U8;
import vincent.m3u8_downloader.utils.MUtils;

public class LoaderTask implements Runnable, Delayed {

    public static final int STATE_INIT = 1;        //第一次状态
    public static final int STATE_DOING = 2;     //完成状态
    public static final int STATE_COMPLETE = 3;     //完成状态
    public static final int STATE_RETRY = 4;        //重试状态
    public static final int STATE_FAILED = 5;       //失败状态

    private LoaderInfo loaderInfo;

    private volatile int state;          //任务状态
    private long time;                       //任务执行时间
    private int retryCount;              //已经重试的次数
    private int maxRetryCount;

    public LoaderTask(LoaderInfo loaderInfo, long delayMillis, int maxRetryCount){

        this.loaderInfo = loaderInfo;
        this.state = STATE_INIT;
        this.time = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delayMillis, TimeUnit.MILLISECONDS);
        this.retryCount = 0;
        this.maxRetryCount = maxRetryCount;
    }

    public void setDelay(long delayMillis){
        this.time = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    public LoaderInfo getLoaderInfo() {
        return loaderInfo;
    }

    public void setLoaderInfo(LoaderInfo loaderInfo) {
        this.loaderInfo = loaderInfo;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }


    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        LoaderTask task = (LoaderTask)o;
        long result = this.getTime() - task.getTime();
        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    /**
     * 时间到了，执行逻辑
     */
    @Override
    public void run() {

        if(loaderInfo==null){//关服放入的任务loaderInfo为null
            this.state = STATE_COMPLETE;
            return;
        }

        if(this.state==STATE_DOING||this.state == STATE_COMPLETE){
            return;
        }
        this.state = STATE_DOING;

        try{
            if(retryCount==0){
                //Log.d("DEBUG", String.format("now to execute a new task. id:%s; url : %s; retryNum: %s ",loaderInfo.getId(), loaderInfo.getUri(), retryCount));
            }else{
                Log.d("DEBUG", String.format("now to execute a retry task. id:%s; url : %s; retryNum: %s ", loaderInfo.getId(), loaderInfo.getUri(), retryCount));
            }

            if(loaderInfo.getHandler()!=null){
                loaderInfo.getHandler().onStart(loaderInfo);
            }
            if(this.doLoad(loaderInfo)){
                this.state = STATE_COMPLETE;
                if(loaderInfo.getHandler()!=null){
                    loaderInfo.getHandler().success(loaderInfo);
                }
                return;
            }
        }catch (Exception e){
            Log.e("ERROR", e.getMessage());
            e.printStackTrace();
        }

        this.retryCount++;
        if(this.retryCount > this.maxRetryCount){
            this.state = STATE_FAILED;
            if(loaderInfo.getHandler()!=null){
                loaderInfo.getHandler().fail(loaderInfo);
            }
        }else{
            this.state = STATE_RETRY;
            if(loaderInfo.getHandler()!=null){
                loaderInfo.getHandler().onErrorWait(loaderInfo);
            }
        }
    }

    public boolean doLoad(LoaderInfo loaderInfo){
        if(loaderInfo.getType()==LoaderInfoType.M3U8_INFO){
            return loadM3u8Info(loaderInfo);
        }
        return loadFile(loaderInfo);
    }

    public boolean loadM3u8Info(LoaderInfo loaderInfo){
        try {
            String url = loaderInfo.getUri();
            M3U8 m3u8Info = MUtils.parseIndex(url);
            if(loaderInfo.getHandler()!=null){
                loaderInfo.getHandler().onM3u8Parse(m3u8Info);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadFile(LoaderInfo loaderInfo){
        //download and save data
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        try {
            int from = loaderInfo.getFrom() + loaderInfo.getLoaded();
            int to = loaderInfo.getTo();
            HttpURLConnection connection = (HttpURLConnection) new URL(loaderInfo.getUri()).openConnection();
            connection.setConnectTimeout(M3U8DownloaderConfig.getConnTimeout());
            connection.setReadTimeout(M3U8DownloaderConfig.getReadTimeout());
            if(from>=0&&to>=0){
                connection.setRequestProperty("Range", "bytes=" + from + "-" + to);
            }
            connection.connect();
            if(connection.getResponseCode() != 200){
                return false;
            }
            int totalSize = connection.getContentLength();
            //Log.d("DEBUG", "分片：" + loaderInfo.getId() + "的剩余：" + totalSize);
            totalSize = totalSize + loaderInfo.getLoaded();
            if(loaderInfo.getTotal()!=totalSize){
                loaderInfo.setTotal(totalSize);
                if(loaderInfo.getLoaded()>0){//服务端资源已经发生了变化，需要重新下载了
                    loaderInfo.setLoaded(0);
                    return false;
                }
            }
            inputStream = connection.getInputStream();
            randomAccessFile = new RandomAccessFile(loaderInfo.getTarget(), "rw");
            randomAccessFile.seek(from);
            byte[] buffer = new byte[1024 * 16];
            int readCount = inputStream.read(buffer, 0, buffer.length);
            while (readCount > 0) {
                int loaded = loaderInfo.getLoaded() + readCount;
                //System.out.println("分片：" + this.id + "的剩余：" + totalSize);
                randomAccessFile.write(buffer, 0, readCount);
                loaderInfo.setLoaded(loaded);
                readCount = inputStream.read(buffer, 0, buffer.length);
            }
            //Log.d("DEBUG", "分片：" + loaderInfo.getId() + "的剩余：" + (totalSize-loaderInfo.getLoaded()));
            if(loaderInfo.getLoaded()==loaderInfo.getTotal()){
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(inputStream!=null){
                try {
                    inputStream.close();
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }
            if(randomAccessFile!=null){
                try {
                    randomAccessFile.close();
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }
}