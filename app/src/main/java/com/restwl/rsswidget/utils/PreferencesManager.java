package com.restwl.rsswidget.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.restwl.rsswidget.widgedprovider.RssWidgetProvider;

public class PreferencesManager {

    public static final String PREFERENCES_KEY = "rss_widget_pref_key";
    public static final String NEWS_KEY = "news_key";
    public static final String PREF_URL_KEY = "url_key";
    public static final String NEWS_ACTUAL_PERIOD = "NEWS_ACTUAL_PERIOD";
    private static final String DEFAULT_URL = null;

    // Suppress default constructor for noninstantiability
    private PreferencesManager() {
        throw new AssertionError();
    }

    public static String getRssResourceUrl(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return preferences.getString(PREF_URL_KEY, DEFAULT_URL);
    }

    public static void setRssResourceUrl(Context context, String urlString) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putString(PREF_URL_KEY, urlString).apply();
    }

    public static void setIndexForWidget(Context context, int appWidgetId, int index) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putInt(NEWS_KEY + appWidgetId, index).apply();
    }

    public static int getNewsIndex(Context context, int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return preferences.getInt(NEWS_KEY + appWidgetId, 0);
    }

    public static int resetNewsIndex(Context context, int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        int index = 0;
        preferences.edit().putInt(NEWS_KEY + appWidgetId, index).apply();
        return index;
    }

    public static void resetNewsIndexForAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = ((AppWidgetManager) context.getSystemService(Context.APPWIDGET_SERVICE));
        ComponentName componentName = new ComponentName(context, RssWidgetProvider.class);
        int[] appWidgetId = appWidgetManager.getAppWidgetIds(componentName);
        for (int id : appWidgetId) {
            setIndexForWidget(context, id, 0);
        }
    }

    public static void setIndexForAllWidgets(Context context, int[] appWidgetIds, int index) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        for (int appWidgetId : appWidgetIds) {
            editor.putInt(NEWS_KEY + appWidgetId, index);
        }
        editor.apply();
    }

    public static int getNewsActualPeriod(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return preferences.getInt(NEWS_ACTUAL_PERIOD, 1);
    }

    public static void setNewsActualPeriod(Context context, int period) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preferences.edit().putInt(NEWS_ACTUAL_PERIOD, period).apply();
    }
}
