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
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;


public class ChannelsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,RssObserver.Callbacks, SwipeRefreshLayout.OnRefreshListener, RssResultReceiver.Receiver {

    private static final int LOADER_CHANNELS = 1;

    private CursorAdapter mAdapter;
    private EditText mEditText;
    private RssObserver mObserver = null;
    private RssResultReceiver mReceiver;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_channels);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_orange_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_red_dark,
                android.R.color.holo_blue_dark
        );

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

        mEditText = (EditText) findViewById(R.id.editText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                int actionAdd = getApplicationContext().getResources().getInteger(R.integer.actionAdd);
                if (actionId == actionAdd) {
                    String potentialUrl = mEditText.getText().toString();
                    try {
                        new URL(potentialUrl);
                    } catch (MalformedURLException e) {
                        Toast.makeText(getApplicationContext(), "Bad url", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    addChannel(potentialUrl);
                    return true;
                }
                return false;
            }
        });

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

        getLoaderManager().initLoader(LOADER_CHANNELS, null, this);
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
                RssContract.Channels.CONTENT_URI, true, mObserver);
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this).forceLoad();
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

    private void addChannel(String url) {
        RssLoaderService.startActionAddChannel(getApplicationContext(), url, mReceiver);
    }

    private void refreshAllChannels() {
        RssLoaderService.startActionLoadAll(getApplicationContext(), mReceiver);
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        long channelId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        Intent intent = new Intent(this, ChannelActivity.class);
        intent.putExtra(ChannelActivity.EXTRA_CHANNEL_ID, channelId);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.rss_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        long id = acmi.id;
        switch (item.getItemId()) {
            case R.id.delete:
                getContentResolver().delete(
                        RssContract.Channels.buildChannelUri(Long.toString(id)),
                        null, null);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channels, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshAllChannels();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(
                this,
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.ALL_COLUMNS,
                null, null, null
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
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this).forceLoad();
    }

    @Override
    public void onRefresh() {
        refreshAllChannels();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        mSwipeRefreshLayout.setRefreshing(false);
        switch (resultCode) {
            case RssLoaderService.RESULT_FAIL:
                Toast.makeText(this, "Error while update, try again.", Toast.LENGTH_SHORT).show();
                break;
            case RssLoaderService.RESULT_BAD_CHANNEL:
                Toast.makeText(this, "Invalid RSS url", Toast.LENGTH_SHORT).show();
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
}
