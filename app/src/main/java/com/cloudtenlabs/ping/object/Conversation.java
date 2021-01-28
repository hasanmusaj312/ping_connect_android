package com.cloudtenlabs.ping.object;

import java.io.Serializable;
import java.util.ArrayList;

public class Conversation implements Serializable {

    private int id;
    private String title;
    private String photo;
    private int creator_id;
    private ArrayList<User> participants;
    private Message last_message = null;
    private int unread_count = 0;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPhoto() {
        return photo;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setCreator_id(int creator_id) {
        this.creator_id = creator_id;
    }

    public int getCreator_id() {
        return creator_id;
    }

    public void setLast_message(Message last_message) {
        this.last_message = last_message;
    }

    public Message getLast_message() {
        return last_message;
    }

    public void setParticipants(ArrayList<User> participants) {
        this.participants = participants;
    }

    public ArrayList<User> getParticipants() {
        return participants;
    }

    public void setUnread_count(int unread_count) {
        this.unread_count = unread_count;
    }

    public int getUnread_count() {
        return unread_count;
    }

}
