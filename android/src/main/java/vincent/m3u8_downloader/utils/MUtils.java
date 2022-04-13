package vincent.m3u8_downloader.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import vincent.m3u8_downloader.M3U8DownloaderConfig;
import vincent.m3u8_downloader.M3U8EncryptHelper;
import vincent.m3u8_downloader.bean.M3U8;
import vincent.m3u8_downloader.bean.M3U8Ts;
import vincent.m3u8_downloader.bean.M3U8TsType;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/18
 * 描    述: 工具类
 * ================================================
 */

public class MUtils {

    /**
     * 将Url转换为M3U8对象
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static M3U8 parseIndex(String url) throws IOException, URISyntaxException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        URI baseUri = new URI(url);
        String basePath = url.substring(0, url.lastIndexOf("/") + 1);
        int index = url.indexOf("/", url.indexOf("//")+2);
        String host = url.substring(0, index>-1?index:url.length());

        M3U8 ret = new M3U8();
        ret.setHost(host);
        ret.setBasePath(basePath);

        String line;
        final BigDecimal zero = new BigDecimal("0.0");
        BigDecimal seconds = zero;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                if (line.startsWith("#EXTINF:")) {
                    line = line.substring(8);
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    seconds = new BigDecimal(line);//Float.parseFloat(line);
                } else if (line.startsWith("#EXT-X-KEY:")) {
                    M3U8Ts m3U8Key = new M3U8Ts();
                    m3U8Key.setType(M3U8TsType.KEY);
                    line = line.split("#EXT-X-KEY:")[1];
                    String[] arr = line.split(",");
                    for (String s : arr) {
                        if (s.contains("=")) {
                            String k = s.split("=")[0];
                            String v = s.split("=")[1];
                            if (k.equals("URI")) {
                                // 去获取key
                                v = v.replaceAll("\"", "");
                                v = v.replaceAll("'", "");
                                String keyUrl = baseUri.resolve(v).toString();
                                m3U8Key.setUrl(keyUrl);
//                                BufferedReader keyReader = new BufferedReader(new InputStreamReader(new URL(keyUrl).openStream()));
//                                m3U8Key.setUrl(keyReader.readLine());
                                Log.e("keyUrl========", keyUrl);
                            } else if (k.equals("IV")) {
                                m3U8Key.setIv(v);
                            } else if (k.equals("METHOD")){
                                m3U8Key.setMethod(v);
                            }
                        }
                    }
                    if(!StringUtils.isEmpty(m3U8Key.getMethod())&&!"NONE".equalsIgnoreCase(m3U8Key.getMethod())&&!StringUtils.isEmpty(m3U8Key.getUrl())){
                        ret.addTs(m3U8Key);
                    }
                }else if (line.startsWith("#EXT-X-ENDLIST")) {
                    break;
                }
                continue;
            }
            if (line.endsWith("m3u8")) {
                return parseIndex(baseUri.resolve(line).toString());
            }
            Log.e("line============>",line);
            if(!line.trim().isEmpty()){
                M3U8Ts m3U8Ts = new M3U8Ts();
                m3U8Ts.setType(M3U8TsType.TS);
                m3U8Ts.setUrl(line);
                m3U8Ts.setSeconds(seconds==null?zero:seconds);
                ret.addTs(m3U8Ts);
                seconds = zero;
            }
        }
        reader.close();

        return ret;
    }



    /**
     * 清空文件夹
     */
    public static boolean clearDir(File dir) {
        if (dir.exists()) {// 判断文件是否存在
            if (dir.isFile()) {// 判断是否是文件
                return dir.delete();// 删除文件
            } else if (dir.isDirectory()) {// 否则如果它是一个目录
                File[] files = dir.listFiles();// 声明目录下所有的文件 files[];
                for (int i = 0; i < files.length; i++) {// 遍历目录下所有的文件
                    clearDir(files[i]);// 把每个文件用这个方法进行迭代
                }
                return dir.delete();// 删除文件夹
            }
        }
        return true;
    }


