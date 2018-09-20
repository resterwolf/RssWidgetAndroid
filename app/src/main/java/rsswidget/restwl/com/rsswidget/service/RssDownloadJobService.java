package rsswidget.restwl.com.rsswidget.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_DATASET_CHANGED;
import static rsswidget.restwl.com.rsswidget.utils.Constants.TAG;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RssDownloadJobService extends JobService {

    public static final int JOB_ID = 1100;
    private Executor executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(getApplicationContext())) {
                String urlString = PreferencesManager.extractUrl(getApplicationContext());
                if (TextUtils.isEmpty(urlString)) {
                    jobFinished(jobParameters, false);
                    return;
                }
                HttpConnector connector = new HttpConnector(urlString);
                List<RemoteNews> newsList = XmlParser.parseRssData(connector.getContentStream());
                databaseManager.deleteAllEntryFromNewsTable();
                databaseManager.insertListNews(newsList);
                RssWidgetProvider.sendActionToAllWidgets(getApplicationContext(), ACTION_DATASET_CHANGED);
                Log.d(TAG, "onStartJob: News download and inserted");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            jobFinished(jobParameters, false);
        };
        executor.execute(runnable);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
