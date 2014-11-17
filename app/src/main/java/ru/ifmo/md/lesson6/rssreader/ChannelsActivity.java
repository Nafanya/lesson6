package ru.ifmo.md.lesson6.rssreader;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class ChannelsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_CHANNELS = 1;

    private CursorAdapter mAdapter;
    private EditText mEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_channels);

        mEditText = (EditText) findViewById(R.id.editText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                int actionAdd = getResources().getInteger(R.integer.actionAdd);
                if (actionId == actionAdd) {
                    String potentialUrl = mEditText.getText().toString();
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

    private void addChannel(String url) {
        RssLoaderService.startActionLoadOne(getApplicationContext(), url);
    }

    private void refreshAllChannels() {
        RssLoaderService.startActionLoadAll(getApplicationContext());
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
                getLoaderManager().getLoader(LOADER_CHANNELS).forceLoad();
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
            setProgressBarIndeterminateVisibility(true);
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

}
