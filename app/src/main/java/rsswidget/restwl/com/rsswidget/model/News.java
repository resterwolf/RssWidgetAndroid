package rsswidget.restwl.com.rsswidget.model;

import android.content.ContentValues;
import android.database.Cursor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rsswidget.restwl.com.rsswidget.database.DatabaseManager;

import static rsswidget.restwl.com.rsswidget.database.DatabaseManager.DESCRIPTION;
import static rsswidget.restwl.com.rsswidget.database.DatabaseManager.LINK;
import static rsswidget.restwl.com.rsswidget.database.DatabaseManager.PUB_DATE;
import static rsswidget.restwl.com.rsswidget.database.DatabaseManager.TITLE;
import static rsswidget.restwl.com.rsswidget.database.DatabaseManager._ID;
import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.RSS_DATE_FORMAT;

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
        this.pubDate = getRssDateFormat().parse(pubDate);
    }

    public News(int id, String title, String description, String link, long pubDate) {
        this(id, title, description, link);
        this.pubDate = new Date(pubDate);
    }

    public News(int id, String title, String description, String link, String pubDate) throws ParseException {
        this(id, title, description, link);
        this.pubDate = getRssDateFormat().parse(pubDate);
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

    private DateFormat getRssDateFormat() {
        return new SimpleDateFormat(RSS_DATE_FORMAT, Locale.ENGLISH);
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

    public ContentValues forBlackListTable() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.NEWS_ID, getId());
        return contentValues;
    }

    public ContentValues forNewsTable() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.TITLE, getTitle());
        contentValues.put(DatabaseManager.DESCRIPTION, getDescription());
        contentValues.put(DatabaseManager.PUB_DATE, getPubDate().getTime());
        contentValues.put(DatabaseManager.LINK, getLink());
        return contentValues;
    }

    public static List<News> parseNewsCursor(Cursor cursor) {
        List<News> newsList = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int id = cursor.getInt(cursor.getColumnIndex(_ID));
                    String title = cursor.getString(cursor.getColumnIndex(TITLE));
                    String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                    String link = cursor.getString(cursor.getColumnIndex(LINK));
                    long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));
                    newsList.add(new News(id, title, description, link, pubDate));
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return newsList;
    }

}
