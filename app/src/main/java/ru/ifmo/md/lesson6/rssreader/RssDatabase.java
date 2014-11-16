package ru.ifmo.md.lesson6.rssreader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

import ru.ifmo.md.lesson6.rssreader.RssContract.*;

/**
 * Created by Nikita Yaschenko on 16.11.14.
 */
public class RssDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "rss.db";

    private static final int VERSION = 1;

    private static final HashMap<String, String> predefinedChannels = new HashMap<String, String>();
    static {
        predefinedChannels.put("http://feeds.bbci.co.uk/news/rss.xml", "BBC News - Home");
        predefinedChannels.put("http://echo.msk.ru/interview/rss-fulltext.xml", "Горячие интервью  | Эхо Москвы");
        predefinedChannels.put("http://bash.im/rss/", "Bash.im");
    }

    interface Tables {
        String CHANNELS = "channels";
        String POSTS = "posts";
    }

    public RssDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CHANNELS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ChannelsColumns.CHANNEL_LINK + " TEXT NOT NULL,"
                + ChannelsColumns.CHANNEL_TITLE + " TEXT NOT NULL);"
        );

        db.execSQL("CREATE TABLE " + Tables.POSTS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PostsColumns.POST_LINK + " TEXT NOT NULL,"
                + PostsColumns.POST_TITLE + " TEXT NOT NULL,"
                + "FOREIGN KEY(" + PostsColumns.POST_CHANNEL + ") REFERENCES " +
                        Tables.CHANNELS + "(" + BaseColumns._ID + ") ON DELETE CASCADE);"
        );

        for (Map.Entry<String, String> entry : predefinedChannels.entrySet()) {
            db.execSQL("INSERT INTO " + Tables.CHANNELS + "("
                    + Channels.CHANNEL_LINK + ", " + Channels.CHANNEL_TITLE + ") "
                    + "VALUES (" + entry.getKey() + ", " + entry.getValue() + ");"
            );
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {

    }
}