    private static float KB = 1024;
    private static float MB = 1024 * KB;
    private static float GB = 1024 * MB;

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size >= GB) {
            return String.format("%.1f GB", size / GB);
        } else if (size >= MB) {
            float value = size / MB;
            return String.format(value > 100 ? "%.0f MB" : "%.1f MB", value);
        } else if (size >= KB) {
            float value = size / KB;
            return String.format(value > 100 ? "%.0f KB" : "%.1f KB", value);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * 生成AES-128加密本地m3u8索引文件，ts切片和m3u8文件放在相同目录下即可
     *
     * @param m3u8Dir
     * @param m3U8
     */
    public static File createLocalM3U8(File m3u8Dir, String fileName, M3U8 m3U8, String encryptKey) throws IOException {
        File m3u8File = new File(m3u8Dir, fileName);
        BufferedWriter bfw = new BufferedWriter(new FileWriter(m3u8File, false));
        bfw.write("#EXTM3U\n");
        bfw.write("#EXT-X-VERSION:3\n");
        bfw.write("#EXT-X-MEDIA-SEQUENCE:0\n");
        //bfw.write("#EXT-X-ALLOW-CACHE:YES\n");
        int maxDuration = 0;
        for (M3U8Ts m3U8Ts : m3U8.getTsList()) {//最大时长
            if(m3U8Ts.getSeconds()!=null&&m3U8Ts.getSeconds().floatValue() > maxDuration){
                maxDuration = (int)Math.ceil(m3U8Ts.getSeconds().floatValue());
            }
        }
        maxDuration++;
        bfw.write("#EXT-X-TARGETDURATION:"+maxDuration+"\n");
        for (M3U8Ts m3U8Ts : m3U8.getTsList()) {
            File file;
            try {
                String name = M3U8EncryptHelper.encryptFileName(encryptKey, m3U8Ts.obtainEncodeTsFileName());
                file = new File(m3u8Dir, name);
            } catch (Exception e) {
                file = new File(m3u8Dir,  m3U8Ts.obtainEncodeTsFileName());
            }
            String filePath = file.getAbsolutePath();
            if(m3U8Ts.getType()==M3U8TsType.KEY){
                if(StringUtils.isEmpty(m3U8Ts.getIv())){
                    if(StringUtils.isEmpty(m3U8Ts.getMethod())){
                        bfw.write(String.format("#EXT-X-KEY:METHOD=AES-128,URI=\"%s\"\n", filePath));
                    }else{
                        bfw.write(String.format("#EXT-X-KEY:METHOD=%s,URI=\"%s\"\n", m3U8Ts.getMethod(), filePath));
                    }
                }else{
                    bfw.write(String.format("#EXT-X-KEY:METHOD=%s,IV=%s,URI=\"%s\"\n", m3U8Ts.getMethod(),
                            m3U8Ts.getIv(), filePath));
                }
            }else{
                bfw.write("#EXTINF:" + m3U8Ts.getSeconds().toString() + ",\n");
                //Log.e("TAG", "createLocalM3U8: " + filePath);
                bfw.write(filePath);
                bfw.newLine();
            }
        }
        bfw.write("#EXT-X-ENDLIST");
        bfw.flush();
        bfw.close();
        return m3u8File;
    }

    public static byte[] readFile(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte[] buffer = new byte[length];
        fis.read(buffer);
        fis.close();
        return buffer;
    }

    public static void saveFile(byte[] bytes, String fileName) throws IOException {
        File file = new File(fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    public static void saveFile(String text, String fileName) throws IOException {
        File file = new File(fileName);
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(text);
        out.flush();
        out.close();
    }

    public static String getSaveFileDir(String url) {
        return M3U8DownloaderConfig.getSaveDir() + File.separator + MD5Utils.encode(url);
    }

}
