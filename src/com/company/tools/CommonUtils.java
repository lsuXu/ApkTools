package com.company.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CommonUtils {

    /**
     * 随机生成包名
     * @return  包名，格式：(*.*.*)
     */
    public static String generateRandomPackage(){
        String randSource = "abcdefghijklmnopqrstuvwxyz";
        int index;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < 3;){
            index = random.nextInt(26);
            sb.append(randSource.charAt(index));
            if(index %5 ==0){
                sb.append('.');
                i++;
            }
        }
        return sb.substring(0,sb.length()-1);
    }

    static int ff = 0, ss = 0;

    private static List<String> n = new ArrayList<String>();
    private static List<String> l = new ArrayList<String>();

    static {
        for (int i = 0; i < 10; i++) {
            n.add(i + "");
        }
        for (char i = 'a'; i < 'z'; i++) {
            n.add(i + "");
            l.add(String.valueOf(i));
        }
        Collections.shuffle(n);
        Collections.shuffle(l);
    }

    //生成一个随机的文件名
    public static String genFileName() {
        String s = l.get(ff) + n.get(ss++);
        if (ss >= n.size()) {
            ss = 0;
            ff++;
        }
        if (ff >= l.size())
            ff = 0;
        return s;
    }

}
