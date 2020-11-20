package com.protal.smartalert;

public class UserClass {
    String name, email,userID;


    public UserClass() {

    }

    public UserClass(String name, String email, String userID) {
        this.name = name;
        this.email = email;
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
