package rsswidget.restwl.com.rsswidget.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import rsswidget.restwl.com.rsswidget.model.News;

public class DatabaseManager extends SQLiteOpenHelper implements Closeable {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "RssWidget.db";

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

    public boolean insertEntryInNews(News news) {
        SQLiteDatabase db = getWritableDatabase();

        SQLiteStatement sqLiteStatement = db.compileStatement("insert into " + NEWS_TABLE_NAME +
                " (" + TITLE + "," + DESCRIPTION + "," + PUB_DATE + "," + LINK + ") " +
                "values (?,?,?,?);");
        sqLiteStatement.bindString(1, news.getTitle());
        sqLiteStatement.bindString(2, news.getDescription());
        sqLiteStatement.bindLong(3, news.getPubDate().getTime());
        sqLiteStatement.bindString(4, news.getLink());
        return sqLiteStatement.executeInsert() != -1;
    }

    public void insertEntriesInNews(List<News> newsList) {
        for (News news : newsList) {
            insertEntryInNews(news);
        }
    }

    public List<News> extractAllEntryFromNews() {
        List<News> newsList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from " + NEWS_TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                String title = cursor.getString(cursor.getColumnIndex(TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                String link = cursor.getString(cursor.getColumnIndex(LINK));
                long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));

                newsList.add(new News(id, title, description, link, pubDate));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return newsList;
    }

    public void deleteAllEntryFromNews() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + NEWS_TABLE_NAME);
    }

    public List<News> extractAllEntryFromBlackList() {
        SQLiteDatabase db = getReadableDatabase();
        List<News> newsList = new ArrayList<>();

        Cursor cursor = db.rawQuery("select * from " + BLACK_LIST_TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                String title = cursor.getString(cursor.getColumnIndex(TITLE));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                String link = cursor.getString(cursor.getColumnIndex(LINK));
                long pubDate = cursor.getLong(cursor.getColumnIndex(PUB_DATE));
                newsList.add(new News(id, title, description, link, pubDate));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return newsList;
    }

    public boolean insertEntryInBlackList(News news) {
        SQLiteDatabase db = getWritableDatabase();

        SQLiteStatement sqLiteStatement = db.compileStatement("insert into " + BLACK_LIST_TABLE_NAME +
                " (" + TITLE + "," + DESCRIPTION + "," + PUB_DATE + "," + LINK + ") " +
                "values (?,?,?,?);");
        sqLiteStatement.bindString(1, news.getTitle());
        sqLiteStatement.bindString(2, news.getDescription());
        sqLiteStatement.bindLong(3, news.getPubDate().getTime());
        sqLiteStatement.bindString(4, news.getLink());
        return sqLiteStatement.executeInsert() != -1;
    }

    public boolean removeEntryFromBlackList(News news) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = _ID + " = ?";
        String[] selectionArgs = {String.valueOf(news.getId())};
        return db.delete(BLACK_LIST_TABLE_NAME, selection, selectionArgs) > 0;
    }

    public void clearAllEntryFromBlackList() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + BLACK_LIST_TABLE_NAME);
    }
}
