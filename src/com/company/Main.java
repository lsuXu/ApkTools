package com.company;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {
    private static final int buffer = 2048;
    public static String apkPath = "/Users/uuu/Desktop/video-flutter/build/app/outputs/apk/release/app-release.apk";
    static int ff = 0, ss = 0;
    private static List<String> n = new ArrayList<String>();
    private static List<String> l = new ArrayList<String>();
    private static FrameWindow window;
    private static String ka, ksp, kp, kFilePath;

    public static void main(String[] args) {
        // write your code here
        System.setProperty("apple.awt.fileDialogForDirectories", "false");
        for (int i = 0; i < 10; i++) {
            n.add(i + "");
        }
        for (char i = 'a'; i < 'z'; i++) {
            n.add(i + "");
            l.add(String.valueOf(i));
        }
        Collections.shuffle(n);
        Collections.shuffle(l);

        window = new FrameWindow();
        window.appendOutput(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        try {
            InputStream is = new FileInputStream("./config.json");
            int iAvail = is.available();
            byte[] bytes = new byte[iAvail];
            is.read(bytes);
            is.close();
            JSONObject jsonObject = new JSONObject(new String(bytes));
            window.setInfo(jsonObject.getString("kf"),
                    jsonObject.getString("ka"),
                    jsonObject.getString("kp"),
                    jsonObject.getString("kp1"));
//            unzip(new File(apkPath), "");
//            rename();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static class FrameWindow extends JFrame {
        JScrollPane js;
        private JTextField tfApkPath, tfKa, tfKp, tvKp1, tfKsFile, tfPackageName, tfIconPath, tfApkName;
        private JTextArea taOut;
        private final JCheckBox jcbModify;

        FrameWindow() {
            super("混淆资源");
            setSize(600, 700);
            setLayout(new FlowLayout());
            tfApkPath = new JTextField();
            tfApkPath.setToolTipText("请选择文件");
            tfKa = new JTextField();
            tfKp = new JTextField();
            tvKp1 = new JTextField();
            taOut = new JTextArea();
            tfKsFile = new JTextField();
            tfPackageName = new JTextField();
            tfIconPath = new JTextField();
            tfApkName = new JTextField();
            tfApkPath.setPreferredSize(new Dimension(400, 30));
            tfKsFile.setPreferredSize(new Dimension(360, 30));
            tfKa.setPreferredSize(new Dimension(300, 30));
            tfKp.setPreferredSize(new Dimension(300, 30));
            tvKp1.setPreferredSize(new Dimension(300, 30));
            tfPackageName.setPreferredSize(new Dimension(360, 30));
            tfIconPath.setPreferredSize(new Dimension(320, 30));
            tfApkName.setPreferredSize(new Dimension(400, 30));
            js = new JScrollPane(taOut);
            js.setVerticalScrollBarPolicy(
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            js.setPreferredSize(new Dimension(600, 220));
            jcbModify = new JCheckBox("混淆Flutter资源文件");

            Button btnSelectapkFile = new Button("选择apk");
            add(getPanel(new JLabel("apk文件路径:(*)"), tfApkPath, btnSelectapkFile));
            Button randomPackageBtn = new Button("随机生成包名");
            add(getPanel(new JLabel("目标包名："), tfPackageName, randomPackageBtn));
            Button btnSelectIcon = new Button("请选择图标文件");
            add(getPanel(new JLabel("替换图标文件路径："), tfIconPath, btnSelectIcon));
            add(getPanel(new JLabel("APP名称："), tfApkName));
            Button btnGetKFile = new Button("选择签名文件");
            add(getPanel(new JLabel("签名文件路径：(*)"), tfKsFile, btnGetKFile));
            Button btnAction = new Button("开始混淆资源");
            btnGetKFile.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    FileDialog dialog = new FileDialog(window);
                    dialog.setVisible(true);

                    if (dialog.getFile() != null)
                        tfKsFile.setText(dialog.getDirectory() + dialog.getFile());
                }
            });
            btnAction.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    if (tfApkPath.getText().isEmpty() || tfKa.getText().isEmpty() || tfKp.getText().isEmpty() || tvKp1.getText().isEmpty() || tfKsFile.getText().isEmpty()) {
                        taOut.setText("请填写信息");
                        return;
                    }
                    try {
                        //将使用过的配置保存起来
                        ka = tfKa.getText();
                        ksp = tfKp.getText();
                        kp = tvKp1.getText();
                        kFilePath = tfKsFile.getText();

                        OutputStream os = new FileOutputStream("./config.json");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("kf", kFilePath);
                        jsonObject.put("ka", ka);
                        jsonObject.put("kp", ksp);
                        jsonObject.put("kp1", kp);
                        os.write(jsonObject.toString().getBytes());
                        os.close();
                        taOut.setText("");
                        ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "output");
                            }
                        });

                        service.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //apk源
                                    String path = tfApkPath.getText();
                                    //目标包名
                                    String targetPackageName = tfPackageName.getText().trim();
                                    String iconPath = tfIconPath.getText().trim();
                                    String apkName = tfApkName.getText().trim();
                                    //临时生成的apk文件名
                                    String savePath;
                                    if (targetPackageName.isEmpty() && iconPath.isEmpty() && apkName.isEmpty()) {
                                        savePath = path;
                                    } else {
                                        savePath = "temp.apk";
                                        ApkToolManager manager = new ApkToolManager(path, savePath);
                                        manager.setCallback(new ApkToolManager.ExecuteCallback() {
                                            @Override
                                            public void onError(InputStream errorStream) {
                                                printMessage(errorStream);
                                            }

                                            @Override
                                            public void onInfo(InputStream infoStream) {
                                                printMessage(infoStream);
                                            }
                                        });
                                        //预备解析内容，检查参数是否合规，解析清单内容
                                        boolean success = manager.prepareParsing();
                                        System.out.println("check status " + (success ? "success" : "fail"));
                                        //图标路径不为空，则替换图标
                                        if (!iconPath.isEmpty()) {
                                            success = manager.doReplaceIcon(iconPath);
                                            System.out.println("replace icon " + (success ? "success" : "fail"));
                                        }
                                        //包名不为空，则替换包名
                                        if (!targetPackageName.isEmpty()) {
                                            success = manager.doReplacePackageName(targetPackageName);
                                            System.out.println("replace packageName " + (success ? "success" : "fail"));
                                        }
                                        //apk名称不为空，则替换apk名称
                                        if (!apkName.isEmpty()) {
                                            success = manager.doChangeApkName(apkName);
                                            System.out.println("replace apkName " + (success ? "success" : "fail"));
                                        }
                                        //处理完成后，释放资源，将在输出目录生成apk文件
                                        success = manager.close();
                                        System.out.println("save " + (success ? "success" : "fail"));
                                    }
                                    unzip(new File(savePath), "");
                                    //删除临时生成的apk包

                                    //./app/
