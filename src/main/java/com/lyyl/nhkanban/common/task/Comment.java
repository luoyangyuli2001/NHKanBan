package com.lyyl.nhkanban.common.task;

import java.util.UUID;

public class Comment {

    private UUID author;
    private long timestamp;
    private CommentType type;
    private String content;

    /** Gson 反序列化用 */
    public Comment() {}

    public Comment(UUID author, long timestamp, CommentType type, String content) {
        this.author = author;
        this.timestamp = timestamp;
        this.type = type;
        this.content = content;
    }

    public UUID getAuthor() {
        return author;
    }

    public void setAuthor(UUID author) {
        this.author = author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public CommentType getType() {
        return type;
    }

    public void setType(CommentType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
