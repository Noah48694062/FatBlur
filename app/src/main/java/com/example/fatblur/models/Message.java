package com.example.fatblur.models;
public class Message {
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
}
