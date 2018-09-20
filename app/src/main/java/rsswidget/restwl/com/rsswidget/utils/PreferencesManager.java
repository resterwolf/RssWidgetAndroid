package rsswidget.restwl.com.rsswidget.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    public static final String PREFERENCES_KEY = "rss_widget_pref_key";
    public static final String NEWS_KEY = "news_key";
    public static final String PREF_URL_KEY = "url_key";

    //    private static final String DEFAULT_URL = "https://lenta.ru/rss/news";
    private static final String DEFAULT_URL = null;

    public static String extractUrl(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return preferences.getString(PREF_URL_KEY, DEFAULT_URL);
    }

    public static void putUrl(Context context, String urlString) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putString(PREF_URL_KEY, urlString).apply();
    }

    public static void putNewsIndex(Context context, int appWidgetId, int index) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putInt(NEWS_KEY + appWidgetId, index).apply();
    }

    public static int extractNewsIndex(Context context, int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return preferences.getInt(NEWS_KEY + appWidgetId, 0);
    }

    public static void resetNewsIndex(Context context, int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putInt(NEWS_KEY + appWidgetId, 0).apply();
    }
}
