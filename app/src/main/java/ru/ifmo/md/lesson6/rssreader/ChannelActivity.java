package ru.ifmo.md.lesson6.rssreader;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;


public class ChannelActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,RssObserver.Callbacks {
    public static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.rssreader.extra.CHANNEL_ID";

    private static final int LOADER_POSTS = 1;

    private CursorAdapter mAdapter;
    private long mChannelId;
    private RssObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        mChannelId = intent.getLongExtra(EXTRA_CHANNEL_ID, -1);
        if (mChannelId == -1) finish();

        mAdapter = new CursorAdapter(this, null, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, viewGroup, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_TITLE)));
                ((TextView) view.findViewById(android.R.id.text2)).setText(cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK)));
            }
        };

        setListAdapter(mAdapter);
        registerForContextMenu(findViewById(android.R.id.list));

        getLoaderManager().initLoader(LOADER_POSTS, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mObserver == null) {
            mObserver = new RssObserver(this);
        }
        getContentResolver().registerContentObserver(
                RssContract.Channels.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mObserver);
        if (mObserver != null) {
            mObserver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            RssLoaderService.startActionLoadOne(this, mChannelId);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(
                this,
                RssContract.Posts.buildPostsUri(Long.toString(mChannelId)),
                RssContract.Posts.ALL_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    @Override
    public void onChannelsObserverFired() {
        getLoaderManager().initLoader(LOADER_POSTS, null, this).forceLoad();
    }
}
