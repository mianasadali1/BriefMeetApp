package com.subhadip.briefmeet.bean;

public class Intro {

    int animation;
    String title, message;

    public Intro(int animation, String title, String message) {
        this.animation = animation;
        this.title = title;
        this.message = message;
    }

    public void setMessage(String message) { this.message = message; }

    public String getMessage() { return message; }

    public int getAnimation() { return animation; }

    public void setAnimation(int animation) { this.animation = animation; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