//                                   Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "cd ./app/ && find . -type f -exec bash -c 'echo -e -n \"\\x00\" >> {}' \\;"});
                                    //find . -type f -exec bash -c 'echo -e -n "\x00" >> {}' \;

                                    FileUtils.deleteFile(savePath);
                                    rename();
                                } catch (Exception exception) {
                                    window.appendOutput(exception.getLocalizedMessage());
                                }
                            }
                        });

                    } catch (Exception exception) {
                        window.appendOutput(exception.getLocalizedMessage());
                    }
                }
            });
            btnSelectapkFile.addActionListener(new AbstractAction() {
                @Override
                //选择文件按钮事件，返回文件名
                public void actionPerformed(ActionEvent e) {
                    FileDialog dialog = new FileDialog(window);
                    //调用该方法，会打开文件管理器，阻塞线程
                    dialog.setVisible(true);
                    if (dialog.getFile() != null)
                        tfApkPath.setText(dialog.getDirectory() + dialog.getFile());
                }
            });
            randomPackageBtn.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tfPackageName.setText(CommonUtils.generateRandomPackage());
                }
            });

            btnSelectIcon.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    FileDialog dialog = new FileDialog(window);
                    //调用该方法，会打开文件管理器，阻塞线程
                    dialog.setVisible(true);
                    if (dialog.getFile() != null)
                        tfIconPath.setText(dialog.getDirectory() + dialog.getFile());
                }
            });

            add(getPanel(new JLabel("签名文件别名key-alias:(*)"), tfKa));
            add(getPanel(new JLabel("签名文件密码ks-pass:(*)"), tfKp));
            add(getPanel(new JLabel("签名文件密码key-pass:(*)"), tvKp1));
            add(getPanel(jcbModify, btnAction));

            JPanel panel = new JPanel();
            panel.add(js);
            add(panel);
            setVisible(true);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        }

        public void setInfo(String kf, String ka, String kp, String kp1) {
            tfKa.setText(ka);
            tfKp.setText(kp);
            tfKsFile.setText(kf);
            tvKp1.setText(kp1);
        }

        public void appendOutput(String text) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (text != null) {
                        taOut.append(text + "\n");
                    }

                }
            });
        }

        JPanel getPanel(Component... components) {
            JPanel jp = new JPanel();
            for (Component component : components) {
                jp.add(component);
            }
            return jp;
        }


        //生成一个随机的文件名
        public String genFileName() {
            String s = l.get(ff) + n.get(ss++);
            if (ss >= n.size()) {
                ss = 0;
                ff++;
            }
            if (ff >= l.size())
                ff = 0;
            return s;
        }

        private void compress(File sourceFile, ZipOutputStream zos, String name,
                              boolean KeepDirStructure) throws Exception {
            byte[] buf = new byte[buffer];
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

        private void printMessage(final InputStream input) {
            new Thread(new Runnable() {
                public void run() {
                    Reader reader = new InputStreamReader(input);
                    BufferedReader bf = new BufferedReader(reader);
                    String line = null;
                    try {
                        while ((line = bf.readLine()) != null) {
                            window.appendOutput(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        public void toZip(String sourceFile)
                throws Exception {
            //将文件夹重新压缩为zip文件
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "cd " + sourceFile + " && zip -q -r ../app.zip ./*",});
            printMessage(process.getErrorStream());
            printMessage(process.getInputStream());
            int value = process.waitFor();
            System.out.println(value);
            if (value != 0)
                return;
            process.destroy();
            //FIXME 资源混淆——Android原生 ？？
            new File("./app.zip").renameTo(new File("./app.apk"));
            process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "java -jar AndResGuard-cli-1.2.15.jar app.apk -config config.xml -out outapk"});
            printMessage(process.getErrorStream());
            printMessage(process.getInputStream());
            value = process.waitFor();
            System.out.println(value);

            if (value != 0)
                return;

            //FIXME 重新签名？？
            new File("./app.apk").delete();
            process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "./zipalign -f -v 4 ./outapk/app_unsigned.apk app_aligned.apk && ./apksigner sign -verbose --ks " + kFilePath + " --v1-signing-enabled false --v2-signing-enabled true --ks-key-alias " + ka + " --ks-pass pass:" + ksp + " --key-pass pass:" + kp + "  --out ./app_aligned_signed.apk ./app_aligned.apk"});
            printMessage(process.getInputStream());
            printMessage(process.getErrorStream());
            value = process.waitFor();
            System.out.println(value);
            if (value != 0)
                return;

            //安装APK
            new File("./app_aligned.apk").delete();
            deleteDirectory(new File("./app/"));
            process = Runtime.getRuntime().exec("adb install ./app_aligned_signed.apk");
            printMessage(process.getInputStream());
            printMessage(process.getErrorStream());
            value = process.waitFor();
            window.appendOutput("资源混淆已完成请查看" + new File("./app_aligned_signed.apk").getAbsolutePath());
//        ZipOutputStream zos = null ;
//        try {
//            zos = new ZipOutputStream(out);
//            compress(sourceFile,zos,sourceFile.getName(),KeepDirStructure);
//        } catch (Exception e) {
//            throw new RuntimeException("zip error from ZipUtils",e);
//        }finally{
//            if(zos != null){
//                try {
//                    zos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        }

        public void rename() throws Exception {
            new File("./app/META-INF/MANIFEST.MF").delete();
            //签名相关
            new File("./app/META-INF/CERT.SF").delete();
            new File("./app/META-INF/CERT.RSA").delete();
            if (jcbModify.isSelected())
                tryModifyFlutterResources();
            //到这里，flutter_assets目录下的文件已经全部被重命名，且修改了AssetManifest的引用地址
            toZip("./app");
        }

        private void tryModifyFlutterResources() throws IOException {
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
                    String nLastName = genFileName();//随机的文件名
                    String newName = f.replaceAll(name, nLastName);
                    new File("./app/assets/flutter_assets/" + f).renameTo(new File("./app/assets/flutter_assets/" + newName));//资源文件重命名
                    jsonObject.getJSONArray(s).put(0, newName);//修改资源文件引用地址
                }
            });
            OutputStream os = new FileOutputStream("./app/assets/flutter_assets/AssetManifest.json");
            os.write(jsonObject.toString().getBytes());
            os.close();
        }

        public void deleteDirectory(File file) throws Exception {

            if (file.isFile()) {
                file.delete();//清理文件
            } else {
                File list[] = file.listFiles();
                if (list != null) {
                    for (File f : list) {
                        deleteDirectory(f);
                    }
                    file.delete();//清理目录
                }
            }
        }


        public void unzip(File zipFile, String descDir) throws Exception {
            try {

                deleteDirectory(new File("./app/"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            descDir = "./app/";
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
                String outPath = (descDir + zipEntryName).replaceAll("\\*", "/");
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));
                if (!file.exists()) {
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                //输出文件路径信息
                window.appendOutput(outPath);
                OutputStream out = new FileOutputStream(outPath);
                byte[] buf1 = new byte[1024];
                int len;
                while ((len = in.read(buf1)) > 0) {
                    out.write(buf1, 0, len);
                }
                in.close();
                out.close();
            }
            window.appendOutput("******************解压完毕********************");
        }
    }

}
