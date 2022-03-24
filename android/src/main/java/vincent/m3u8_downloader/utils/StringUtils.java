package vincent.m3u8_downloader.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StringUtils {

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static ConcurrentHashMap<String, String> split2HashMap(String str, String regx1, String regx2) {
        ConcurrentHashMap<String, String> result=null;
        if (isEmpty(str)) {
            return null;
        }

        String[] splits = str.split(regx1);
        if (splits != null) {
            result = new ConcurrentHashMap<>();
            for (int i=0;i<splits.length;i++){
                String s = splits[i].trim();
                if (!isEmpty(s)) {
                    String key;
                    String value;
                    int index = s.indexOf(regx2);
                    if(index!=-1){
                        key = s.substring(0, index).trim();
                        value = s.substring(index+1).trim();
                    }else{
                        key = s;
                        value = "";
                    }
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * 替换所有字符串
     * @param str
     * @param oldChar
     * @param newChar
     * @return
     */
    public static String replaceAll(String str, String oldChar, String newChar) {
        while(str.indexOf(oldChar)!=-1){
            str = str.replace(oldChar, newChar);
        }
        return str;
    }

    /**
     * 字符串混淆
     * @param data
     * @param c
     * @return String
     */
    public static String encode(String data, char c){
        char[] chars = data.toCharArray();
        for(int i=0;i<chars.length;i++){
            chars[i] = (char) (chars[i]^c);
        }
        return new String(chars);
    }


    /**
     * 生成随机数字和字母组合
     * @param length
     * @return String
     */
    public static String getCharAndNumRandom(int length) {
        String charStr = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";
        return getRandomString(charStr, length);
    }

    /**
     * 生成随机字母组合
     * @param length
     * @return String
     */
    public static String getCharRandom(int length) {
        String charStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return getRandomString(charStr, length);
    }

    /**
     * 生成随机字符串组合
     * @param length
     * @param length
     * @return String
     */
    public static String getRandomString(String charStr, int length) {
        Random random = new Random();
        StringBuffer valSb = new StringBuffer();
        int charLength = charStr.length();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charLength);
            valSb.append(charStr.charAt(index));
        }

        return valSb.toString();
    }
}
