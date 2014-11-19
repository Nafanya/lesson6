package ru.ifmo.md.lesson6.rssreader;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class ChannelActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,RssObserver.Callbacks, RssResultReceiver.Receiver, SwipeRefreshLayout.OnRefreshListener {
    public static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.rssreader.extra.CHANNEL_ID";

    private static final int LOADER_POSTS = 1;
    private static final int LOADER_CHANNEL_INFO = 2;

    private CursorAdapter mAdapter;
    private long mChannelId;
    private RssObserver mObserver;
    private RssResultReceiver mReceiver;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mChannelId = intent.getLongExtra(EXTRA_CHANNEL_ID, -1);
        if (mChannelId == -1) finish();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        final ListView listView = getListView();

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                final int topRowVerticalPosition;
                if (listView == null || listView.getChildCount() == 0) {
                    topRowVerticalPosition = 0;
                } else {
                    topRowVerticalPosition = listView.getChildAt(0).getTop();
                }
                mSwipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        mAdapter = new CursorAdapter(this, null, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, viewGroup, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(cursor.getColumnIndex(RssContract.Posts.POST_TITLE)));
                ((TextView) view.findViewById(android.R.id.text2)).setText(cursor.getString(cursor.getColumnIndex(RssContract.Posts.POST_LINK)));
            }
        };

        setListAdapter(mAdapter);
        registerForContextMenu(findViewById(android.R.id.list));

        getLoaderManager().initLoader(LOADER_POSTS, null, this);
        getLoaderManager().initLoader(LOADER_CHANNEL_INFO, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new RssResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        if (mObserver == null) {
            mObserver = new RssObserver(this);
        }
        getContentResolver().registerContentObserver(
                RssContract.Posts.CONTENT_URI, true, mObserver);
        getLoaderManager().initLoader(LOADER_POSTS, null, this).forceLoad();
    }

    @Override
    public void onPause() {
        super.onPause();
        mReceiver.setReceiver(null);
        getContentResolver().unregisterContentObserver(mObserver);
        if (mObserver != null) {
            mObserver = null;
        }
    }

    @Override
    protected void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        String url = cursor.getString(cursor.getColumnIndex(RssContract.Posts.POST_LINK));
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                refreshChannel();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshChannel() {
        mSwipeRefreshLayout.setRefreshing(true);
        RssLoaderService.startActionLoadOne(this, mChannelId, mReceiver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        switch (id) {
            case LOADER_POSTS:
                return new CursorLoader(
                        this,
                        RssContract.Posts.buildPostsUri(Long.toString(mChannelId)),
                        RssContract.Posts.ALL_COLUMNS,
                        null, null, null);
            case LOADER_CHANNEL_INFO:
                return new CursorLoader(
                        this,
                        RssContract.Channels.buildChannelUri(Long.toString(mChannelId)),
                        RssContract.Channels.ALL_COLUMNS,
                        null, null, null);
            default:
                throw new UnsupportedOperationException("Unknown loader: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case LOADER_POSTS:
                mAdapter.swapCursor(cursor);
                break;
            case LOADER_CHANNEL_INFO:
                cursor.moveToFirst();
                final String title = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_TITLE));
                setTitle(title);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader: " + cursorLoader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    @Override
    public void onChannelsObserverFired() {
        getLoaderManager().initLoader(LOADER_POSTS, null, this).forceLoad();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        mSwipeRefreshLayout.setRefreshing(false);
        switch (resultCode) {
            case RssLoaderService.RESULT_FAIL:
                Toast.makeText(this, "Error while update, try again.", Toast.LENGTH_SHORT).show();
                break;
            case RssLoaderService.RESULT_OK:
                int newPosts = data.getInt(RssLoaderService.EXTRA_NEW_POSTS, 0);
                if (newPosts > 0) {
                    Toast.makeText(this, "You have " + newPosts + " new posts", Toast.LENGTH_SHORT).show();
                }
                break;
            case RssLoaderService.RESULT_NO_INTERNET:
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onRefresh() {
        refreshChannel();
    }
}
