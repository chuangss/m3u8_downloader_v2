package vincent.m3u8_downloader.loader;

import java.io.File;

public class LoaderInfo {

    private static int globalId = 0;

    private int total = 0;
    private int loaded = 0;
    private int from = 0;
    private int to = -1;
    private File target;
    private String uri;
    private int type;
    private int id;
    LoaderHandler handler;

    public LoaderInfo(){
        globalId++;
        id = globalId;
    }

    public boolean isCompleted(){
        return total > 0 && loaded == total;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLoaded() {
        return loaded;
    }

    public void setLoaded(int loaded) {
        this.loaded = loaded;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public File getTarget() {
        return target;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getId() {
        return id;
    }

    public LoaderHandler getHandler() {
        return handler;
    }

    public void setHandler(LoaderHandler handler) {
        this.handler = handler;
    }
}
