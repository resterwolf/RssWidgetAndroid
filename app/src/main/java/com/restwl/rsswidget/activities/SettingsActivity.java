package com.restwl.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.restwl.rsswidget.R;
import com.restwl.rsswidget.adapters.RVBlackListAdapter;
import com.restwl.rsswidget.contentprovider.WidgetContentProvider;
import com.restwl.rsswidget.loaders.DataLoader;
import com.restwl.rsswidget.model.LoaderData;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.utils.HelperUtils;
import com.restwl.rsswidget.utils.PreferencesManager;
import com.restwl.rsswidget.widgedprovider.RssWidgetProvider;

import static com.restwl.rsswidget.utils.WidgetConstants.ACTION_OPEN_SETTINGS;
import static com.restwl.rsswidget.utils.WidgetConstants.EXTRA_URL;

public class SettingsActivity extends AppCompatActivity implements RVBlackListAdapter.CellClickListener,
        LoaderManager.LoaderCallbacks<LoaderData>, Spinner.OnItemSelectedListener {

    private int mAppWidgetId;
    private List<News> newsList = new ArrayList<>();
    private ConfigurationFlag flag = ConfigurationFlag.Initial;
    private String prefUrlString;
    private int prefActualNewsHours;
    private boolean settingsHasChanged = false;

    // UI
    private TextInputEditText editTextUrl;
    private Button buttonCommit, buttonClearBlackList;
    private ProgressBar progressBar;
    private RecyclerView rvBlackList;
    private TextInputLayout textInputUrlWrapper;
    private Spinner housekeeperSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initActionBar();
        handleIntentAndPreference();
        initViews();
        initSpinner();
        initRecyclerView();
        setBlackListData();
    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.settings_spinner_housekeeper_time_hours, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        housekeeperSpinner.setAdapter(adapter);

        String[] spinnerItems = getResources().getStringArray(R.array.settings_spinner_housekeeper_time_hours);
        for (int i = 0; i < spinnerItems.length; i++) {
            if (TextUtils.equals(spinnerItems[i], String.valueOf(prefActualNewsHours))) {
                housekeeperSpinner.setSelection(i);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        WidgetContentProvider.deleteEntryBlackListTable(this, news.getId());
        newsList.remove(news);
        rvBlackList.getAdapter().notifyDataSetChanged();
        RssWidgetProvider.sendActionToAllWidgets(this, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    }

    @Override
    public void onItemClick(View viewGroup, News news, int index) {
        openNewsInWebView(news);
    }

    private void onButtonCommitClick(View view) {
        hideKeyboard();
        textInputUrlWrapper.setError("");
        String newUrlString = editTextUrl.getText().toString().trim();

        if (TextUtils.isEmpty(newUrlString)) {
            textInputUrlWrapper.setError(getString(R.string.settings_activity_empty_enter));
            return;
        }

        if (HelperUtils.urlIsInvalid(newUrlString)) {
            textInputUrlWrapper.setError(getString(R.string.settings_activity_invalid_format_url));
            return;
        }

        if (!TextUtils.equals(prefUrlString, newUrlString)) {
            settingsHasChanged = true;
        }

        if (settingsHasChanged) {
            executeNetLoader(newUrlString);
        } else {
            finishCurrentAndNotifyWidgets();
        }

    }

    private void onButtonClearBlackListClick(View view) {
        if (newsList.isEmpty()) return;
        WidgetContentProvider.clearBlackListTable(this);
        newsList.clear();
        rvBlackList.getAdapter().notifyDataSetChanged();
        RssWidgetProvider.sendActionToAllWidgets(this, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
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
        housekeeperSpinner = findViewById(R.id.housekeeper_spinner);
        housekeeperSpinner.setOnItemSelectedListener(this);
        buttonCommit.setOnClickListener(this::onButtonCommitClick);
        buttonClearBlackList.setOnClickListener(this::onButtonClearBlackListClick);

        if (!TextUtils.isEmpty(prefUrlString)) {
            editTextUrl.setText(prefUrlString);
        }
    }

    private void handleIntentAndPreference() {
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

        prefUrlString = PreferencesManager.getRssResourceUrl(this);
        prefActualNewsHours = PreferencesManager.getNewsActualPeriod(this);
    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvBlackList.setLayoutManager(linearLayoutManager);
        rvBlackList.setHasFixedSize(true);
    }

    private void setBlackListData() {
        Cursor cursor = WidgetContentProvider.getAllEntryFromBlackListTable(this);
        this.newsList.clear();
        this.newsList.addAll(News.parseNewsCursor(cursor));
        if (rvBlackList.getAdapter() == null) {
            RVBlackListAdapter adapter = new RVBlackListAdapter(this, this.newsList, this);
            rvBlackList.setAdapter(adapter);
        } else {
            rvBlackList.getAdapter().notifyDataSetChanged();
        }
    }

    public void executeNetLoader(String urlString) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_URL, urlString);
        Loader loader = getSupportLoaderManager().getLoader(DataLoader.LOADER_ID);
        if (loader == null) {
            getSupportLoaderManager().initLoader(DataLoader.LOADER_ID, bundle, this);
        } else {
            getSupportLoaderManager().restartLoader(DataLoader.LOADER_ID, bundle, this);
        }
    }

    private void openNewsInWebView(News news) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLink()));
        startActivity(intent);
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

    private void finishCurrentAndNotifyWidgets() {
        RssWidgetProvider.sendActionToAllWidgets(this, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        if (flag == ConfigurationFlag.Initial) {
            Intent resultValue = new Intent();
            resultValue.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
        }
        finish();
    }

    private void handleLoaderDataStatus(LoaderData.Status status) {
        String errorMessage = getString(R.string.settings_activity_unidentified_error);
        switch (status) {
            case ResourceIsNotRssService:
                errorMessage = getString(R.string.settings_activity_remote_resource_not_rss_service);
                break;
            case NetworkError:
                errorMessage = getString(R.string.settings_activity_connection_error);
                break;
            case ServerError:
                errorMessage = getString(R.string.settings_activity_server_error);
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
            case DataLoader.LOADER_ID:
                showProgress();
                disableViewsState();
                loader = new DataLoader(this, args);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<LoaderData> loader, LoaderData loaderData) {
        int loaderId = loader.getId();
        switch (loaderId) {
            case DataLoader.LOADER_ID:
                hideProgress();
                enableViewsState();
                if (loaderData == null) return;
                if (loaderData.getStatus() == LoaderData.Status.Success) {
                    PreferencesManager.setRssResourceUrl(SettingsActivity.this, loaderData.getUrlString());
                    finishCurrentAndNotifyWidgets();
                } else {
                    handleLoaderDataStatus(loaderData.getStatus());
                }
                break;
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<LoaderData> loader) {

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        int prefPeriod = PreferencesManager.getNewsActualPeriod(this);
        String item = ((CharSequence) adapterView.getItemAtPosition(pos)).toString();
        int period = Integer.parseInt(item);
        if (prefPeriod != period) {
            PreferencesManager.setNewsActualPeriod(this, period);
            settingsHasChanged = true;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private enum ConfigurationFlag {
        Initial, Custom
    }

}
