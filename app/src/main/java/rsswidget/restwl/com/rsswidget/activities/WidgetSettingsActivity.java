package rsswidget.restwl.com.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.utils.Constants;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_OPEN_SETTINGS;
import static rsswidget.restwl.com.rsswidget.utils.Constants.TAG;

public class WidgetSettingsActivity extends AppCompatActivity {

    private ExecutorService executor;
    private int mAppWidgetId;

    // UI
    private EditText editTextUrl;
    private Button buttonContinue;
    private ProgressBar progressBar;

    enum ConfigurationFlag {
        Initial, Custom
    }

    private ConfigurationFlag flag = ConfigurationFlag.Initial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        initViews();
        setViews();

        executor = Executors.newSingleThreadExecutor();
        handleIndent();
        initViews();
    }

    private void initViews() {
        editTextUrl = findViewById(R.id.ev_url);
        buttonContinue = findViewById(R.id.button_continue);
        progressBar = findViewById(R.id.progressBar);

    }

    private void setViews() {
        buttonContinue.setOnClickListener(this::buttonContinueClicked);
        String urlString = PreferencesManager.extractUrl(this);
        if (!TextUtils.isEmpty(urlString)) {
            editTextUrl.setText(urlString);
        }
    }

    public void buttonContinueClicked(View button) {
        String urlString = editTextUrl.getText().toString().trim();
        if (!HelperUtils.urlStrIsValidFormat(urlString)) {
            Log.d(TAG, "WidgetSettingsActivity. Invalid url format");
            return;
        }
        downloadAndSaveData(urlString);
    }

    private void handleIndent() {
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (intentAction == null) {
            finish();
            return;
        }

        switch (intentAction) {
            case AppWidgetManager.ACTION_APPWIDGET_CONFIGURE:
                extractWidgetId(intent.getExtras());
                break;
            case ACTION_OPEN_SETTINGS:
                extractWidgetId(intent.getExtras());
                flag = ConfigurationFlag.Custom;
                break;
        }
    }

    private void extractWidgetId(Bundle extras) {
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    private void downloadAndSaveData(String urlString) {
        Runnable runnable = () -> {
            if (isFinishing()) return;
            try {
                HttpConnector connector = new HttpConnector(urlString);
                List<RemoteNews> newsList = XmlParser.parseRssData(connector.getContentStream());
                saveDateIntoDatabase(newsList);
                PreferencesManager.putUrl(WidgetSettingsActivity.this, urlString);
                if (flag == ConfigurationFlag.Initial) {
                    notifyWidgetAfterInitialConfigAndCloseCurrent();
                } else {
                    notifyWidgetAfterCustomConfigAndCloseCurrent();
                }
                Log.d(TAG, "WidgetSettingsActivity: News downloaded and saved");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                runOnUiThread(this::hideProgress);
                executor.shutdown();
            }
        };
        executor.execute(runnable);
        showProgress();
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void notifyWidgetAfterInitialConfigAndCloseCurrent() {
        RssWidgetProvider.sendActionToAllWidgets(this, Constants.ACTION_INITIAL_CONFIG);
        setResult(RESULT_OK, getSuccessResultIntent(mAppWidgetId));
        finish();
    }

    private void notifyWidgetAfterCustomConfigAndCloseCurrent() {
        RssWidgetProvider.sendActionToAllWidgets(this, Constants.ACTION_CUSTOM_CONFIG);
        finish();
    }

    private Intent getSuccessResultIntent(int appWidgetId) {
        Intent resultValue = new Intent();
        resultValue.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return resultValue;
    }

    private void saveDateIntoDatabase(List<RemoteNews> newsList) {
        try (DatabaseManager databaseManager = new DatabaseManager(getApplicationContext())) {
            databaseManager.deleteAllEntryFromNewsTable();
            databaseManager.insertListNews(newsList);
        }
    }

}
