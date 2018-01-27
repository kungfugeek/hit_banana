package com.dev.nate.firsties;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Nate on 1/21/2018.
 */

public class BananaDBHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "banana.db";
    public static final String TABLE_HIGHSCORE = "highscore";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_HIGHSCORE = "_score";

    public BananaDBHandler(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_HIGHSCORE + "(" +
                COLUMN_ID + " INTEGER, " +
                COLUMN_HIGHSCORE + " INTEGER " +
                ");";
        db.execSQL(query);
        initHighScore(0L, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIGHSCORE);
        onCreate(db);
    }


    private void initHighScore(long score, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_HIGHSCORE, score);
        values.put(COLUMN_ID, 1);
        db.insert(TABLE_HIGHSCORE, null, values);
    }

    public void updateHighScore(long score) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_HIGHSCORE, score);
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_HIGHSCORE, values, COLUMN_ID + " = 1", null);
    }

    public long getHighScore() {
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_HIGHSCORE + " FROM " + TABLE_HIGHSCORE + " WHERE " + COLUMN_ID + " = 1";
        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        long val = c.getLong(c.getColumnIndex(COLUMN_HIGHSCORE));
        return val;
    }

    public void close() {
        SQLiteDatabase db = getWritableDatabase();
        db.close();
    }
}
