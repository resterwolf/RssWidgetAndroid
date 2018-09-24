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
import android.util.Log;

import java.util.List;

import com.restwl.rsswidget.database.DatabaseManager;
import com.restwl.rsswidget.model.News;

import static com.restwl.rsswidget.utils.WidgetConstants.TAG;

public class WidgetContentProvider extends ContentProvider {

    private DatabaseManager databaseManager;

    public static final String AUTHORITY = "com.restwl.provider.RssWidget";

    public static final String NEWS_PATH = "news";
    public static final String BLACK_LIST_PATH = "blackList";
    public static final String FILTERED_NEWS_PATH = "filteredNews";

    public static final Uri NEWS_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + NEWS_PATH);
    public static final Uri BLACK_LIST_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + BLACK_LIST_PATH);
    public static final Uri FILTERED_NEWS_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + FILTERED_NEWS_PATH);

    static final String NEWS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + NEWS_PATH;
    static final String NEWS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + NEWS_PATH;

    static final String BLACK_LIST_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + BLACK_LIST_PATH;
    static final String BLACK_LIST_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + BLACK_LIST_PATH;

    static final String FILTERED_NEWS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + FILTERED_NEWS_PATH;
    static final String FILTERED_NEWS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + FILTERED_NEWS_PATH;

    static final int URI_NEWS = 100;
    static final int URI_NEWS_ID = 101;

    static final int URI_BLACK_LIST = 200;
    static final int URI_BLACK_LIST_ID = 201;

    static final int URI_FILTERED_NEWS = 300;
    static final int URI_FILTERED_NEWS_ID = 301;

    // описание и создание UriMatcher
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, NEWS_PATH, URI_NEWS);
        uriMatcher.addURI(AUTHORITY, NEWS_PATH + "/#", URI_NEWS_ID);
        uriMatcher.addURI(AUTHORITY, BLACK_LIST_PATH, URI_BLACK_LIST);
        uriMatcher.addURI(AUTHORITY, BLACK_LIST_PATH + "/#", URI_BLACK_LIST_ID);
        uriMatcher.addURI(AUTHORITY, FILTERED_NEWS_PATH, URI_FILTERED_NEWS);
        uriMatcher.addURI(AUTHORITY, FILTERED_NEWS_PATH + "/#", URI_FILTERED_NEWS_ID);
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

            case URI_BLACK_LIST:
                cursor = databaseManager.queryBlackListTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        BLACK_LIST_CONTENT_URI);
                break;

            case URI_BLACK_LIST_ID:
                String blackListItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + blackListItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager.NEWS_ID + " = " + blackListItemId;
                }
                cursor = databaseManager.queryBlackListTable(projection, selection,
                        selectionArgs, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(),
                        BLACK_LIST_CONTENT_URI);
                break;

            case URI_FILTERED_NEWS:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DatabaseManager.SHORT_NEWS_TABLE_NAME + "." + DatabaseManager.PUB_DATE + " DESC";
                }
                cursor = getNoBlockedEntry(databaseManager.queryLeftInnerJoin(selectionArgs, sortOrder));
                cursor.setNotificationUri(getContext().getContentResolver(),
                        FILTERED_NEWS_CONTENT_URI);
                break;

            case URI_FILTERED_NEWS_ID:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DatabaseManager.SHORT_NEWS_TABLE_NAME + "." + DatabaseManager.PUB_DATE + " DESC";
                }
                cursor = getNoBlockedEntry(databaseManager.queryLeftInnerJoin(selectionArgs, sortOrder));
                cursor.setNotificationUri(getContext().getContentResolver(),
                        FILTERED_NEWS_CONTENT_URI);
                break;

            default:
                Log.d(TAG, "query: ");
                Log.d(TAG, "uri id:");
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
        } else if (uriMatcher.match(uri) == URI_BLACK_LIST) {
            long rowID = databaseManager.insertBlackListTable(contentValues);
            resultUri = ContentUris.withAppendedId(BLACK_LIST_CONTENT_URI, rowID);
        } else if (uriMatcher.match(uri) == URI_FILTERED_NEWS) {
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

            case URI_BLACK_LIST:
                cnt = databaseManager.deleteBlackListTable(selection, selectionArgs);
                break;
            case URI_BLACK_LIST_ID:
                String blackListItemId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = DatabaseManager._ID + " = " + blackListItemId;
                } else {
                    selection = selection + " AND " + DatabaseManager._ID + " = " + blackListItemId;
                }
                cnt = databaseManager.deleteBlackListTable(selection, selectionArgs);
                break;

            case URI_FILTERED_NEWS:
                throw new UnsupportedOperationException();

            case URI_FILTERED_NEWS_ID:
                throw new UnsupportedOperationException();

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
            case URI_BLACK_LIST:
                return BLACK_LIST_CONTENT_TYPE;
            case URI_BLACK_LIST_ID:
                return BLACK_LIST_CONTENT_ITEM_TYPE;
            case URI_FILTERED_NEWS:
                return FILTERED_NEWS_CONTENT_TYPE;
            case URI_FILTERED_NEWS_ID:
                return FILTERED_NEWS_CONTENT_ITEM_TYPE;
        }
        return null;
    }

    private Cursor getNoBlockedEntry(Cursor cursor) {
        String[] columnNames = {DatabaseManager._ID, DatabaseManager.TITLE,
                DatabaseManager.DESCRIPTION, DatabaseManager.PUB_DATE, DatabaseManager.LINK};
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(DatabaseManager._ID));
                String title = cursor.getString(cursor.getColumnIndex(DatabaseManager.TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DatabaseManager.DESCRIPTION));
                String link = cursor.getString(cursor.getColumnIndex(DatabaseManager.LINK));
                long pubDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.PUB_DATE));
                if (cursor.isNull(cursor.getColumnIndex(DatabaseManager.NEWS_ID))) {
                    matrixCursor.newRow().add(DatabaseManager._ID, id)
                            .add(DatabaseManager.TITLE, title)
                            .add(DatabaseManager.DESCRIPTION, description)
                            .add(DatabaseManager.PUB_DATE, pubDate)
                            .add(DatabaseManager.LINK, link);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return matrixCursor;
    }

    // Public methods

    public static Cursor getAllEntryFromTableNews(Context context) {
        return context.getContentResolver().query(NEWS_CONTENT_URI, null, null, null, null);
    }

    public static Cursor getEntryFromTableNews(Context context, int id) {
        Uri uriId = Uri.parse("content://" + WidgetContentProvider.AUTHORITY + "/" + WidgetContentProvider.NEWS_PATH + "/" + id);
        return context.getContentResolver().query(uriId, null, null, null, null);
    }

    public static void insertEntryInBlackListTable(Context context, ContentValues values) {
        context.getContentResolver().insert(BLACK_LIST_CONTENT_URI, values);
    }

    public static void insertEntryInNewsTable(Context context, ContentValues values) {
        context.getContentResolver().insert(NEWS_CONTENT_URI, values);
    }

    public static Cursor getAllFilteredNewsFromDatabase(Context context) {
        Cursor cursor = context.getContentResolver().query(FILTERED_NEWS_CONTENT_URI, null, null, null, null);
        return cursor;
    }

    public static Cursor getAllEntryFromBlackListTable(Context context) {
        return context.getContentResolver().query(BLACK_LIST_CONTENT_URI, null, null, null, null);
    }

    public static void clearNewsTable(Context context) {
        context.getContentResolver().delete(NEWS_CONTENT_URI, null, null);
    }

    public static void clearBlackListTable(Context context) {
        context.getContentResolver().delete(BLACK_LIST_CONTENT_URI, null, null);
    }

    public static void deleteEntryBlackListTable(Context context, int newsId) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/" + BLACK_LIST_PATH + "/" + newsId);
        context.getContentResolver().delete(uri, null, null);
    }

    public static void insertAllNewsInNewsTable(Context context, List<News> newsList) {
        ContentValues[] contentValues = new ContentValues[newsList.size()];
        for (int i = 0; i < newsList.size(); i++) {
            contentValues[i] = newsList.get(i).forNewsTable();
        }
        context.getContentResolver().bulkInsert(NEWS_CONTENT_URI, contentValues);
    }

}
