package xyz.knaughts.models;

import xyz.knaughts.KnaughtsBot;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Represents a note.
 */
public class Note {
    private final String id;
    private String title;
    private String content;
    private String created;
    private String updated;

    public Note(String id, String title, String content, String created, String updated) {
        this.id = id;
        this.title = title;
        this.content = content;

        final SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
        final SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try {
            this.created = newFormat.format(originalFormat.parse(created));
            this.updated = newFormat.format(originalFormat.parse(updated));
        } catch (ParseException | NullPointerException e) {
            KnaughtsBot.LOG.error("Error parsing date: " + e.getMessage());
            this.created = "null";
            this.updated = "null";
        }
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public String created() {
        return this.created;
    }

    public String updated() {
        return this.updated;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
