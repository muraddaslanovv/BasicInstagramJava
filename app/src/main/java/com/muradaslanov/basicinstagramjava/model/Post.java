package com.muradaslanov.basicinstagramjava.model;

public class Post {

    public String email;
    public String caption;
    public String downloadUrl;

    public Post(String email, String caption, String downloadUrl) {
        this.email = email;
        this.caption = caption;
        this.downloadUrl = downloadUrl;
    }
}
