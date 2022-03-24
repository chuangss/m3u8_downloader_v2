package vincent.m3u8_downloader.bean;

import vincent.m3u8_downloader.loader.LoaderInfo;
import vincent.m3u8_downloader.utils.MD5Utils;
import vincent.m3u8_downloader.utils.StringUtils;

import java.math.BigDecimal;

public class M3U8Ts {

    private int type;
    private String url;
    private BigDecimal seconds;
    private int fileSize;
    private String method;
    private String iv;

    private LoaderInfo loaderInfo;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BigDecimal getSeconds() {
        return seconds;
    }

    public void setSeconds(BigDecimal seconds) {
        this.seconds = seconds;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public LoaderInfo getLoaderInfo() {
        return loaderInfo;
    }

    public void setLoaderInfo(LoaderInfo loaderInfo) {
        this.loaderInfo = loaderInfo;
    }

    public String obtainFullUrl(String hostUrl, String basePath){
        if (url == null) {
            return null;
        }
        if (url.startsWith("http")) {
            return url;
        }else if (url.startsWith("//")) {
            if(hostUrl.startsWith("https")){
                return "https:".concat(url);
            }else{
                return "http:".concat(url);
            }
        }else {
            String same = lookForSamePath(url, basePath);
            if(StringUtils.isEmpty(same)){
                if(isSimpleFileName(url)){
                    if(url.startsWith("/")){
                        return basePath.concat(url.substring(1));
                    }else{
                        return basePath.concat(url);
                    }
                }else{
                    if(url.startsWith("/")){
                        return hostUrl.concat(url);
                    }else{
                        return hostUrl.concat("/").concat(url);
                    }
                }
            }else{
                String base = basePath.substring(0, basePath.lastIndexOf(same));
                return base.concat(url);
            }
        }
    }

    public String obtainEncodeTsFileName(){
        String fix = ".ts";
        if(this.type == M3U8TsType.KEY){
            fix = ".key";
        }
        if (url == null)return "error"+fix;
        return MD5Utils.encode(url).concat(fix);
    }

    private String lookForSamePath(String url, String base){
        while (base.endsWith("/")){
            base = base.substring(0, base.length()-1);
        }

        while (true){
            if(base.endsWith(url)){
                return url;
            }
            int i = url.lastIndexOf("/");
            if(i>0){
                url = url.substring(0, i);
            }else{
                return "";
            }
        }
    }

    private boolean isSimpleFileName(String url){
        String[] arr = url.split("/");
        int count=0;
        for (String i : arr){
            if(i!=null&&i.trim().length()>0){
                count++;
            }
        }
        return count<2;
    }
}
