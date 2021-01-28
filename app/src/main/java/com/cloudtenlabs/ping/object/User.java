package com.cloudtenlabs.ping.object;

import android.content.Intent;

import com.quickblox.users.model.QBUser;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {

    private int id;
    private String phone;
    private String email;
    private String photo = null;

    private String first_name = null;
    private String birthday = null;
    private int height = -1;

    private String city = null;
    private String gender = null;
    private String about = null;

    private String preferred_gender = null;

    private int preferred_age_min = -1;
    private int preferred_age_max = -1;

    private String vibration = "0";
    private String push_notification = "0";

    private double latitude = 0;
    private double longitude = 0;

    private String player_id = null;
    private String is_favorited = "0";
    private String is_blocked = "0";
    private String is_blocked_by = "0";
    private String is_test = "0";

    private int preferred_radius = 0;

    private QBUser qb_user = null;
    private Integer qb_id = null;

    private ArrayList<Photo> photos = new ArrayList<>();

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Integer getQb_id() {
        return qb_id;
    }

    public void setQb_id(Integer qb_id) {
        this.qb_id = qb_id;
    }

    public QBUser getQb_user() {
        return qb_user;
    }

    public void setQb_user(QBUser qb_user) {
        this.qb_user = qb_user;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPhoto() {
        return photo;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getAbout() {
        return about;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setIs_favorited(String is_favorited) {
        this.is_favorited = is_favorited;
    }

    public String getIs_favorited() {
        return is_favorited;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setPreferred_age_max(int preferred_age_max) {
        this.preferred_age_max = preferred_age_max;
    }

    public int getPreferred_age_max() {
        return preferred_age_max;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPlayer_id(String player_id) {
        this.player_id = player_id;
    }

    public String getPlayer_id() {
        return player_id;
    }

    public void setPreferred_age_min(int preferred_age_min) {
        this.preferred_age_min = preferred_age_min;
    }

    public int getPreferred_age_min() {
        return preferred_age_min;
    }

    public void setPreferred_gender(String preferred_gender) {
        this.preferred_gender = preferred_gender;
    }

    public String getPreferred_gender() {
        return preferred_gender;
    }

    public void setVibration(String vibration) {
        this.vibration = vibration;
    }

    public String getVibration() {
        return vibration;
    }

    public void setPush_notification(String push_notification) {
        this.push_notification = push_notification;
    }

    public String getPush_notification() {
        return push_notification;
    }

    public void setPhotos(ArrayList<Photo> photos) {
        this.photos = photos;
    }

    public ArrayList<Photo> getPhotos() {
        return photos;
    }

    public void setPreferred_radius(int preferred_radius) {
        this.preferred_radius = preferred_radius;
    }

    public int getPreferred_radius() {
        return preferred_radius;
    }

    public String getIs_blocked() {
        return is_blocked;
    }

    public void setIs_blocked(String is_blocked) {
        this.is_blocked = is_blocked;
    }

    public String getIs_blocked_by() {
        return is_blocked_by;
    }

    public void setIs_blocked_by(String is_blocked_by) {
        this.is_blocked_by = is_blocked_by;
    }

    public String getIs_test() {
        return is_test;
    }

    public void setIs_test(String is_test) {
        this.is_test = is_test;
    }
}


