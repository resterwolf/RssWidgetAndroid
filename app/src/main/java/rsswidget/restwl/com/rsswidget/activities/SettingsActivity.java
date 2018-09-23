package rsswidget.restwl.com.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import rsswidget.restwl.com.rsswidget.loaders.DataRecipientLoader;
import rsswidget.restwl.com.rsswidget.model.LoaderData;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.utils.WidgetConstants;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.ACTION_UPDATE_WIDGET_DATA_AND_VIEW;
import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.ACTION_OPEN_SETTINGS;
import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.EXTRA_URL;

public class SettingsActivity extends AppCompatActivity implements RVBlackListAdapter.CellClickListener,
        LoaderManager.LoaderCallbacks<LoaderData> {

    private ExecutorService executor;
    private int mAppWidgetId;
    private List<News> newsList = new ArrayList<>();
    private ConfigurationFlag flag = ConfigurationFlag.Initial;
    private String prefUrlString;

    // UI
    private AppCompatEditText editTextUrl;
    private Button buttonCommit, buttonClearBlackList;
    private ProgressBar progressBar;
    private RecyclerView rvBlackList;
    private TextInputLayout textInputUrlWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        executor = Executors.newSingleThreadExecutor();
        initActionBar();
        initViews();
        handleIndent();
        initRecyclerView();
        executeBlackListLoader();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard();
        return super.onTouchEvent(event);
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
        removeAsyncNewsFromBlackList(news);
    }

    @Override
    public void onItemClick(View viewGroup, News news, int index) {
        openNewsInWebView(news);
    }

    private void onButtonCommitClick(View view) {
        hideKeyboard();
        textInputUrlWrapper.setErrorEnabled(false);
        String newUrlString = editTextUrl.getText().toString().trim();

        if (TextUtils.isEmpty(newUrlString)) {
            textInputUrlWrapper.setError(getString(R.string.settings_activity_empty_enter));
            return;
        }

        if (HelperUtils.urlIsValid(newUrlString)) {
            if (TextUtils.equals(prefUrlString, newUrlString)) {
                cancelConfigurationAndNotifyWidgets();
                return;
            }
            executeNetLoader(newUrlString);
        } else {
            textInputUrlWrapper.setError(getString(R.string.settings_activity_invalid_format_url));
        }
    }

    private void onButtonClearBlackListClick(View view) {
        if (newsList.isEmpty()) return;
        removeAsyncAllNewsFromBlackList();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_activity_actionbar_title);
        }
    }

    private void initViews() {
        editTextUrl = findViewById(R.id.ev_url);
        buttonCommit = findViewById(R.id.button_commit);
        progressBar = findViewById(R.id.progressBar);
        buttonClearBlackList = findViewById(R.id.button_clear_black_list);
        textInputUrlWrapper = findViewById(R.id.textInputLayout);
        rvBlackList = findViewById(R.id.recycler_view_blocked_news_container);
        buttonCommit.setOnClickListener(this::onButtonCommitClick);
        buttonClearBlackList.setOnClickListener(this::onButtonClearBlackListClick);
        prefUrlString = PreferencesManager.extractUrl(this);
        if (!TextUtils.isEmpty(prefUrlString)) {
            editTextUrl.setText(prefUrlString);
        }
    }

    private void handleIndent() {
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (intentAction == null) {
            finish();
            return;
        }

        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (intentAction.equals(ACTION_OPEN_SETTINGS)) {
            flag = ConfigurationFlag.Custom;
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvBlackList.setLayoutManager(linearLayoutManager);
        rvBlackList.setHasFixedSize(true);
    }

    private void executeBlackListLoader() {
        Loader loader = getSupportLoaderManager().getLoader(BlackListLoader.LOADER_ID);
        if (loader == null) {
            getSupportLoaderManager().initLoader(BlackListLoader.LOADER_ID, null, this);
        } else {
            getSupportLoaderManager().restartLoader(BlackListLoader.LOADER_ID, null, this);
        }
    }

    public void executeNetLoader(String urlString) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_URL, urlString);
        Loader loader = getSupportLoaderManager().getLoader(DataRecipientLoader.LOADER_ID);
        if (loader == null) {
            getSupportLoaderManager().initLoader(DataRecipientLoader.LOADER_ID, bundle, this);
        } else {
            getSupportLoaderManager().restartLoader(DataRecipientLoader.LOADER_ID, bundle, this);
        }
    }

    private void removeAsyncNewsFromBlackList(News news) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.removeEntryFromBlackList(news);
                runOnUiThread(() -> {
                    this.newsList.remove(news);
                    rvBlackList.getAdapter().notifyDataSetChanged();
                    RssWidgetProvider.sendActionToAllWidgets(this, ACTION_UPDATE_WIDGET_DATA_AND_VIEW);
                });
            }
        };
        executor.execute(runnable);
    }

    private void openNewsInWebView(News news) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLink()));
        startActivity(intent);
    }

    public void removeAsyncAllNewsFromBlackList() {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(this)) {
                databaseManager.clearAllEntryFromBlackList();
                runOnUiThread(() -> {
                    newsList.clear();
                    rvBlackList.getAdapter().notifyDataSetChanged();
                    RssWidgetProvider.sendActionToAllWidgets(this, ACTION_UPDATE_WIDGET_DATA_AND_VIEW);
                });
            }
        };
        executor.execute(runnable);
    }

    public void enableViewsState() {
        editTextUrl.setEnabled(true);
        buttonCommit.setEnabled(true);
    }

    public void disableViewsState() {
        editTextUrl.setEnabled(false);
        buttonCommit.setEnabled(false);
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void cancelConfigurationAndNotifyWidgets() {
        RssWidgetProvider.sendActionToAllWidgets(this, WidgetConstants.ACTION_INITIAL_CONFIG);
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

    private void handleLoaderDataStatus(LoaderData.Status status) {
        String errorMessage = getString(R.string.settings_activity_unidentified_error);
        switch (status) {
            case RemoteResourceNotRssService:
                errorMessage = getString(R.string.settings_activity_remote_resource_not_rss_service);
                break;
            case NetworkError:
                errorMessage = getString(R.string.settings_activity_connection_error);
                break;
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @NonNull
    @Override
    public Loader<LoaderData> onCreateLoader(int id, @Nullable Bundle args) {
        Loader<LoaderData> loader = null;
        switch (id) {
            case DataRecipientLoader.LOADER_ID:
                showProgress();
                disableViewsState();
                loader = new DataRecipientLoader(this, args);
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
            case DataRecipientLoader.LOADER_ID:
                hideProgress();
                enableViewsState();
                if (loaderData == null) return;
                if (loaderData.getStatus() == LoaderData.Status.Success) {
                    PreferencesManager.putUrl(SettingsActivity.this, loaderData.getUrlString());
                    cancelConfigurationAndNotifyWidgets();
                } else {
                    handleLoaderDataStatus(loaderData.getStatus());
                }
                break;

            case BlackListLoader.LOADER_ID:
                List<News> localNewsList = loaderData.getListNews();
                this.newsList.clear();
                this.newsList.addAll(localNewsList);
                if (rvBlackList.getAdapter() == null) {
                    RVBlackListAdapter adapter = new RVBlackListAdapter(this, newsList, this);
                    rvBlackList.setAdapter(adapter);
                } else {
                    rvBlackList.getAdapter().notifyDataSetChanged();
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
