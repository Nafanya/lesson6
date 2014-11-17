package ru.ifmo.md.lesson6.rssreader;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;


public class RssLoaderService extends IntentService {
    private static final String ACTION_LOAD_ONE = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ONE";
    private static final String ACTION_LOAD_ALL = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ALL";

    private static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.rssreader.extra.CHANNEL_ID";


    public static void startActionAddChannel(Context context, String url) {
        ContentValues values = new ContentValues();
        values.put(RssContract.Channels.CHANNEL_TITLE, "Loading");
        values.put(RssContract.Channels.CHANNEL_LINK, url);
        Uri uri = context.getContentResolver().insert(RssContract.Channels.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        context.getContentResolver().notifyChange(RssContract.Channels.CONTENT_URI, null);

        startActionLoadOne(context, id);
    }

    public static void startActionLoadOne(Context context, long channelId) {
        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ONE);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        context.startService(intent);
    }

    public static void startActionLoadAll(Context context) {
        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ALL);
        context.startService(intent);
    }

    public RssLoaderService() {
        super("RssLoaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_ONE.equals(action)) {
                final long id = intent.getLongExtra(EXTRA_CHANNEL_ID, -1);
                if (id == -1) return;
                handleActionLoadOne(id);
            } else if (ACTION_LOAD_ALL.equals(action)) {
                handleActionLoadAll();
            }
        }
    }

    private void handleActionLoadOne(long id) {
        final String channelId = Long.toString(id);
        Cursor cursor = getContentResolver().query(
                RssContract.Channels.buildChannelUri(channelId),
                RssContract.Channels.UPDATE_COLUMNS,
                RssContract.ChannelsColumns.CHANNEL_LINK + " = ?",
                new String[]{channelId},
                null
        );
        cursor.moveToNext();
        if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return;
        }
        final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
        final long chId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        loadChannel(url, chId);
    }

    private void handleActionLoadAll() {
        Cursor cursor = getContentResolver().query(
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.UPDATE_COLUMNS,
                null, null, null
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
            final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            loadChannel(url, id);
        }
    }

    private void loadChannel(final String url, final long channelId) {

    }
}
