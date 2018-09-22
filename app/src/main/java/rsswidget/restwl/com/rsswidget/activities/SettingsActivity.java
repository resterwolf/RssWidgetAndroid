package rsswidget.restwl.com.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.adapters.RVBlackListAdapter;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.loaders.BlackListLoader;
import rsswidget.restwl.com.rsswidget.loaders.NetLoader;
import rsswidget.restwl.com.rsswidget.model.LoaderData;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.utils.Constants;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_DATASET_CHANGED;
import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_OPEN_SETTINGS;
import static rsswidget.restwl.com.rsswidget.utils.Constants.EXTRA_URL;
import static rsswidget.restwl.com.rsswidget.utils.Constants.TAG;

public class SettingsActivity extends AppCompatActivity implements RVBlackListAdapter.CellClickListener,
        LoaderManager.LoaderCallbacks<LoaderData> {

    private ExecutorService executor;
    private int mAppWidgetId;
    private List<News> newsList = new ArrayList<>();
    private ConfigurationFlag flag = ConfigurationFlag.Initial;

    // UI
    private TextInputEditText editTextUrl;
    private Button buttonCommit, buttonClearBlackList;
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
        executeBlackListLoader();
    }

    private void executeBlackListLoader() {
        Loader loader = getSupportLoaderManager().getLoader(BlackListLoader.LOADER_ID);
        if (loader == null) {
            getSupportLoaderManager().initLoader(BlackListLoader.LOADER_ID, null, this);
        } else {
            getSupportLoaderManager().restartLoader(BlackListLoader.LOADER_ID, null, this);
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onHideButtonClick(View view, News news, int index) {
        removeNewsFromBlackList(news);
    }

    @Override
    public void onItemClick(View viewGroup, News news, int index) {
        openNewsInActionView(news);
    }

    private void onButtonCommitClick(View button) {
        String urlString = editTextUrl.getText().toString().trim();
        if (!HelperUtils.urlStrIsValidFormat(urlString)) {
            editTextUrl.setError("Invalid url");
            Log.d(TAG, "SettingsActivity. Invalid url format");
            return;
        }
        executeNetLoader(urlString);
    }

    private void onButtonClearBlackListClick(View view) {
        if (newsList.isEmpty()) return;
        clearAsyncAllEntryFromBlackListDb();
    }

    private void removeNewsFromBlackList(News news) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.removeEntryFromBlackList(news);
                runOnUiThread(() -> {
                    this.newsList.remove(news);
                    recyclerView.getAdapter().notifyDataSetChanged();
                    RssWidgetProvider.sendActionToAllWidgets(this, ACTION_DATASET_CHANGED);
                });
            }
        };
        executor.execute(runnable);
    }

    private void openNewsInActionView(News news) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLink()));
        startActivity(intent);
    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
    }

    private void initViews() {
        editTextUrl = findViewById(R.id.ev_url);
        buttonCommit = findViewById(R.id.button_commit);
        progressBar = findViewById(R.id.progressBar);
        buttonClearBlackList = findViewById(R.id.button_clear_black_list);
        recyclerView = findViewById(R.id.recycler_view_blocked_news_container);
        buttonCommit.setOnClickListener(this::onButtonCommitClick);
        buttonClearBlackList.setOnClickListener(this::onButtonClearBlackListClick);
        String urlString = PreferencesManager.extractUrl(this);
        if (!TextUtils.isEmpty(urlString)) {
            editTextUrl.setText(urlString);
        }
    }

    public void clearAsyncAllEntryFromBlackListDb() {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.clearAllEntryFromBlackList();
                runOnUiThread(() -> {
                    newsList.clear();
                    recyclerView.getAdapter().notifyDataSetChanged();
                    RssWidgetProvider.sendActionToAllWidgets(this, ACTION_DATASET_CHANGED);
                });
            }
        };
        executor.execute(runnable);
    }

    public void executeNetLoader(String urlString) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_URL, urlString);
        Loader loader = getSupportLoaderManager().getLoader(NetLoader.LOADER_ID);
        if (loader == null) {
            getSupportLoaderManager().initLoader(NetLoader.LOADER_ID, bundle, this);
        } else {
            getSupportLoaderManager().restartLoader(NetLoader.LOADER_ID, bundle, this);
        }
    }

    public void enableViewsState() {
        editTextUrl.setEnabled(true);
        buttonCommit.setEnabled(true);
    }

    public void disableViewsState() {
        editTextUrl.setEnabled(false);
        buttonCommit.setEnabled(false);
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
                setWidgetId(intent.getExtras());
                break;
            case ACTION_OPEN_SETTINGS:
                setWidgetId(intent.getExtras());
                flag = ConfigurationFlag.Custom;
                break;
        }
    }

    private void setWidgetId(Bundle extras) {
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void notifyWidgetsConfigurationCanceled() {
        RssWidgetProvider.sendActionToAllWidgets(this, Constants.ACTION_INITIAL_CONFIG);
        if (flag == ConfigurationFlag.Initial) {
            setResult(RESULT_OK, getSuccessResultIntent(mAppWidgetId));
        }
        finish();
    }

    private Intent getSuccessResultIntent(int appWidgetId) {
        Intent resultValue = new Intent();
        resultValue.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return resultValue;
    }

    @NonNull
    @Override
    public Loader<LoaderData> onCreateLoader(int id, @Nullable Bundle args) {
        Loader<LoaderData> loader = null;
        switch (id) {
            case NetLoader.LOADER_ID:
                showProgress();
                disableViewsState();
                loader = new NetLoader(this, args);
                break;
            case BlackListLoader.LOADER_ID:
                loader = new BlackListLoader(this);
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<LoaderData> loader, LoaderData loaderData) {
        int loaderId = loader.getId();
        switch (loaderId) {
            case NetLoader.LOADER_ID:
                hideProgress();
                enableViewsState();
                if (loaderData == null) return;
                if (loaderData.getStatus() == LoaderData.Status.Success) {
                    PreferencesManager.putUrl(SettingsActivity.this, loaderData.getUrlString());
                    notifyWidgetsConfigurationCanceled();
                } else {
                    Toast.makeText(this, "Ошибка " + loaderData.getStatus().name(), Toast.LENGTH_SHORT).show();
                }
                break;
            case BlackListLoader.LOADER_ID:
                List<News> localNewsList = loaderData.getListNews();
                this.newsList.clear();
                this.newsList.addAll(localNewsList);
                if (recyclerView.getAdapter() == null) {
                    RVBlackListAdapter adapter = new RVBlackListAdapter(this, newsList, this);
                    recyclerView.setAdapter(adapter);
                } else {
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                break;
        }


    }

    @Override
    public void onLoaderReset(@NonNull Loader<LoaderData> loader) {

    }

    private enum ConfigurationFlag {
        Initial, Custom
    }

}
