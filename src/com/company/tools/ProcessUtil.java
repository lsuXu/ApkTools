package com.company.tools;

import java.io.File;
import java.io.IOException;

public class ProcessUtil {

    static ProcessBuilder builder ;

    static{
        builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
    }

    public static Process getProcess() throws IOException{
        return getProcess(null);
    }

    public static Process getProcess(File root) throws IOException{
        if(root != null && !root.exists()){
            return builder.directory(root).start();
        }else{
            return builder.start();
        }
    }

    public static Process execute(String... cmds) throws IOException {
        return Runtime.getRuntime().exec(cmds);
    }

    public static Process execute(File root,String... cmds) throws IOException {
        Process process = getProcess(root);
        for(String cmd:cmds) {
            process.getOutputStream().write(cmd.getBytes());
        }
        return process;
    }
}
