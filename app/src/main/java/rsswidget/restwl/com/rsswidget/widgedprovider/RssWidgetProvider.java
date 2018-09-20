package rsswidget.restwl.com.rsswidget.widgedprovider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.activities.NewsActivity;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.LocalNews;
import rsswidget.restwl.com.rsswidget.receiver.UpdateReceiver;
import rsswidget.restwl.com.rsswidget.service.RssDownloadJobService;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;

import static rsswidget.restwl.com.rsswidget.utils.Constans.NEWS_ID;
import static rsswidget.restwl.com.rsswidget.utils.Constans.PREFERENCES_KEY;
import static rsswidget.restwl.com.rsswidget.utils.Constans.TAG;

public class RssWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_PREVIOUS_NEWS_BUTTON = "ACTION_PREVIOUS_NEWS_BUTTON";
    private static final String ACTION_NEXT_NEWS_BUTTON = "ACTION_NEXT_NEWS_BUTTON";
    private static final String ACTION_UPDATE_DATA = "ACTION_UPDATE_DATA";

    private static final List<LocalNews> newsList = new ArrayList<>();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("MyLog", "onUpdate: ");
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        int currentNewsIndex = preferences.getInt(NEWS_ID, 0);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.rss_widget_layout);

        if (newsDataIsEmpty()) return;
        if (currentNewsIndex > newsList.size() - 1) {
            saveIndexInPreferences(preferences, 0);
        }
        LocalNews news = newsList.get(currentNewsIndex);
        setDataToViews(context, remoteViews, appWidgetIds, news);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        final String intentAction = intent.getAction();
        if (intentAction == null) throw new IllegalArgumentException();

        if (intentAction.equals(ACTION_UPDATE_DATA)) {
            Runnable runnable = () -> {
                try (DatabaseManager databaseManager = new DatabaseManager(context)) {
                    newsList.clear();
                    newsList.addAll(databaseManager.getAllNews());
                    updateSelf(context);
                }
            };
            Executors.newSingleThreadExecutor().execute(runnable);
            return;
        }

        if (!intentAction.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) && newsDataIsNotEmpty()) {
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);

            int currentNewsIndex = preferences.getInt(NEWS_ID, 0);
            if (intentAction.equals(ACTION_PREVIOUS_NEWS_BUTTON)) {
                if (currentNewsIndex == 0) {
                    currentNewsIndex = newsList.size() - 1;
                } else {
                    currentNewsIndex--;
                }
            }
            if (intentAction.equals(ACTION_NEXT_NEWS_BUTTON)) {
                if (currentNewsIndex == newsList.size() - 1) {
                    currentNewsIndex = 0;
                } else {
                    currentNewsIndex++;
                }
            }
            saveIndexInPreferences(preferences, currentNewsIndex);
            onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled: ");
        super.onEnabled(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startJobSchedulerTask(context);
        } else {
            startAlarmManagerTask(context);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stopJobSchedulerTask(context);
        } else {
            stopAlarmManagerTask(context);
        }
    }

    private static void setDataToViews(Context context, RemoteViews remoteViews, int[] appWidgetIds, LocalNews news) {
        String newsTitle = String.format(context.getString(R.string.news_title_placeholder), news.getId(), news.getTitle());
        remoteViews.setTextViewText(R.id.tv_news_title, newsTitle);
        remoteViews.setTextViewText(R.id.tv_news_description, news.getDescription());
        remoteViews.setTextViewText(R.id.tv_news_pub_date, HelperUtils.convertDateToRuLocal(news.convertDate()));
        remoteViews.setOnClickPendingIntent(R.id.linearLayout_container, getNewsActivityPendingIntent(context));
        PendingIntent pendingIntentPreviousButton = getCustomActionPendingIntent(context, appWidgetIds, ACTION_PREVIOUS_NEWS_BUTTON);
        PendingIntent pendingIntentNextButton = getCustomActionPendingIntent(context, appWidgetIds, ACTION_NEXT_NEWS_BUTTON);
        remoteViews.setOnClickPendingIntent(R.id.button_previous, pendingIntentPreviousButton);
        remoteViews.setOnClickPendingIntent(R.id.button_next, pendingIntentNextButton);
    }

    private boolean newsDataIsEmpty() {
        return newsList.isEmpty();
    }

    private boolean newsDataIsNotEmpty() {
        return !newsList.isEmpty();
    }

    public static void saveIndexInPreferences(SharedPreferences preferences, int index) {
        preferences.edit().putInt(NEWS_ID, index).apply();
    }

    private static PendingIntent getSelfUpdatePendingIntent(Context context, int[] appWidgetIds) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getCustomActionPendingIntent(Context context, int[] appWidgetIds, String action) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getNewsActivityPendingIntent(Context context) {
        Intent intent = new Intent(context, NewsActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static void updateSelf(Context context) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, RssWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    public static void updateSelfData(Context context) {
        Intent intent = new Intent(context, RssWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_DATA);
        int[] appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, RssWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void startJobSchedulerTask(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context, RssDownloadJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(RssDownloadJobService.JOB_ID, componentName)
                .setPeriodic(TimeUnit.SECONDS.toMillis(60))
                .build();
        jobScheduler.schedule(jobInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void stopJobSchedulerTask(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(RssDownloadJobService.JOB_ID);
    }

    private static void startAlarmManagerTask(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UpdateReceiver.class);
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

}
