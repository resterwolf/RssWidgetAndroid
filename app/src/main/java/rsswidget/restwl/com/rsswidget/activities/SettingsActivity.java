package rsswidget.restwl.com.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.adapters.RVBlackListAdapter;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.LocalNews;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.utils.Constants;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_OPEN_SETTINGS;
import static rsswidget.restwl.com.rsswidget.utils.Constants.TAG;

public class SettingsActivity extends AppCompatActivity implements RVBlackListAdapter.CellClickListener {

    private ExecutorService executor;
    private int mAppWidgetId;
    private List<LocalNews> newsList = new ArrayList<>();
    private ConfigurationFlag flag = ConfigurationFlag.Initial;

    // UI
    private TextInputEditText editTextUrl;
    private Button buttonContinue, buttonClearBlackList;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initActionBar();
        initViews();

        executor = Executors.newSingleThreadExecutor();
        handleIndent();
        initRecyclerView();
        extractFromDatabaseBlockedNews();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_activity_title);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Override
    public void onHideButtonClick(View view, LocalNews news, int index) {
        removeNewsFromBlackList(news);
    }

    @Override
    public void onItemClick(View viewGroup, LocalNews news, int index) {
        openNewsInActionView(news);
    }

    private void removeNewsFromBlackList(LocalNews localNews) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.removeEntryFromBlackList(localNews);
                runOnUiThread(() -> {
                    this.newsList.remove(localNews);
                    recyclerView.getAdapter().notifyDataSetChanged();
                });
            }
        };
        executor.execute(runnable);
    }

    private void openNewsInActionView(News news) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLink()));
        startActivity(intent);
    }

    private void extractFromDatabaseBlockedNews() {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                List<LocalNews> localNewsList = databaseManager.extractAllEntryFromBlackList();
                runOnUiThread(() -> {
                    this.newsList.clear();
                    this.newsList.addAll(localNewsList);
                    if (recyclerView.getAdapter() == null) {
                        RVBlackListAdapter adapter = new RVBlackListAdapter(this, newsList, this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        };
        executor.execute(runnable);
    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
    }

    private void initViews() {
        editTextUrl = findViewById(R.id.ev_url);
        buttonContinue = findViewById(R.id.button_continue);
        progressBar = findViewById(R.id.progressBar);
        buttonClearBlackList = findViewById(R.id.button_clear_black_list);
        recyclerView = findViewById(R.id.recycler_view_blocked_news_container);
        buttonContinue.setOnClickListener(this::buttonContinueOnClick);
        buttonClearBlackList.setOnClickListener(this::buttonClearBlackListOnClick);
        String urlString = PreferencesManager.extractUrl(this);
        if (!TextUtils.isEmpty(urlString)) {
            editTextUrl.setText(urlString);
        }
    }

    private void buttonClearBlackListOnClick(View view) {
        if (newsList.isEmpty()) return;
        clearAsyncAllEntryFromBlackListDb();
    }

    public void clearAsyncAllEntryFromBlackListDb() {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.clearAllEntryFromBlackList();
                newsList.clear();
                runOnUiThread(() -> {
                    recyclerView.getAdapter().notifyDataSetChanged();
                });
            }
        };
        executor.execute(runnable);
    }

    public void buttonContinueOnClick(View button) {
        String urlString = editTextUrl.getText().toString().trim();
        if (!HelperUtils.urlStrIsValidFormat(urlString)) {
            editTextUrl.setError("Invalid url");
            Log.d(TAG, "SettingsActivity. Invalid url format");
            return;
        }
        downloadAndSaveData(urlString);
    }

    public void enableViewsState() {
        editTextUrl.setEnabled(true);
        buttonContinue.setEnabled(true);
    }

    public void disableViewsState() {
        editTextUrl.setEnabled(false);
        buttonContinue.setEnabled(false);
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
                PreferencesManager.putUrl(SettingsActivity.this, urlString);
                if (flag == ConfigurationFlag.Initial) {
                    notifyWidgetAfterInitialConfigAndCloseCurrent();
                } else {
                    notifyWidgetAfterCustomConfigAndCloseCurrent();
                }
                Log.d(TAG, "SettingsActivity: News downloaded and saved");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                runOnUiThread(() -> {
                    hideProgress();
                    enableViewsState();
                });
//                executor.shutdown();
            }
        };
        disableViewsState();
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
            databaseManager.deleteAllEntryFromNews();
            databaseManager.insertEntriesInNews(newsList);
        }
    }

    enum ConfigurationFlag {
        Initial, Custom
    }

}
