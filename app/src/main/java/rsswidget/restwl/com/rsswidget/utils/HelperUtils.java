package rsswidget.restwl.com.rsswidget.utils;

import android.text.Html;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.*;

public class HelperUtils {

    // Suppress default constructor for noninstantiability
    private HelperUtils() {
        throw new AssertionError();
    }

    public static String removeHtmlTags(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public static String convertDateToRuLocal(Date date) {
        DateFormat formatter = new SimpleDateFormat(RU_DATE_PATTERN, new Locale(LOCALE_RU));
        return formatter.format(date);
    }

    public static boolean urlIsValid(String urlString) {
        try {
            URL url = new URL(urlString);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }
}
