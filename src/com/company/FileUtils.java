package com.company;

import java.io.*;
import java.nio.channels.FileChannel;

public class FileUtils {

    /**
     * 删除文件
     * @param filePath  待删除的文件路径
     * @return  <code>true</code> delete success ,else fail
     */
    public static boolean deleteFile(String filePath){
        return deleteFile(new File(filePath));
    }

    /**
     * 删除文件
     * @param file  待删除的文件路径
     * @return  <code>true</code> delete success ,else fail
     */
    public static boolean deleteFile(File file){
        if(file.exists()){
            if(file.isFile()){
                return file.delete();
            }else{
                boolean success = true;
                for(File child : file.listFiles()){
                    success = success && deleteFile(child);
                }
                return file.delete() && success;
            }
        }else{
            return true;
        }
    }

    /**
     * 复制文件
     * @param source    源文件
     * @param dest  目标文件
     * @throws IOException
     */
    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    /**
     * 读取文件内容
     * @param configFile    文件路径
     * @return  文件内容
     * @throws IOException
     */
    public static String getFileContent(File configFile) throws IOException{
        InputStream is = new FileInputStream(configFile);
        int iAvail = is.available();
        byte[] bytes = new byte[iAvail];
        is.read(bytes);
        is.close();
        return new String(bytes);
    }

}
