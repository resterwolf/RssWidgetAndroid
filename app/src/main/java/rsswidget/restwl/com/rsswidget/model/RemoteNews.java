package rsswidget.restwl.com.rsswidget.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RemoteNews extends News {

    private String pubDate;

    public RemoteNews(String title, String description, String pubDate, String link) {
        super(title, description, link);
        this.pubDate = pubDate;
    }

    public String getPubDate() {
        return pubDate;
    }

    @Override
    public Date convertDate() {
        Date date = null;
        DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        try {
            date = formatter.parse(getPubDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
