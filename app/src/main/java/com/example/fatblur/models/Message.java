package com.example.fatblur.models;
public class Message {
    public String messageId;
    public String senderId;
    public String content;
    public long timestamp;

    public Message() {

    }

    public Message(String senderId, String content, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setMessageId(String messageId){
        this.messageId = messageId;
    }


    public void setSenderId(String senderId){
        this.senderId = senderId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
