package com.legion.ftplib;

public class User {
    private String passWord = null;
    private boolean authenticated = true;
    private String userName = null;

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassWord() {
        return passWord;
    }
}
