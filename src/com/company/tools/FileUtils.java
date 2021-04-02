package com.company.tools;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    /**
     * 删除文件
     *
     * @param filePath 待删除的文件路径
     * @return <code>true</code> delete success ,else fail
     */
    public static boolean deleteFile(String filePath) {
        return deleteFile(new File(filePath));
    }

    /**
     * 删除文件
     *
     * @param file 待删除的文件路径
     * @return <code>true</code> delete success ,else fail
     */
    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                return file.delete();
            } else {
                boolean success = true;
                for (File child : file.listFiles()) {
                    success = success && deleteFile(child);
                }
                return file.delete() && success;
            }
        } else {
            return true;
        }
    }

    /**
     * 复制文件
     *
     * @param source 源文件
     * @param dest   目标文件
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
     *
     * @param configFile 文件路径
     * @return 文件内容
     * @throws IOException
     */
    public static String getFileContent(File configFile) throws IOException {
        InputStream is = new FileInputStream(configFile);
        int iAvail = is.available();
        byte[] bytes = new byte[iAvail];
        is.read(bytes);
        is.close();
        return new String(bytes);
    }

    /**
     * 解压zip文件
     * @param zipFile   目标zip文件
     * @param descDir   解压后的文件夹
     * @throws IOException
     */
    public static void unzip(File zipFile, String descDir) throws IOException {
        unzip(zipFile,descDir,null);
    }

    /**
     * 解压Zip文件夹
     * @param zipFile   目标zip文件
     * @param descDir   解压后保存的文件夹
     * @param ops   日志输出流
     * @throws IOException
     */
    public static void unzip(File zipFile, String descDir, OutputStream ops) throws IOException {
        if(zipFile == null || descDir == null || descDir.isEmpty()){
            throw new IOException("zipFile or descDir can not be null");
        }
        if(!zipFile.exists() || !zipFile.isFile()){
            throw new IOException(String.format("can not find file %s",zipFile.getAbsolutePath()));
        }
        //删除已经存在的文件夹
        FileUtils.deleteFile(descDir);
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        //解决zip文件中有中文目录或者中文文件
        ZipFile zip = new ZipFile(zipFile, Charset.forName("GBK"));
        for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String zipEntryName = entry.getName();
            InputStream in = zip.getInputStream(entry);
            String outPath = (descDir + File.separator + zipEntryName).replaceAll("\\*", "/");
            int index = outPath.lastIndexOf('/');
            //判断路径是否存在,不存在则创建文件路径
            if(index >0) {
                File file = new File(outPath.substring(0, index));
                if (!file.exists()) {
                    file.mkdirs();
                }
            }
            //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
            if (new File(outPath).isDirectory()) {
                continue;
            }
            //输出文件路径信息
            if (ops != null) {
                ops.write(outPath.getBytes());
            }
            System.out.println("unzip filename =" + outPath  + " ,hasNext = " + entries.hasMoreElements());
            OutputStream out = new FileOutputStream(outPath);
            byte[] buf1 = new byte[1024];
            int len;
            while ((len = in.read(buf1)) > 0) {
                out.write(buf1, 0, len);
            }
            in.close();
            out.close();
        }
    }

    private void compress(File sourceFile, ZipOutputStream zos, String name,
                          boolean KeepDirStructure) throws Exception {
        byte[] buf = new byte[1024];
        if (sourceFile.isFile()) {
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if (KeepDirStructure) {
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }
            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (KeepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), KeepDirStructure);
                    }
                }
            }
        }
    }

}
