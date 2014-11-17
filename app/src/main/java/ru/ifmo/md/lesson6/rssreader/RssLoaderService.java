package ru.ifmo.md.lesson6.rssreader;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;


public class RssLoaderService extends IntentService {
    private static final String ACTION_LOAD_ONE = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ONE";
    private static final String ACTION_LOAD_ALL = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ALL";

    private static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.rssreader.extra.CHANNEL_ID";


    public static void startActionLoadOne(Context context, String url) {
        ContentValues values = new ContentValues();
        values.put(RssContract.Channels.CHANNEL_TITLE, "Loading");
        values.put(RssContract.Channels.CHANNEL_LINK, url);
        Uri uri = context.getContentResolver().insert(RssContract.Channels.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        context.getContentResolver().notifyChange(RssContract.Channels.CONTENT_URI, null);

        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ONE);
        intent.putExtra(EXTRA_CHANNEL_ID, id);
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

    }

    private void handleActionLoadAll() {

    }
}
