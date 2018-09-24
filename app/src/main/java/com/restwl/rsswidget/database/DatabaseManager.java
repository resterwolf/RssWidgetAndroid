package com.restwl.rsswidget.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.io.Closeable;

public class DatabaseManager extends SQLiteOpenHelper implements Closeable {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "RssWidget.db";

    public static final String NEWS_TABLE_NAME = "NEWS_TABLE";
    public static final String SHORT_NEWS_TABLE_NAME = "NT";

    public static final String BLACK_LIST_TABLE_NAME = "BLACK_LIST_TABLE";
    public static final String SHORT_BLACK_LIST_TABLE_NAME = "BLT";

    public static final String _ID = "id";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String PUB_DATE = "pubDate";
    public static final String LINK = "link";
    public static final String NEWS_ID = "newsId";

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
                    NEWS_ID + " INTEGER," +
                    " FOREIGN KEY (" + NEWS_ID + ") REFERENCES " + NEWS_TABLE_NAME + "(" + _ID + ") ON DELETE CASCADE);";

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

    public Cursor queryNewsTable(@Nullable String[] projection, @Nullable String selection,
                                 @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(NEWS_TABLE_NAME, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    public Cursor queryBlackListTable(@Nullable String[] projection, @Nullable String selection,
                                      @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();
        String sqlQuery = "select * from " + NEWS_TABLE_NAME + " as NT "
                + "inner join " + BLACK_LIST_TABLE_NAME + " as BLT "
                + "on NT." + _ID + " = BLT." + NEWS_ID;
        if (selectionArgs != null) {
            sqlQuery += " where NT." + _ID + " = ?";
        }
        return db.rawQuery(sqlQuery, selectionArgs);
    }

//    public long insertNewsTable(@Nullable ContentValues contentValues) {
//        SQLiteDatabase db = getWritableDatabase();
//        return db.insert(NEWS_TABLE_NAME, null, contentValues);
//    }

    public long insertNewsTable(@Nullable ContentValues contentValues) {
        SQLiteDatabase db = getWritableDatabase();
        String title = contentValues.getAsString(TITLE);
        String description = contentValues.getAsString(DESCRIPTION);
        long pubDate = contentValues.getAsLong(PUB_DATE);
        String link = contentValues.getAsString(LINK);

        String existEntrySqlQuery = "select * from " + NEWS_TABLE_NAME + " where " + TITLE + " = '" + title + "' AND " +
                DESCRIPTION + " = '" + description + "' AND " +
                PUB_DATE + " = '" + pubDate + "' AND " +
                LINK + " = '" + link + "'";

        Cursor entryExistCursor = db.rawQuery(existEntrySqlQuery, null);
        if (entryExistCursor.getCount() == 0) {
            return db.insert(NEWS_TABLE_NAME, null, contentValues);
        }

        entryExistCursor.moveToFirst();
        return entryExistCursor.getLong(entryExistCursor.getColumnIndex(_ID));
    }

    public long insertBlackListTable(ContentValues contentValues) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(BLACK_LIST_TABLE_NAME, null, contentValues);
    }

    public int deleteNewsTable(@Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(NEWS_TABLE_NAME, selection, selectionArgs);
    }

    public void deleteEntryByDateFromNewsTable(String offset) {
        SQLiteDatabase db = getWritableDatabase();
        String sqlQuery = "DELETE FROM " + NEWS_TABLE_NAME + " WHERE " + PUB_DATE +
                " IN (SELECT " + PUB_DATE + " FROM " + NEWS_TABLE_NAME + " ORDER BY " +
                PUB_DATE + " DESC LIMIT -1 OFFSET " + offset + ");";
        db.execSQL(sqlQuery);
    }

    public int deleteBlackListTable(@Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(BLACK_LIST_TABLE_NAME, selection, selectionArgs);
    }

    public Cursor queryLeftInnerJoin(@Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();
        String sqlQuery = "select " + SHORT_NEWS_TABLE_NAME + "." + _ID + ", " +
                SHORT_NEWS_TABLE_NAME + "." + TITLE + ", " +
                SHORT_NEWS_TABLE_NAME + "." + DESCRIPTION + ", " +
                SHORT_NEWS_TABLE_NAME + "." + PUB_DATE + ", " +
                SHORT_NEWS_TABLE_NAME + "." + LINK + ", " +
                SHORT_BLACK_LIST_TABLE_NAME + "." + NEWS_ID +
                " from " + NEWS_TABLE_NAME + " as " + SHORT_NEWS_TABLE_NAME + " "
                + "left join " + BLACK_LIST_TABLE_NAME + " as " + SHORT_BLACK_LIST_TABLE_NAME + " "
                + "on " + SHORT_NEWS_TABLE_NAME + "." + _ID + " = " + SHORT_BLACK_LIST_TABLE_NAME + "." + NEWS_ID;

        if (selectionArgs != null) {
            sqlQuery += " where " + SHORT_NEWS_TABLE_NAME + "." + _ID + " = ?";
        }
        if (sortOrder != null) {
            sqlQuery += " ORDER BY " + sortOrder;
        }

        Cursor cursor = db.rawQuery(sqlQuery, selectionArgs);
        return cursor;
    }

}
