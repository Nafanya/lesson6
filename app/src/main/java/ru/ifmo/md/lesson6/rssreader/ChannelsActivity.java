package ru.ifmo.md.lesson6.rssreader;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;


public class ChannelsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,RssObserver.Callbacks {

    private static final int LOADER_CHANNELS = 1;

    private CursorAdapter mAdapter;
    private EditText mEditText;
    private RssObserver mObserver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_channels);

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

    private void addChannel(String url) {
        RssLoaderService.startActionAddChannel(getApplicationContext(), url);
    }

    private void refreshAllChannels() {
        RssLoaderService.startActionLoadAll(getApplicationContext());
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
}
