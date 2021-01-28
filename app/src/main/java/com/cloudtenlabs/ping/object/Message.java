package com.cloudtenlabs.ping.object;

import java.io.Serializable;

public class Message implements Serializable {

    private int id;
    private int conversation_id;
    private String message_type;
    private String message;
    private String attachment_thumb;
    private String attachment;
    private String create_date;
    private User sender;
    private int is_read;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setConversation_id(int conversation_id) {
        this.conversation_id = conversation_id;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment_thumb(String attachment_thumb) {
        this.attachment_thumb = attachment_thumb;
    }

    public String getAttachment_thumb() {
        return attachment_thumb;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public String getCreate_date() {
        return create_date;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getSender() {
        return sender;
    }

    public int getIs_read() {
        return is_read;
    }

    public void setIs_read(int is_read) {
        this.is_read = is_read;
    }
}
