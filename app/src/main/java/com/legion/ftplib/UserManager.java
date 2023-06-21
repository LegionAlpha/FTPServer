package com.legion.ftplib;

import java.util.ArrayList;

public class UserManager {
    private ArrayList<User> users = new ArrayList();

    public void addUser(String userName, String password) {
        User currentUser = new User();
        currentUser.setUserName(userName);
        currentUser.setPassWord(password);
        users.add(currentUser);
    }

    public boolean authenticate(String userName, String passWord) {
        boolean result = false;

        for (User currentUser : users) {
            if ((currentUser.getUserName().equals(userName)) && (currentUser.getPassWord().equals(passWord))) {
                result = true;
                break;
            }
        }

        return result;
    }
}
