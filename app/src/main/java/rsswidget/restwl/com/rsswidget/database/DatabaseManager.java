package rsswidget.restwl.com.rsswidget.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import rsswidget.restwl.com.rsswidget.model.LocalNews;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;

public class DatabaseManager extends SQLiteOpenHelper implements Closeable {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RssWidget.db";

    private static final String NEWS_TABLE_NAME = "NEWS_TABLE";
    private static final String BLACK_LIST_TABLE_NAME = "BLACK_LIST_TABLE";
    private static final String _ID = "id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String PUB_DATE = "pubDate";
    private static final String LINK = "link";

    private static final String SQL_CREATE_NEWS_ENTRIES =
            "CREATE TABLE " + NEWS_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    TITLE + " TEXT," +
                    DESCRIPTION + " TEXT," +
                    PUB_DATE + " INTEGER," +
                    LINK + " TEXT)";

    private static final String SQL_CREATE_BLACK_LIST_ENTRIES =
            "CREATE TABLE " + BLACK_LIST_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    TITLE + " TEXT," +
                    DESCRIPTION + " TEXT," +
                    PUB_DATE + " INTEGER," +
                    LINK + " TEXT)";

    private static final String SQL_DELETE_NEWS_ENTRIES =
            "DROP TABLE IF EXISTS " + NEWS_TABLE_NAME;

    private static final String SQL_DELETE_BLACK_LIST_ENTRIES =
            "DROP TABLE IF EXISTS " + BLACK_LIST_TABLE_NAME;

    public DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_NEWS_ENTRIES);
        db.execSQL(SQL_CREATE_BLACK_LIST_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_NEWS_ENTRIES);
        db.execSQL(SQL_DELETE_BLACK_LIST_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public boolean insertNews(RemoteNews news) {
        SQLiteDatabase db = getWritableDatabase();

        SQLiteStatement sqLiteStatement = db.compileStatement("insert into " + NEWS_TABLE_NAME +
                " (" + TITLE + "," + DESCRIPTION + "," + PUB_DATE + "," + LINK + ") " +
                "values (?,?,?,?);");
        sqLiteStatement.bindString(1, news.getTitle());
        sqLiteStatement.bindString(2, news.getDescription());
        if (news.convertDate() != null)
            sqLiteStatement.bindLong(3, news.convertDate().getTime());
        sqLiteStatement.bindString(4, news.getLink());
        return sqLiteStatement.executeInsert() != -1;
    }

    public void insertListNews(List<RemoteNews> newsList) {
        resetAutoincrementIdsFromNewsTable();
        for (RemoteNews news : newsList) {
            insertNews(news);
        }
    }

    public LocalNews getSingleNews(News news) {
        LocalNews localNews = null;
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                _ID,
                TITLE,
                DESCRIPTION,
                PUB_DATE,
                LINK
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = TITLE + " = ?";
        String[] selectionArgs = {news.getTitle()};

        // How you want the results sorted in the resulting Cursor
//        String sortOrder =
//                FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        Cursor cursor = db.query(
                NEWS_TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                String title = cursor.getString(cursor.getColumnIndex(TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));
                String link = cursor.getString(cursor.getColumnIndex(LINK));

                localNews = new LocalNews(id, title, description, pubDate, link);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return localNews;
    }

    public LocalNews getSingleNews(int newsId) {
        LocalNews localNews = null;
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                _ID,
                TITLE,
                DESCRIPTION,
                PUB_DATE,
                LINK
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = _ID + " = ?";
        String[] selectionArgs = {String.valueOf(newsId)};

        // How you want the results sorted in the resulting Cursor
//        String sortOrder =
//                FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        Cursor cursor = db.query(
                NEWS_TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                String title = cursor.getString(cursor.getColumnIndex(TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));
                String link = cursor.getString(cursor.getColumnIndex(LINK));

                localNews = new LocalNews(id, title, description, pubDate, link);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return localNews;
    }

    public List<LocalNews> getAllNews() {
        List<LocalNews> newsList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from " + NEWS_TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                String title = cursor.getString(cursor.getColumnIndex(TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));
                String link = cursor.getString(cursor.getColumnIndex(LINK));

                newsList.add(new LocalNews(id, title, description, pubDate, link));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return newsList;
    }

    public void resetAutoincrementIdsFromNewsTable() {
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + NEWS_TABLE_NAME + "'");
    }

    public void resetAutoincrementIdsFromBlackListTable() {
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + BLACK_LIST_TABLE_NAME + "'");
    }

    public void deleteAllEntryFromNewsTable() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + NEWS_TABLE_NAME);
    }

    public void deleteAllEntryFromBlackListTable() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + BLACK_LIST_TABLE_NAME);
    }

}
