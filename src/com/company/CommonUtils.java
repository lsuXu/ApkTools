package com.company;

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
}
