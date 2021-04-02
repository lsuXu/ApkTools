package com.company;

import com.company.bean.SignatureKey;
import com.company.tools.CommonUtils;
import com.company.tools.FileUtils;
import com.company.tools.ProcessUtil;
import org.json.JSONObject;

import java.io.*;
import java.util.function.Consumer;

/**
 * 资源混淆管理器，
 */
public class ResourceMixManager implements IExecuteCallback {
    private final String TEMP_PATH;
    //APK源路径，保存路径
    private final String sourceApk, targetApk;
    //标志是否混淆flutter资源
    private final boolean mixFlutterResource;
    //签名需要的key
    private final SignatureKey key;

    private IExecuteCallback extraCallback;

    public ResourceMixManager(String sourceApk, String targetApk, boolean mixFlutterResource, SignatureKey key) {
        this.sourceApk = sourceApk;
        this.targetApk = targetApk;
        this.mixFlutterResource = mixFlutterResource;
        this.key = key;
        TEMP_PATH = "build_" + System.currentTimeMillis();
    }

    /**
     * 设置执行过程的内容回调
     * @param callback 回调
     */
    public void setCallback(IExecuteCallback callback) {
        this.extraCallback = callback;
    }

    /**
     * 开始任务
     * @return  <code>true</code>,执行成功，else 执行失败
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean startTask() throws IOException, InterruptedException {
        checkParams();
        //先清除临时文件夹
        FileUtils.deleteFile(TEMP_PATH);

        boolean result = true;
        //解压APK文件
        FileUtils.unzip(new File(sourceApk), TEMP_PATH);
        System.out.printf("解压完成,解压路径:%s%n", new File(TEMP_PATH).getAbsolutePath());
        if (mixFlutterResource) {
            //混淆flutter资源
            tryModifyFlutterResources(TEMP_PATH);
        }

        result = toZip(TEMP_PATH, "../app.zip", extraCallback);

        if (!result) {
            throw new IOException("toZip fail");
        }
        //资源混淆——Android原生 ？？
        result = mixResource("./app.zip", extraCallback);

        if (!result) {
            throw new IOException("mixResource fail");
        }
        //重新签名？？
        result = signApk(targetApk, key, extraCallback);
        if (!result) {
            throw new IOException("signApk fail");
        }
        //安装APP
        result = installApk(targetApk, extraCallback);
        if (!result) {
            throw new IOException("installAPk fail");
        }
        cleanTempFile();
        return true;
    }

    /**
     * 清除临时文件
     */
    private void cleanTempFile(){
        //清除临时文件夹
        FileUtils.deleteFile(TEMP_PATH);
        FileUtils.deleteFile("app.apk");
        FileUtils.deleteFile("app_aligned.apk");
    }

    private void checkParams() throws IOException {
        if (sourceApk == null || sourceApk.isEmpty()) {
            throw new IOException("sourceApk path can not be empty");
        }
        if (targetApk == null || targetApk.isEmpty()) {
            throw new IOException("targetApk path can not be empty");
        }
        File file = new File(sourceApk);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("source apk " + file.getAbsolutePath() + " is not exist");
        }
    }

    private boolean toZip(String sourceFolder, String targetName, IExecuteCallback callback) throws IOException, InterruptedException {
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

    private boolean mixResource(String zipPath, IExecuteCallback callback) throws IOException, InterruptedException {
        new File(zipPath).renameTo(new File("app.apk"));
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "java -jar AndResGuard-cli-1.2.15.jar app.apk -config config.xml -out outapk"});
        if (callback != null) {
            callback.onError(process.getErrorStream());
            callback.onInfo(process.getInputStream());
        }
        int value = process.waitFor();
        return value == 0;
    }

    private boolean signApk(String apkFilePath, SignatureKey key, IExecuteCallback callback) throws InterruptedException, IOException {
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
            process = ProcessUtil.execute("/bin/sh", "-c", "./apksigner sign -verbose --ks " + key.getkFilePath() + " --v1-signing-enabled false --v2-signing-enabled true --ks-key-alias " + key.getKa() + " --ks-pass pass:" + key.getKsp() + " --key-pass pass:" + key.getKp() + " --out " + apkFilePath + " app_aligned.apk");
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

    private boolean installApk(String apkPath, IExecuteCallback callback) throws IOException, InterruptedException {
        Process process = ProcessUtil.execute("/bin/sh", "-c", String.format("adb install %s", apkPath));
        if (callback != null) {
            callback.onInfo(process.getInputStream());
            callback.onError(process.getErrorStream());
        }
        return process.waitFor() == 0;
    }

    private void tryModifyFlutterResources(String folderPath) throws IOException {
        InputStream is = new FileInputStream(folderPath + "/assets/flutter_assets/AssetManifest.json");
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
                new File(folderPath + "/assets/flutter_assets/" + f).renameTo(new File(folderPath + "/assets/flutter_assets/" + newName));//资源文件重命名
                jsonObject.getJSONArray(s).put(0, newName);//修改资源文件引用地址
            }
        });
        OutputStream os = new FileOutputStream(folderPath + "/assets/flutter_assets/AssetManifest.json");
        os.write(jsonObject.toString().getBytes());
        os.close();
    }

    @Override
    public void onError(InputStream errorStream) {
        if (extraCallback != null) {
            extraCallback.onError(errorStream);
        }
    }

    @Override
    public void onInfo(InputStream infoStream) {
        if (extraCallback != null) {
            extraCallback.onInfo(infoStream);
        }
    }

    @Override
    public void onMsg(String msg) {
        if (extraCallback != null) {
            extraCallback.onMsg(msg);
        }
    }
}
