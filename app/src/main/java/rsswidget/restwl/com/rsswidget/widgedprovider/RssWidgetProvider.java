package rsswidget.restwl.com.rsswidget.widgedprovider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.activities.SettingsActivity;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.LocalNews;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.receiver.UpdateReceiver;
import rsswidget.restwl.com.rsswidget.utils.Constants;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;

import static rsswidget.restwl.com.rsswidget.utils.Constants.*;

public class RssWidgetProvider extends AppWidgetProvider {

    private static final List<LocalNews> newsList = new ArrayList<>();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: ");
        for (int id : appWidgetIds) {
            handleUpdateWidget(context, appWidgetManager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        handleReceiveWidget(context, intent);
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

    public static void handleUpdateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.rss_widget_layout);
        remoteViews.setOnClickPendingIntent(R.id.button_settings,
                getPendingIntentSetting(context, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.button_hide,
                getPendingIntentCustomAction(context, appWidgetId, ACTION_HIDE_NEWS));
        remoteViews.setOnClickPendingIntent(R.id.button_previous,
                getPendingIntentCustomAction(context, appWidgetId, ACTION_SHOW_PREVIOUS + appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.button_next,
                getPendingIntentCustomAction(context, appWidgetId, ACTION_SHOW_NEXT + appWidgetId));
        if (newsDataIsEmpty()) {
            remoteViews.setTextViewText(R.id.tv_news_title, context.getString(R.string.news_list_empty));
            hideViews(remoteViews);
            remoteViews.setOnClickPendingIntent(R.id.linearLayout_container, null);
        } else {
            showViews(remoteViews);
            int currentNewsIndex = PreferencesManager.extractNewsIndex(context, appWidgetId);
            if (!indexIsCorrect(currentNewsIndex)) {
                currentNewsIndex = 0;
            }

            LocalNews news = newsList.get(currentNewsIndex);
            String newsTitle = String.format(context.getString(R.string.news_title_placeholder), currentNewsIndex + 1, news.getTitle());
            remoteViews.setTextViewText(R.id.tv_news_title, newsTitle);
            remoteViews.setTextViewText(R.id.tv_news_description, news.getDescription());
            remoteViews.setTextViewText(R.id.tv_news_pub_date, HelperUtils.convertDateToRuLocal(news.convertDate()));
            remoteViews.setOnClickPendingIntent(R.id.linearLayout_container, getPendingIntentActionView(context, news.getLink()));
        }
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private void handleReceiveWidget(Context context, Intent intent) {
        final String intentAction = intent.getAction();
        if (intentAction == null) throw new IllegalArgumentException();

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (intentAction.equals(Constants.ACTION_INITIAL_CONFIG)) {
            extractAndSetDataFromDatabase(context);
            return;
        }

        if (intentAction.equals(Constants.ACTION_CUSTOM_CONFIG)) {
            extractAndSetDataFromDatabase(context);
            return;
        }

        if (intentAction.equals(ACTION_DATASET_CHANGED)) {
            extractAndSetDataFromDatabase(context);
            return;
        }

        if (intentAction.equals(ACTION_SHOW_PREVIOUS + appWidgetId) || intentAction.equals(ACTION_SHOW_NEXT + appWidgetId)) {
            if (newsDataIsNotEmpty()) {
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;
                int shownIndex = PreferencesManager.extractNewsIndex(context, appWidgetId);
                int displayedIndex = calculateNewIndexForWidget(appWidgetId, intentAction, shownIndex);
                PreferencesManager.putNewsIndex(context, appWidgetId, displayedIndex);
                handleUpdateWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            } else {
                extractAndSetDataFromDatabase(context);
            }
        }

        if (intentAction.equals(ACTION_HIDE_NEWS)) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;
            if (newsDataIsNotEmpty()) {
                int shownIndex = PreferencesManager.extractNewsIndex(context, appWidgetId);
                int displayedIndex = calculateNextIndexAfterRemovingNews(shownIndex);
                News news = newsList.remove(displayedIndex);
                addNewsInBlockedList(context, news);
                PreferencesManager.putNewsIndex(context, appWidgetId, displayedIndex);
            }
            sendActionToAllWidgets(context, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        }
    }

    private static boolean indexIsCorrect(int index) {
        return index >= 0 && index < newsList.size();
    }

    private void extractAndSetDataFromDatabase(Context context) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(context)) {
                List<LocalNews> allNews = databaseManager.extractAllEntryFromNews();
                List<LocalNews> blackListNews = databaseManager.extractAllEntryFromBlackList();
                allNews.removeAll(blackListNews);
                newsList.clear();
                newsList.addAll(allNews);
                sendActionToAllWidgets(context, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            }
        };
        Executors.newSingleThreadExecutor().execute(runnable);
    }

    private static PendingIntent getPendingIntentCustomAction(Context context, int[] appWidgetIds, String action) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntentCustomAction(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntentSetting(Context context, int appWidgetId) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.setAction(ACTION_OPEN_SETTINGS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static PendingIntent getPendingIntentActionView(Context context, String urlString) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static PendingIntent getSelfUpdatePendingIntent(Context context, int[] appWidgetIds) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static int calculateNextIndexAfterRemovingNews(int removingNewsIndex) {
        return removingNewsIndex >= newsList.size() - 1 ? 0 : removingNewsIndex;
    }

    public static void showViews(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.button_hide, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.button_next, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.button_previous, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_title, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_description, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_pub_date, View.VISIBLE);
    }

    public static void hideViews(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.button_hide, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.button_next, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.button_previous, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_description, View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.tv_news_pub_date, View.INVISIBLE);
    }

    public static void addNewsInBlockedList(Context context, News news) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(context)) {
                databaseManager.insertEntryInBlackList(news);
            }
        };
        Executors.newSingleThreadExecutor().execute(runnable);
    }

    public static void schedulePeriodicallyTasks(Context context) {
        startAlarmManagerTask(context);
    }

    private void stopSchedulePeriodicallyTasks(Context context) {
        stopAlarmManagerTask(context);
    }

    private static int calculateNewIndexForWidget(int appWidgetId, String intentAction, int currentIndex) {
        if (intentAction.equals(ACTION_SHOW_PREVIOUS + appWidgetId)) {
            return currentIndex == 0 ? newsList.size() - 1 : --currentIndex;
        } else if (intentAction.equals(ACTION_SHOW_NEXT + appWidgetId)) {
            return currentIndex == newsList.size() - 1 ? 0 : ++currentIndex;
        }
        return 0;
    }

    private static void startAlarmManagerTask(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UpdateReceiver.class);
        intent.setAction(ACTION_START_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        am.setRepeating(AlarmManager.RTC_WAKEUP, currentTime + 5 * 1000, 60000, pi);
    }

    private static void stopAlarmManagerTask(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UpdateReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.cancel(pi);
    }

    private static boolean newsDataIsEmpty() {
        return newsList.isEmpty();
    }

    private static boolean newsDataIsNotEmpty() {
        return !newsList.isEmpty();
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
