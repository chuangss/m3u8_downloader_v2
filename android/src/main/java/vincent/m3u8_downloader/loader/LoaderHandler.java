package vincent.m3u8_downloader.loader;

import vincent.m3u8_downloader.bean.M3U8;

public abstract class LoaderHandler {
    public abstract void onStart(LoaderInfo loaderInfo);
    public abstract void onProgress(LoaderInfo loaderInfo);
    public abstract void onErrorWait(LoaderInfo loaderInfo);
    public abstract void success(LoaderInfo loaderInfo);
    public abstract void fail(LoaderInfo loaderInfo);
    public abstract void onM3u8Parse(M3U8 m3U8);
}
