package com.restwl.rsswidget.widgedprovider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.restwl.rsswidget.R;
import com.restwl.rsswidget.activities.SettingsActivity;
import com.restwl.rsswidget.contentprovider.WidgetContentProvider;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.receiver.WidgetTasksReceiver;
import com.restwl.rsswidget.utils.HelperUtils;
import com.restwl.rsswidget.utils.IndexCalculator;
import com.restwl.rsswidget.utils.PreferencesManager;

import static com.restwl.rsswidget.utils.WidgetConstants.*;

public class RssWidgetProvider extends AppWidgetProvider {

    private static final long START_TIME_DELAY = TimeUnit.SECONDS.toMillis(5);
    private static final long REPEATING_TIME = TimeUnit.SECONDS.toMillis(60);

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: ");
        for (int id : appWidgetIds) {
            updateWidgetView(context, appWidgetManager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        handleReceiveWidgetEvent(context, intent);
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled: ");
        super.onEnabled(context);
        schedulePeriodicallyTasks(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled: ");
        super.onDisabled(context);
        stopSchedulePeriodicallyTasks(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted: ");
        super.onDeleted(context, appWidgetIds);
        for (int id : appWidgetIds) {
            PreferencesManager.resetNewsIndex(context, id);
        }
    }

    public static void updateWidgetView(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.rss_widget_layout);
        remoteViews.setOnClickPendingIntent(R.id.button_settings,
                getPendingIntentSettingsActivity(context, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.button_hide,
                getPendingIntent(context, appWidgetId, ACTION_HIDE_NEWS));
        remoteViews.setOnClickPendingIntent(R.id.button_previous,
                getPendingIntent(context, appWidgetId, ACTION_SHOW_PREVIOUS + appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.button_next,
                getPendingIntent(context, appWidgetId, ACTION_SHOW_NEXT + appWidgetId));

        int index = PreferencesManager.extractNewsIndex(context, appWidgetId);
        Cursor cursor = WidgetContentProvider.getAllFilteredNewsFromDatabase(context);
        List<News> newsList = News.parseNewsCursor(cursor);

        if (!newsList.isEmpty()) {
            setViewsVisible(remoteViews);
            News news = newsList.get(index);
            String newsTitle = String.format(context.getString(R.string.placeholder_string_tab_number_text), index + 1, news.getTitle());
            remoteViews.setTextViewText(R.id.tv_news_title, newsTitle);
            remoteViews.setTextViewText(R.id.tv_news_description, news.getDescription());
            remoteViews.setTextViewText(R.id.tv_news_pub_date, HelperUtils.convertDateToRuLocal(news.getPubDate()));
            remoteViews.setOnClickPendingIntent(R.id.linearLayout_container, getPendingIntentActionView(context, news.getLink()));
        } else {
            setViewsInvisible(remoteViews);
            remoteViews.setTextViewText(R.id.tv_news_title, context.getString(R.string.widget_news_list_empty));
            remoteViews.setOnClickPendingIntent(R.id.linearLayout_container, null);
        }

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private void handleReceiveWidgetEvent(Context context, Intent intent) {
        final String intentAction = intent.getAction();
        if (intentAction == null) throw new IllegalArgumentException();

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        Cursor cursor = WidgetContentProvider.getAllFilteredNewsFromDatabase(context);
        List<News> newsList = News.parseNewsCursor(cursor);

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (intentAction.equals(ACTION_SHOW_PREVIOUS + appWidgetId) || intentAction.equals(ACTION_SHOW_NEXT + appWidgetId)) {
                if (!newsList.isEmpty()) {
                    handleNavigationAction(context, appWidgetId, intentAction, newsList);
                }
            }
            if (intentAction.equals(ACTION_HIDE_NEWS)) {
                if (!newsList.isEmpty())
                    handleHideAction(context, appWidgetId, newsList);
            }
        }
    }

    private void handleHideAction(Context context, int appWidgetId, List<News> newsList) {
        int currentIndex = PreferencesManager.extractNewsIndex(context, appWidgetId);
        int newIndex = IndexCalculator.nextIndexAfterRemote(currentIndex, newsList.size() - 1);
        News news = newsList.remove(currentIndex);
        WidgetContentProvider.insertEntryInBlackListTable(context, news.forBlackListTable());
        ComponentName componentName = new ComponentName(context, RssWidgetProvider.class);
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName);
        PreferencesManager.setIndexForAllWidgets(context, appWidgetIds, newIndex);
        sendActionToAllWidgets(context, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    }

    private void handleNavigationAction(Context context, int appWidgetId, String action, List<News> newsList) {
        int oldIndex = PreferencesManager.extractNewsIndex(context, appWidgetId);
        int newIndex = IndexCalculator.getNewNavigationIndex(action, oldIndex, newsList.size() - 1);
        PreferencesManager.setIndexForWidget(context, appWidgetId, newIndex);
        updateWidgetView(context, AppWidgetManager.getInstance(context), appWidgetId);
    }

    private static PendingIntent getPendingIntent(Context context, int[] appWidgetIds, String action) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntent(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntentSettingsActivity(Context context, int appWidgetId) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.setAction(ACTION_OPEN_SETTINGS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntentActionView(Context context, String urlString) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static void setViewsVisible(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.button_hide, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.button_next, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.button_previous, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_title, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_description, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_pub_date, View.VISIBLE);
    }

    public static void setViewsInvisible(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.button_hide, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.button_next, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.button_previous, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_description, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_pub_date, View.INVISIBLE);
    }

    public static void schedulePeriodicallyTasks(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetTasksReceiver.class);
        intent.setAction(ACTION_START_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        am.setRepeating(AlarmManager.RTC_WAKEUP, currentTime + START_TIME_DELAY, REPEATING_TIME, pi);
    }

    private static void stopSchedulePeriodicallyTasks(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetTasksReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.cancel(pi);
    }

    public static void sendActionToAllWidgets(Context context, String intentAction) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(intentAction);
        int[] appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, RssWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    public static void sendActionToSpecifyWidget(Context context, String intentAction, int appWidgetId) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(intentAction);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        context.sendBroadcast(intent);
    }

}
