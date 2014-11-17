package ru.ifmo.md.lesson6.rssreader;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

/**
 * Created by Nikita Yaschenko on 17.11.14.
 */
public class ChannelsObserver extends ContentObserver {
    Handler mHandler;
    Callbacks mCallback = null;

    public interface Callbacks {
        public void onChannelsObserverFired();
    }

    public ChannelsObserver(Callbacks callback) {
        super(null);
        mHandler = new Handler();
        mCallback = callback;
    }

    @Override
    public void onChange(boolean selfChange) {
        mCallback.onChannelsObserverFired();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }
}