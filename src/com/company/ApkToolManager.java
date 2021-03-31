package com.company;

import org.json.JSONArray;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * apktool工具处理脚本，提供替换图标，修改包名，修改APP名称功能
 * 开始解析之前，需要先执行{@link #prepareParsing()}，
 * 解析完成后，需要调用{@link #close()}来保存完成的修改
 */
public class ApkToolManager {
    private static final String tempSaveFolderName = "apkTemp";
    //检索文件，修改报名
    private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    //脚本执行回调
    private ExecuteCallback callback;
    //apk源路径/apk处理完成的保存路径
    private final String sourceApkPath, targetPath;
    //apk解压后的临时存储文件夹
    private final File apkTempFolder;
    //配置信息，来自AndroidManifest.xml文件
    private String configInfo;

    public ApkToolManager(String sourceApkPath, String targetPath) {
        this.sourceApkPath = sourceApkPath;
        this.targetPath = targetPath;
        this.apkTempFolder = new File(tempSaveFolderName);
    }

    //检查参数和环境是否正常，同时解析出xml配置文件
    public boolean prepareParsing() throws IOException, InterruptedException {
        if (sourceApkPath == null || sourceApkPath.isEmpty() || targetPath == null || targetPath.isEmpty()) {
            throw new IllegalArgumentException(String.format("param error by sourceApkPath = %s,targetPath = %s", sourceApkPath, targetPath));
        }
        File apkSource = new File(sourceApkPath);
        if (!apkSource.exists() || !apkSource.isFile()) {
            throw new IOException(String.format("file %s is not exist", apkSource));
        }
        //临时保存apk反编译的路径,先清除
        if (apkTempFolder.exists() && apkTempFolder.isDirectory()) {
            boolean success = FileUtils.deleteFile(apkTempFolder);
            System.out.println("tempFile is exist,delete tempFile " + (success ? "success" : "error"));
            if (!success) {
                return false;
            }
        }
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", String.format("java -jar apktool.jar d %s -o %s", apkSource.getAbsolutePath(), tempSaveFolderName)});
        callbackInfo(process);
        int value = process.waitFor();
        if (value == 0) {
            System.out.println("解压成功");
            //读取文件信息
            configInfo = FileUtils.getFileContent(new File(apkTempFolder, MANIFEST_FILE_NAME));
            return !configInfo.isEmpty();
        } else {
            System.out.println("解压失败：errorCode =" + value);
            return false;
        }
    }

    /**
     * 替换图标
     *
     * @param iconPath 图标路径
     * @return <code>true</code> replace success ,else false
     * @throws IOException
     */
    public boolean doReplaceIcon(String iconPath) throws IOException {
        String iconKey = getSourceIconName(configInfo);
        System.out.printf("apk icon info =%s%n", iconKey);
        return replaceIcon(apkTempFolder, iconKey, iconPath);
    }

    /**
     * 替换APK包名
     *
     * @param packageName 目标包名
     * @return <code>true</code> replace success ,else false
     */
    public boolean doReplacePackageName(String packageName) {
        String sourcePackage = getSourcePackage(configInfo);
        System.out.printf("apk package =" + sourcePackage + ",replace to " + packageName);
        if (sourcePackage.isEmpty()) {
            System.out.println("解析原包名失败");
            return false;
        }
        //替换项目包名
        configInfo = replaceProjectPackage(sourcePackage, configInfo, packageName);
        //替换所有provide包裹下的包名
        configInfo = replaceProviderPackage(sourcePackage, configInfo, packageName);
        return true;
    }

    /**
     * 替换apk的名称
     *
     * @param apkName apk名称
     * @return <code>true</code> replace success ,else false
     */
    public boolean doChangeApkName(String apkName) {
        Pattern pattern = Pattern.compile("(<application.*android:label=\")([^\"]*)(\".*>)");
        Matcher matcher = pattern.matcher(configInfo);
        if (!matcher.find()) {
            return false;
        }
        System.out.println("source packageName =" + matcher.group(2) + "replace to " + apkName);
        configInfo = matcher.replaceAll(matcher.group(1) + apkName + matcher.group(3));
        return true;
    }

    /**
     * 关闭文件，将会将清单文件恢复，生成APK文件到目标路径
     *
     * @return <code>true</code>,generate apk success,save  path {@link #targetPath},else false;
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean close() throws IOException, InterruptedException {
        //先把AndroidManifest清单文件写回去
        OutputStream os = new FileOutputStream(new File(apkTempFolder, MANIFEST_FILE_NAME));
        os.write(configInfo.getBytes());
        os.flush();
        os.close();
        return zipApk(tempSaveFolderName, targetPath);
    }

    /**
     * 设置执行回调信息,处理流输出
     *
     * @param callback callback
     */
    public void setCallback(ExecuteCallback callback) {
        this.callback = callback;
    }

    /**
     * 替换图标
     *
     * @param apkFolder apkTool解压后的根文件夹
     * @param iconKey   icon标识，从{@link #MANIFEST_FILE_NAME}文件中解析出
     *                  符合正则规则("@.*"/.*")，示例：@mipmap/icon_launcher
     * @param iconPath  替换的图标路径，次路径文件将被作为新图标使用
     * @return <code>true</code> replace success ,else false;
     * @throws IOException
     */
    private static boolean replaceIcon(File apkFolder, String iconKey, String iconPath) throws IOException {
        File iconFile = new File(iconPath);
        if (!apkFolder.exists() || !apkFolder.isDirectory()) {
            throw new IOException(String.format("folder %s is not exist", apkFolder.getAbsolutePath()));
        }
        if (!iconFile.exists() || iconFile.isDirectory()) {
            throw new IOException(String.format("iconFile %s is not exist", iconPath));
        }
        List<File> iconFiles = searchIconFile(apkFolder, iconKey);
        System.out.println("iconFiles =\n" + new JSONArray(iconFiles).toString());
        for (File file : iconFiles) {
            FileUtils.copyFileUsingFileChannels(iconFile, file);
            System.out.println(String.format("copy icon from %s to %s", iconFile.getAbsolutePath(), file.getAbsolutePath()));
        }
        return true;
    }

    /**
     * 搜索所有需要替换的图标文件
     *
     * @param apkFolder apk解压后的根目录
     * @param iconKey   icon标识，从{@link #MANIFEST_FILE_NAME}文件中解析出
     *                  符合正则规则("@.*"/.*")，示例：@mipmap/icon_launcher
     * @return 所有需要被替换的图标文件列表
     * @throws IOException
     */
    private static List<File> searchIconFile(File apkFolder, String iconKey) throws IOException {
        if (!apkFolder.exists() || !apkFolder.isDirectory()) {
            throw new IOException(String.format("folder %s is not exist", apkFolder.getAbsolutePath()));
        }
        Pattern pattern = Pattern.compile("@([a-z]*)/([0-9a-z_.]*)");
        Matcher matcher = pattern.matcher(iconKey);
        if (!matcher.find()) {
            throw new IllegalArgumentException("iconKey must like (@mipmap/ic_launcher),but the iconKey =" + iconKey);
        }
        List<File> iconFiles = new ArrayList<>();
        String folderName = matcher.group(1);
        String iconName = matcher.group(2);
        for (File child : new File(apkFolder, "res").listFiles()) {
            if (child.getName().startsWith(folderName) && child.isDirectory()) {
                for (File subChild : child.listFiles()) {
                    if (subChild.isFile() && subChild.getName().startsWith(iconName + ".")) {
                        iconFiles.add(subChild);
                    }
                }
            }
        }
        return iconFiles;
    }

    /**
     * 匹配包名信息
     *
     * @param content {@link #MANIFEST_FILE_NAME}文件的内容
     * @return A parsed App packageName or an empty String
     */
    private static String getSourcePackage(String content) {
        Pattern pattern = Pattern.compile("package=\"(([0-9a-zA-Z]+[.]?)+)[\"]");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    /**
     * 匹配应用使用的图标key,格式例如（@mipmap/ic_launcher）
     *
     * @param content {@link #MANIFEST_FILE_NAME}文件的内容
     * @return A parsed App icon info,or an empty String
     */
    private static String getSourceIconName(String content) {
        Pattern pattern = Pattern.compile("<application.*android:icon=\"([^\"]*)\".*>");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    /**
     * 替换provider内的包名
     *
     * @param sourcePackage 源包名
     * @param content       替换字符串信息，来自{@link #MANIFEST_FILE_NAME}的内容
     * @param targetPackage 目标包名
     * @return 替换后的string 信息
     */
    private String replaceProviderPackage(String sourcePackage, String content, String targetPackage) {
        Pattern pattern = Pattern.compile("(.+<provider.*)(" + sourcePackage + ")(.*(/?)>)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String res = matcher.group(1) + targetPackage + matcher.group(3);
            matcher.appendReplacement(sb, res);
            System.out.println("replace res =" + res);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 替换package内的包名
     *
     * @param sourcePackage 源包名
     * @param content       替换字符串信息，来自{@link #MANIFEST_FILE_NAME}的内容
     * @param targetPackage 目标包名
     * @return 替换后的string 信息
     */
    private String replaceProjectPackage(String sourcePackage, String content, String targetPackage) {
        Pattern pattern = Pattern.compile("(package=\")(" + sourcePackage + ")([\"])");
        Matcher matcher = pattern.matcher(content);
        String result = content;
        while (matcher.find()) {
            String res = matcher.group(1) + targetPackage + matcher.group(3);
            result = matcher.replaceAll(res);
        }
        return result;
    }

    /**
     * 将解压后的APK文件夹重新打包为APK文件
     *
     * @param folderName  APK文件夹
     * @param saveApkPath 保存路径，即APK文件的目标路径
     * @return <code>true</code> zip success ,else false
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean zipApk(String folderName, String saveApkPath) throws InterruptedException, IOException {
        //临时保存apk反编译的路径
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", String.format("java -jar apktool.jar b %s -o %s", folderName, saveApkPath)});
        callbackInfo(process);
        int value = process.waitFor();
        System.out.println(value);
        return true;
    }

    /**
     * 回调执行结果，通过Runtime.getRuntime()方法执行的回调信息，将在这里统一输出
     *
     * @param process
     */
    private void callbackInfo(Process process) {
        if (callback != null) {
            callback.onError(process.getErrorStream());
            callback.onInfo(process.getInputStream());
        }
    }

    /**
     * 脚本执行回调
     */
    interface ExecuteCallback {
        void onError(InputStream errorStream);

        void onInfo(InputStream infoStream);
    }
    
}
