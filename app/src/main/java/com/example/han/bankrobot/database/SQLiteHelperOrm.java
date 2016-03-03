package com.example.han.bankrobot.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.han.bankrobot.SpeechApp;
import com.example.han.bankrobot.po.POMedia;
import com.example.han.bankrobot.utils.Logger;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class SQLiteHelperOrm extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "bankrobot.db";
    private static final int DATABASE_VERSION = 1;

    public SQLiteHelperOrm(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public SQLiteHelperOrm() {
        super(SpeechApp.getContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, POMedia.class);
        } catch (SQLException e) {
            Logger.e(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int arg2, int arg3) {
        try {
            TableUtils.dropTable(connectionSource, POMedia.class, true);
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Logger.e(e);
        }
    }
}