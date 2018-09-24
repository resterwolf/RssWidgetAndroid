package rsswidget.restwl.com.rsswidget.network.parsers;

import android.support.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;

public class XmlParser {

    private static final String ITEM = "item";
    private static final String TITLE = "title";
    private static final String LINK = "link";
    private static final String DESCRIPTION = "description";
    private static final String PUB_DATE = "pubDate";
    private static final String CHANNEL = "channel";

    public static List<News> parseRssData(@Nullable InputStream inputData) throws XmlPullParserException, IOException, ParseException {
        if (inputData == null) return null;
        List<News> newsList = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();

        parser.setInput(inputData, Xml.Encoding.UTF_8.name());
        int eventType = parser.getEventType();
        boolean done = false;
        String title = null;
        String description = null;
        String pubDate = null;
        String link = null;
        while (eventType != XmlPullParser.END_DOCUMENT && !done) {
            String name;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase(ITEM)) {
                        title = null;
                        description = null;
                        pubDate = null;
                        link = null;
                    } else if (name.equalsIgnoreCase(TITLE)) {
                        title = parser.nextText();
                    } else if (name.equalsIgnoreCase(LINK)) {
                        link = parser.nextText();
                    } else if (name.equalsIgnoreCase(DESCRIPTION)) {
                        description = HelperUtils.removeHtmlTags(parser.nextText());
                    } else if (name.equalsIgnoreCase(PUB_DATE)) {
                        pubDate = parser.nextText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase(ITEM)) {
                        newsList.add(new News(title, description, link, pubDate));
                    } else if (name.equalsIgnoreCase(CHANNEL)) {
                        done = true;
                    }
                    break;
            }
            eventType = parser.next();
        }
        return newsList;
    }
}
