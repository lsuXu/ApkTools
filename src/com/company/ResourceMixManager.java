package com.company;

import com.company.bean.ApkKey;
import com.company.tools.CommonUtils;
import com.company.tools.FileUtils;
import com.company.tools.ProcessUtil;
import org.json.JSONObject;

import java.io.*;
import java.util.function.Consumer;

public class ResourceMixManager {

    public static boolean toZip(String sourceFolder, String targetName, IExecuteCallback callback) throws IOException, InterruptedException {
        //将文件夹重新压缩为zip文件
        Process process = ProcessUtil.execute("/bin/sh", "-c", "cd " + sourceFolder + " && zip -q -r " + targetName + " ./*");
        if (callback != null) {
            callback.onInfo(process.getInputStream());
            callback.onError(process.getErrorStream());
        }
        int value = process.waitFor();
        System.out.println(value);
        process.destroy();
        return value == 0;
    }

    public static boolean mixResource(String zipPath, IExecuteCallback callback) throws IOException, InterruptedException {
        new File(zipPath).renameTo(new File("app.apk"));
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "java -jar AndResGuard-cli-1.2.15.jar app.apk -config config.xml -out outapk"});
        if (callback != null) {
            callback.onError(process.getErrorStream());
            callback.onInfo(process.getInputStream());
        }
        int value = process.waitFor();
        return value == 0;
    }

    public static boolean signApk(String apkFilePath, ApkKey key, IExecuteCallback callback) throws InterruptedException, IOException {
        FileUtils.deleteFile(apkFilePath);
        System.out.println("key = " + key.toString());
        Process process;
        process = ProcessUtil.execute("/bin/sh", "-c", "./zipalign -f -v 4 ./outapk/app_unsigned.apk app_aligned.apk");
        if (callback != null) {
            callback.onInfo(process.getInputStream());
            callback.onError(process.getErrorStream());
        }
        if (process.waitFor() == 0) {
            System.out.println("打包完成");
            process = ProcessUtil.execute("/bin/sh", "-c", "./apksigner sign -verbose --ks " + key.getkFilePath() + " --v1-signing-enabled false --v2-signing-enabled true --ks-key-alias " + key.getKa() + " --ks-pass pass:" + key.getKsp() + " --key-pass pass:" + key.getKp() + " --out ./app_aligned_signed.apk app_aligned.apk");
            if (callback != null) {
                callback.onInfo(process.getInputStream());
                callback.onError(process.getErrorStream());
            }
        } else {
            System.out.println("打包失败");
            return false;
        }
        boolean result = process.waitFor() == 0;
        System.out.println("签名" + (result ? "成功" : "失败"));
        return result;
    }

    public static boolean installApk(String apkPath, IExecuteCallback callback) throws IOException, InterruptedException {
        Process process = ProcessUtil.execute("/bin/sh", "-c",String.format("adb install %s", apkPath));
        if (callback != null) {
            callback.onInfo(process.getInputStream());
            callback.onError(process.getErrorStream());
        }
        return process.waitFor() == 0;
    }

    public static void tryModifyFlutterResources() throws IOException {
        InputStream is = new FileInputStream("./app/assets/flutter_assets/AssetManifest.json");
        int iAvail = is.available();
        byte[] bytes = new byte[iAvail];
        is.read(bytes);
        is.close();
        JSONObject jsonObject = new JSONObject(new String(bytes));
        jsonObject.keySet().forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                String f = jsonObject.getJSONArray(s).getString(0);
                String[] d = f.split("/");
                String name = d[d.length - 1];
                String nLastName = CommonUtils.genFileName();//随机的文件名
                String newName = f.replaceAll(name, nLastName);
                new File("./app/assets/flutter_assets/" + f).renameTo(new File("./app/assets/flutter_assets/" + newName));//资源文件重命名
                jsonObject.getJSONArray(s).put(0, newName);//修改资源文件引用地址
            }
        });
        OutputStream os = new FileOutputStream("./app/assets/flutter_assets/AssetManifest.json");
        os.write(jsonObject.toString().getBytes());
        os.close();
    }
}
