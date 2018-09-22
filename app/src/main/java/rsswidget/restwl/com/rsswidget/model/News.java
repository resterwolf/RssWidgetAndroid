package rsswidget.restwl.com.rsswidget.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class News {

    private int id;
    private String title;
    private String description;
    private String link;
    private Date pubDate;

    private News(int id, String title, String description, String link) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.link = link;
    }

    public News(int id, String title, String description, String link, Date pubDate) {
        this(id, title, description, link);
        this.pubDate = pubDate;
    }

    public News(String title, String description, String link, String pubDate) throws ParseException {
        this(0, title, description, link);
        DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        this.pubDate = formatter.parse(pubDate);
    }

    public News(int id, String title, String description, String link, long pubDate) {
        this(id, title, description, link);
        this.pubDate = new Date(pubDate);
    }

    public News(int id, String title, String description, String link, String pubDate) throws ParseException {
        this(id, title, description, link);
        DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        this.pubDate = formatter.parse(pubDate);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public Date getPubDate() {
        return pubDate;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof News)) return false;
        News otherMyClass = (News) other;
        if (title.equals(otherMyClass.title) && description.equals(otherMyClass.description) &&
                link.equals(otherMyClass.link)) {
            return true;
        } else {
            return false;
        }
    }
}
