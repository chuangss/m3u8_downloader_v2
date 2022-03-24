package vincent.m3u8_downloader.utils;

/**
 * 文件大小转换工具类
 */
public class FileSizeUtils {

    private static String[][] sizeArr = {{"K","1024"},{"M","1024*1024"},{"G","1024*1024*1024"},{"T","1024*1024*1024*1024"}};
    private static String[] bits = {"","K","M","G","T"};

    /**
     * 字符串转成bytes
     * @param sizeStr
     * @return
     */
    public static long convertToBytes(String sizeStr){

        for(int i=0;i<sizeArr.length;i++){
            String sig = sizeArr[i][0];
            String s = sizeArr[i][1];

            sizeStr = replace(sizeStr, sig, s);
            sizeStr = replace(sizeStr, sig.toLowerCase(), s);
        }

        String[] sizes = sizeStr.split("\\*");
        long size = 1;
        for(int i=0;i<sizes.length;i++){
            size *= sizes[i]==null||sizes[i].length()==0?1:Long.parseLong(sizes[i]);
        }

        return  size;
    }

    private static String replace(String str, String oldChar, String newChar){
        int i;
        while ((i = str.indexOf(oldChar)) != -1){
            String nStr = "";
            if(i>0){
                nStr += str.substring(0, i) + "*";
            }
            nStr += newChar;
            if(i<str.length()-1){
                nStr += "*" + str.substring(i+1);
            }
            str = nStr;
        }

        return str;
    }

    public static String convertToString(long bytes){
        long y = 0;
        for(int i=0;i<bits.length;i++){
           if(bytes >= 1024){
               y = bytes%1024;
               bytes = bytes/1024;
           }else{
               y = Math.round(y*100/1024f);
               return bytes+(y>0?"."+y:"")+bits[i];
           }
        }

        y = Math.round(y*100/1024f);
        return bytes+(y>0?"."+y:"")+bits[bits.length-1];
    }

//    /**
//     * @param args
//     */
//    public static void main(String[] args) {
//        String str = "1024K";
//        System.out.println(convertToBytes(str));
//        System.out.println(convertToString(123));
//        System.out.println(convertToString(1230));
//        System.out.println(convertToString(1024));
//        System.out.println(convertToString(10000000));
//        System.out.println(convertToString(1999990000000000000L));
//    }

}
