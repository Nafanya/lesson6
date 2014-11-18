package ru.ifmo.md.lesson6.rssreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by Nikita Yaschenko on 18.11.14.
 */
public class RssResultReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle data);
    }

    public RssResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle data) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, data);
        }
    }


}
