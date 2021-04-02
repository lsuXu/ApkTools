package com.company.bean;

public class ApkKey {

    private String kFilePath;

    private String ka;

    private String kp;

    private String ksp;

    public ApkKey(String kFilePath, String ka, String kp, String ksp) {
        this.kFilePath = kFilePath;
        this.ka = ka;
        this.kp = kp;
        this.ksp = ksp;
    }

    public String getkFilePath() {
        return kFilePath;
    }

    public void setkFilePath(String kFilePath) {
        this.kFilePath = kFilePath;
    }

    public String getKa() {
        return ka;
    }

    public void setKa(String ka) {
        this.ka = ka;
    }

    public String getKp() {
        return kp;
    }

    public void setKp(String kp) {
        this.kp = kp;
    }

    public String getKsp() {
        return ksp;
    }

    public void setKsp(String ksp) {
        this.ksp = ksp;
    }

    @Override
    public String toString() {
        return "ApkKey{" +
                "kFilePath='" + kFilePath + '\'' +
                ", ka='" + ka + '\'' +
                ", kp='" + kp + '\'' +
                ", ksp='" + ksp + '\'' +
                '}';
    }
}
