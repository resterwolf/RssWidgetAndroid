package com.restwl.rsswidget.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.restwl.rsswidget.database.DatabaseManager;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.utils.PreferencesManager;

public class WidgetContentProvider extends ContentProvider {

    private DatabaseManager databaseManager;

    private static final String AUTHORITY = "com.restwl.provider.RssWidget";

    private static final String NEWS_PATH = "news";
    private static final String BLOCK_NEWS_LIST_PATH = "blackListNews";
    private static final String ACTUAL_NEWS_LIST_PATH = "actualListNews";
    private static final String HOUSEKEEPER_PATH = "newsHousekeeper";

    private static final Uri NEWS_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + NEWS_PATH);
    private static final Uri BLOCK_NEWS_LIST_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + BLOCK_NEWS_LIST_PATH);
    private static final Uri ACTUAL_NEWS_LIST_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + ACTUAL_NEWS_LIST_PATH);
    private static final Uri HOUSEKEEPER_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + HOUSEKEEPER_PATH);

    private static final String NEWS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + NEWS_PATH;
    private static final String NEWS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + NEWS_PATH;
    private static final String BLOCK_NEWS_LIST_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + BLOCK_NEWS_LIST_PATH;
    private static final String BLOCK_NEWS_LIST_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + BLOCK_NEWS_LIST_PATH;
    private static final String ACTUAL_NEWS_LIST_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + ACTUAL_NEWS_LIST_PATH;
    private static final String ACTUAL_NEWS_LIST_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + ACTUAL_NEWS_LIST_PATH;
    private static final String HOUSEKEEPER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + HOUSEKEEPER_PATH;

    private static final int URI_NEWS = 100;
    private static final int URI_NEWS_ID = 101;
    private static final int URI_BLOCK_NEWS_LIST = 200;
    private static final int URI_BLOCK_NEWS_LIST_ID = 201;
    private static final int URI_ACTUAL_NEWS_LIST = 300;
    private static final int URI_ACTUAL_NEWS_LIST_ID = 301;
    private static final int URI_HOUSEKEEPER = 400;

    // описание и создание UriMatcher
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, NEWS_PATH, URI_NEWS);
        uriMatcher.addURI(AUTHORITY, NEWS_PATH + "/#", URI_NEWS_ID);
        uriMatcher.addURI(AUTHORITY, BLOCK_NEWS_LIST_PATH, URI_BLOCK_NEWS_LIST);
        uriMatcher.addURI(AUTHORITY, BLOCK_NEWS_LIST_PATH + "/#", URI_BLOCK_NEWS_LIST_ID);
        uriMatcher.addURI(AUTHORITY, ACTUAL_NEWS_LIST_PATH, URI_ACTUAL_NEWS_LIST);
        uriMatcher.addURI(AUTHORITY, ACTUAL_NEWS_LIST_PATH + "/#", URI_ACTUAL_NEWS_LIST_ID);
        uriMatcher.addURI(AUTHORITY, HOUSEKEEPER_PATH, URI_HOUSEKEEPER);
    }

    @Override
    public boolean onCreate() {
        databaseManager = new DatabaseManager(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case URI_NEWS:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DatabaseManager.PUB_DATE + " DESC";
                }
                cursor = databaseManager.queryNewsTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        NEWS_CONTENT_URI);
                break;

            case URI_NEWS_ID:
                String newsItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + newsItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager._ID + " = " + newsItemId;
                }
                cursor = databaseManager.queryNewsTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        NEWS_CONTENT_URI);
                break;

            case URI_BLOCK_NEWS_LIST:
                cursor = databaseManager.queryBlackListTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        BLOCK_NEWS_LIST_CONTENT_URI);
                break;

            case URI_BLOCK_NEWS_LIST_ID:
                String blackListItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + blackListItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager.NEWS_ID + " = " + blackListItemId;
                }
                cursor = databaseManager.queryBlackListTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        BLOCK_NEWS_LIST_CONTENT_URI);
                break;

            case URI_ACTUAL_NEWS_LIST:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DatabaseManager.SHORT_NEWS_TABLE_NAME + "." + DatabaseManager.PUB_DATE + " DESC";
                }
                cursor = filterActualNewsList(databaseManager.queryLeftInnerJoin(selectionArgs, sortOrder));
                cursor.setNotificationUri(getContext().getContentResolver(),
                        ACTUAL_NEWS_LIST_CONTENT_URI);
                break;

            case URI_ACTUAL_NEWS_LIST_ID:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DatabaseManager.SHORT_NEWS_TABLE_NAME + "." + DatabaseManager.PUB_DATE + " DESC";
                }
                cursor = filterActualNewsList(databaseManager.queryLeftInnerJoin(selectionArgs, sortOrder));
                cursor.setNotificationUri(getContext().getContentResolver(),
                        ACTUAL_NEWS_LIST_CONTENT_URI);
                break;

            case URI_HOUSEKEEPER:
                throw new UnsupportedOperationException();

            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Uri resultUri;
        if (uriMatcher.match(uri) == URI_NEWS) {
            long rowID = databaseManager.insertNewsTable(contentValues);
            resultUri = ContentUris.withAppendedId(NEWS_CONTENT_URI, rowID);
        } else if (uriMatcher.match(uri) == URI_BLOCK_NEWS_LIST) {
            long rowID = databaseManager.insertBlackListTable(contentValues);
            resultUri = ContentUris.withAppendedId(BLOCK_NEWS_LIST_CONTENT_URI, rowID);
        } else if (uriMatcher.match(uri) == URI_ACTUAL_NEWS_LIST) {
            throw new UnsupportedOperationException();
        } else if (uriMatcher.match(uri) == URI_HOUSEKEEPER) {
            throw new UnsupportedOperationException();
        } else {
            throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int cnt;
        switch (uriMatcher.match(uri)) {
            case URI_NEWS:
                cnt = databaseManager.deleteNewsTable(selection, selectionArgs);
                break;
            case URI_NEWS_ID:
                String newsItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + newsItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager._ID + " = " + newsItemId;
                }
                cnt = databaseManager.deleteNewsTable(selection, selectionArgs);
                break;

            case URI_BLOCK_NEWS_LIST:
                cnt = databaseManager.deleteBlackListTable(selection, selectionArgs);
                break;
            case URI_BLOCK_NEWS_LIST_ID:
                String blackListItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + blackListItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager._ID + " = " + blackListItemId;
                }
                cnt = databaseManager.deleteBlackListTable(selection, selectionArgs);
                break;

            case URI_ACTUAL_NEWS_LIST:
                throw new UnsupportedOperationException();

            case URI_ACTUAL_NEWS_LIST_ID:
                throw new UnsupportedOperationException();

            case URI_HOUSEKEEPER:
                cnt = databaseManager.deleteNewsTable(selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case URI_NEWS:
                return NEWS_CONTENT_TYPE;
            case URI_NEWS_ID:
                return NEWS_CONTENT_ITEM_TYPE;
            case URI_BLOCK_NEWS_LIST:
                return BLOCK_NEWS_LIST_CONTENT_TYPE;
            case URI_BLOCK_NEWS_LIST_ID:
                return BLOCK_NEWS_LIST_CONTENT_ITEM_TYPE;
            case URI_ACTUAL_NEWS_LIST:
                return ACTUAL_NEWS_LIST_CONTENT_TYPE;
            case URI_ACTUAL_NEWS_LIST_ID:
                return ACTUAL_NEWS_LIST_CONTENT_ITEM_TYPE;
            case URI_HOUSEKEEPER:
                return HOUSEKEEPER_CONTENT_TYPE;
        }
        return null;
    }

    private Cursor filterActualNewsList(Cursor blockNewsListCursor) {
        String[] columnNames = {DatabaseManager._ID, DatabaseManager.TITLE,
                DatabaseManager.DESCRIPTION, DatabaseManager.PUB_DATE, DatabaseManager.LINK};
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        if (blockNewsListCursor.moveToFirst()) {
            while (!blockNewsListCursor.isAfterLast()) {
                int id = blockNewsListCursor.getInt(blockNewsListCursor.getColumnIndex(DatabaseManager._ID));
                String title = blockNewsListCursor.getString(blockNewsListCursor.getColumnIndex(DatabaseManager.TITLE));
                String description = blockNewsListCursor.getString(blockNewsListCursor.getColumnIndex(DatabaseManager.DESCRIPTION));
                String link = blockNewsListCursor.getString(blockNewsListCursor.getColumnIndex(DatabaseManager.LINK));
                long pubDate = blockNewsListCursor.getLong(blockNewsListCursor.getColumnIndex(DatabaseManager.PUB_DATE));
                if (blockNewsListCursor.isNull(blockNewsListCursor.getColumnIndex(DatabaseManager.NEWS_ID))) {
                    matrixCursor.newRow().add(DatabaseManager._ID, id)
                            .add(DatabaseManager.TITLE, title)
                            .add(DatabaseManager.DESCRIPTION, description)
                            .add(DatabaseManager.PUB_DATE, pubDate)
                            .add(DatabaseManager.LINK, link);
                }
                blockNewsListCursor.moveToNext();
            }
            blockNewsListCursor.close();
        }
        return matrixCursor;
    }

    // Public methods

    public static Cursor queryAllNews(Context context) {
        return context.getContentResolver().query(NEWS_CONTENT_URI, null, null, null, null);
    }

    public static Cursor querySingleNews(Context context, int id) {
        Uri uriId = Uri.parse("content://" + WidgetContentProvider.AUTHORITY + "/" + WidgetContentProvider.NEWS_PATH + "/" + id);
        return context.getContentResolver().query(uriId, null, null, null, null);
    }

    public static void insertEntryInBlockNewsList(Context context, ContentValues values) {
        context.getContentResolver().insert(BLOCK_NEWS_LIST_CONTENT_URI, values);
    }

    public static void insertEntryInNewsTable(Context context, ContentValues values) {
        context.getContentResolver().insert(NEWS_CONTENT_URI, values);
    }

    public static Cursor queryAllActualNewsList(Context context) {
        Cursor cursor = context.getContentResolver().query(ACTUAL_NEWS_LIST_CONTENT_URI, null, null, null, null);
        return cursor;
    }

    public static Cursor queryAllBlockNewsList(Context context) {
        return context.getContentResolver().query(BLOCK_NEWS_LIST_CONTENT_URI, null, null, null, null);
    }

    public static void clearNews(Context context) {
        context.getContentResolver().delete(NEWS_CONTENT_URI, null, null);
    }

    public static void clearBlockNewsList(Context context) {
        context.getContentResolver().delete(BLOCK_NEWS_LIST_CONTENT_URI, null, null);
    }

    public static void deleteBlockNews(Context context, int newsId) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/" + BLOCK_NEWS_LIST_PATH + "/" + newsId);
        context.getContentResolver().delete(uri, null, null);
    }

    public static void insertNewsList(Context context, List<News> newsList) {
        ContentValues[] contentValues = new ContentValues[newsList.size()];
        for (int i = 0; i < newsList.size(); i++) {
            contentValues[i] = newsList.get(i).forNewsTable();
        }
        context.getContentResolver().bulkInsert(NEWS_CONTENT_URI, contentValues);
    }

    // Clear oldest data which public date less than CONSTANT hour;
    public static void executeHousekeeper(Context context) {
        String selection = DatabaseManager.PUB_DATE + " < ?";
        long currentTime = Calendar.getInstance().getTimeInMillis();
        int hours = PreferencesManager.getNewsActualPeriod(context);
        long timeOffset = TimeUnit.HOURS.toMillis(hours);
        String[] selectionArgs = new String[]{String.valueOf(currentTime - timeOffset)};
        context.getContentResolver().delete(HOUSEKEEPER_CONTENT_URI, selection, selectionArgs);
    }

}
