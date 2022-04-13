package vincent.m3u8_downloader.loader;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import vincent.m3u8_downloader.M3U8DownloaderConfig;

public class LoaderTaskManager {
    private static final int MAX_RETRY_NUM = Integer.MAX_VALUE;              //最多重试7次
    private static final long DELAY_MILLIS = 3000L;             //每次延迟执行间隔,ms
//    private static final long[] DELAY_MILLIS = {
//            0L,
//            2000L,
//            4000L,
//            8000L,
//            16000L,
//            32000L,
//            64000L,
//            64000L,
//            64000L,
//    };           //每次延迟执行间隔,ms

    private int threadCount;

    private DelayQueue<LoaderTask> tasks;

    private ExecutorService executor;

    private volatile boolean isRunning = false;

    public LoaderTaskManager(){
        SSLAgent.getInstance().trustAllHttpsCertificates();
        this.tasks = new DelayQueue<LoaderTask>();
//        this.doingTasks = new ArrayList<LoaderTask>();
        threadCount = M3U8DownloaderConfig.getThreadCount();
        executor = Executors.newFixedThreadPool(threadCount);
    }

    //添加一个新任务到队列中
    public void addTask(LoaderInfo info){
        this.addTask(info, 0, MAX_RETRY_NUM);
    }

    //添加一个新任务到队列中
    public void addOrder(LoaderInfo info, int maxRetryNum){
        this.addTask(info, DELAY_MILLIS, maxRetryNum);
    }

    //添加一个新任务到队列中
    public void addTask(LoaderInfo info, long delay, int maxRetryNum){
        LoaderTask task = new LoaderTask(info, delay, maxRetryNum);
        this.tasks.add(task);
        if(isRunning){
            submit();
        }
    }

    public int getSize(){
        return this.tasks.size();
    }

    public void clear(){
        this.tasks.clear();
    }

//    private synchronized LoaderTask take() throws InterruptedException {
//        return tasks.take();
//    }

    private void submit(){
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if(!isRunning){
                    return;
                }
                try{
                    LoaderTask task = tasks.take();
                    if(task.getState()==LoaderTask.STATE_DOING){
                        if(isRunning){
                            submit();
                        }
                    }else{
                        task.run();
                        if(task.getState()==LoaderTask.STATE_DOING){
                            if(isRunning){
                                submit();
                            }
                        }else if(task.getState() == LoaderTask.STATE_RETRY){
                            task.setDelay(DELAY_MILLIS);
                            tasks.add(task);
                            if(isRunning){
                                submit();
                            }
                        }else if(task.getState() == LoaderTask.STATE_FAILED){
                            Log.e("Error",String.format("the %s download failed.", task.getLoaderInfo().getUri()));
                        }
                    }
                }catch (Exception e){
                    Log.e("task error. %s", e.getMessage());
                    //重试
                    submit();
                    Log.e("Error","task error. restart success");
                    e.printStackTrace();
                }
            }
        });
    }

    private void submitAll(){
        if(executor==null){
            executor = Executors.newFixedThreadPool(threadCount);
        }
        int size = tasks.size();
        for(int i=0;i<size;i++){
            submit();
        }
    }

    public void start(){
        Log.e("DEBUG","LoaderTaskManager Start");
        if(!isRunning){
            isRunning = true;
            submitAll();
        }
    }

    public void pause(){
        stop();
    }

    public void stop(){
        if(!this.isRunning){
           return;
        }
        Log.e("DEBUG","LoaderTaskManager Stop");
        this.isRunning = false;
        if(executor != null){
            executor.shutdownNow();
            executor = null;
        }
        tasks.clear();
    }
}
