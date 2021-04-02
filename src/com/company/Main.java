package com.company;

import com.company.bean.ApkKey;
import com.company.tools.CommonUtils;
import com.company.tools.FileUtils;
import com.company.tools.ThreadPool;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

public class Main {
    public static String apkPath = "/Users/jz/Desktop/workspace/video-flutter/release/2021_03_29_17_14_33_release.apk";

    private static FrameWindow window;
    private static String ka, ksp, kp, kFilePath;

    private static ApkKey key;

    public static void main(String[] args) {
        // write your code here
        System.setProperty("apple.awt.fileDialogForDirectories", "false");


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
            tfApkPath.setText(apkPath);
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
                        key = new ApkKey(kFilePath,ka,kp,ksp);

                        OutputStream os = new FileOutputStream("./config.json");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("kf", kFilePath);
                        jsonObject.put("ka", ka);
                        jsonObject.put("kp", ksp);
                        jsonObject.put("kp1", kp);
                        os.write(jsonObject.toString().getBytes());
                        os.close();
                        taOut.setText("");

                        ThreadPool.getInstance().submit(new Runnable() {
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
                                        manager.setCallback(new IExecuteCallback() {
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
                                        System.out.println("save apk file " + (success ? "success" : "fail"));
                                    }
                                    FileUtils.unzip(new File(savePath), "app");
                                    System.out.printf("解压完成,解压路径:%s%n",new File("app").getAbsolutePath());

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


        private void printMessage(final InputStream input) {
            ThreadPool.getInstance().execute(new Runnable() {
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
            });
        }

        public void toZip(String sourceFolder)
                throws Exception {

            //将文件夹重新压缩为zip文件
            ResourceMixManager.toZip(sourceFolder,"../app.zip",callback);

            //FIXME 资源混淆——Android原生 ？？
            ResourceMixManager.mixResource("./app.zip",callback);

            //FIXME 重新签名？？
            ResourceMixManager.signApk("./app.apk",key,callback);

            ResourceMixManager.installApk("./app_aligned_signed.apk",callback);

            //安装APK

            window.appendOutput("资源混淆已完成请查看" + new File("./app_aligned_signed.apk").getAbsolutePath());
        }

        public void rename() throws Exception {
            new File("./app/META-INF/MANIFEST.MF").delete();
            //签名相关
            new File("./app/META-INF/CERT.SF").delete();
            new File("./app/META-INF/CERT.RSA").delete();
            if (jcbModify.isSelected())
                ResourceMixManager.tryModifyFlutterResources();
            //到这里，flutter_assets目录下的文件已经全部被重命名，且修改了AssetManifest的引用地址
            toZip("./app");
        }

        private IExecuteCallback callback = new IExecuteCallback() {
            @Override
            public void onError(InputStream errorStream) {
                printMessage(errorStream);
            }

            @Override
            public void onInfo(InputStream infoStream) {
                printMessage(infoStream);
            }
        };


    }

}
