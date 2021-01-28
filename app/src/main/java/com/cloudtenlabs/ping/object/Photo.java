package com.cloudtenlabs.ping.object;

import java.io.Serializable;

public class Photo implements Serializable {

    private String photo;

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPhoto() {
        return photo;
    }

}
