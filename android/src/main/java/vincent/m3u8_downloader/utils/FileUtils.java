package vincent.m3u8_downloader.utils;

import java.io.*;

public class FileUtils {

    /**
     * 获得target相对于source的相对路径
     * @param target	: 目标文件路径
     * @param source	: 原文件路径
     * @return
     */
    public static String getRelativePath(File target, File source) {
        String t = target.getAbsolutePath();
        String s = source.getAbsolutePath();
        if("\\".equals(File.separator)){
            t = StringUtils.replaceAll(t, "\\", "/");
            s = StringUtils.replaceAll(s, "\\", "/");
        }
        return getRelativePath(t, s);
    }

    /**
     * 获得targetPath相对于sourcePath的相对路径
     * @param targetPath	: 目标文件路径
     * @param sourcePath	: 原文件路径
     * @return
     */
    public static String getRelativePath(String targetPath, String sourcePath) {
        StringBuffer pathSB = new StringBuffer();

        if (targetPath.indexOf(sourcePath) == 0){
            pathSB.append(targetPath.replace(sourcePath, ""));
        }else {
            String[] sourcePathArray = sourcePath.split("/");
            String[] targetPathArray = targetPath.split("/");
            int i = 0;
            for(; i<Math.min(sourcePathArray.length, targetPathArray.length);i++){
                if(sourcePathArray[i].equals(targetPathArray[i])){
                    continue;
                }
                break;
            }
            for (int j = i; j < sourcePathArray.length; j++){
                pathSB.append("../");
            }
            for (;i < targetPathArray.length; i++){
                pathSB.append(targetPathArray[i] + (i<targetPathArray.length-1?"/":""));
            }
        }

        return pathSB.toString();
    }

    /**
     * 删除文件(夹)
     * @param file	: 文件路径
     * @return
     */
    public static void deleteFile(File file) {
        if(!file.exists()){
            return;
        }

        if(file.isDirectory()){
            File[] childs = file.listFiles();
            for(int i=0;i<childs.length;i++){
                File child = childs[i];
                deleteFile(child);
            }
        }

        file.delete();
    }

    /**
     * 复制文件(夹),自动识别是文件还是文件夹
     * @param sourceFile: 原文件路径
     * @param targetFile: 目标文件路径
     * @return
     */
    public static void copyFiles(File sourceFile,File targetFile){
        if (sourceFile.isDirectory()) {
            copyDirectiory(sourceFile, targetFile);
        }else{
            copyFile(sourceFile, targetFile);
        }
    }

    /**
     * 复制文件
     * @param sourceFile: 原文件路径
     * @param targetFile: 目标文件路径
     * @return
     */
    public static void copyFile(File sourceFile,File targetFile){

        FileInputStream input=null;
        BufferedInputStream inBuff=null;
        FileOutputStream output = null;
        BufferedOutputStream outBuff = null;
        try{
            // 新建文件输入流并对它进行缓冲
            input = new FileInputStream(sourceFile);
            inBuff=new BufferedInputStream(input);

            // 新建文件输出流并对它进行缓冲
            output = new FileOutputStream(targetFile);
            outBuff=new BufferedOutputStream(output);

            int bufferSize = 1024*16;

            // 缓冲数组
            byte[] b = new byte[bufferSize];
            int len;
            while ((len =inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //关闭流
            if (inBuff != null){
                try{
                    inBuff.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (outBuff != null){
                try{
                    outBuff.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (output != null){
                try{
                    output.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (input != null){
                try{
                    input.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 复制文件夹
     * @param sourceDir: 原文件路径
     * @param targetDir: 目标文件路径
     * @return
     */
    public static void copyDirectiory(File sourceDir, File targetDir) {
        // 新建目标目录
        if(!targetDir.exists() || !targetDir.isDirectory()){
            targetDir.mkdirs();
        }

        // 获取源文件夹当前下的文件或目录
        File[] file = sourceDir.listFiles();
        for (int i = 0; i < file.length; i++) {
            // 源文件
            File sourceFile=file[i];
            // 目标文件
            File targetFile=new File(targetDir.getAbsolutePath()
                    +File.separator+file[i].getName());
            if (sourceFile.isDirectory()) {
                copyDirectiory(sourceFile, targetFile);
            }else{
                copyFile(sourceFile,targetFile);
            }
        }
    }

    /**
     * 读取文本文件
     * @param file: 文件
     * @return String
     */
    public static String readFileContent(File file) {
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            char[] chars = new char[1024];
            reader = new BufferedReader(new FileReader(file));
            int len = 0;
            while ((len=reader.read(chars))!=-1){
                sbf.append(chars, 0, len);
            }
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return "";
    }

    /**
     * 写入文本文件
     * @param file: 文件
     * @param content: 内容
     * @return boolean
     */
    public static boolean writeFileContent(File file, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 读取文件中指定字节
     * @param file: 文件
     * @param offset: 偏移
     * @param length: 长度
     * @return byte[]
     */
    public static byte[] read(File file, long offset, int length) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.skip(offset);

            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);

            return buffer;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            if(fis!=null){
                try {
                    fis.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

}
