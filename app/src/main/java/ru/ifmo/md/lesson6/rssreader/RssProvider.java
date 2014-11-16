package ru.ifmo.md.lesson6.rssreader;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import ru.ifmo.md.lesson6.rssreader.RssContract.*;
import ru.ifmo.md.lesson6.rssreader.RssDatabase.Tables;

public class RssProvider extends ContentProvider {

    private RssDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildMatcher();

    private static final int CHANNELS = 100;
    private static final int CHANNELS_ID = 101;

    private static final int POSTS = 200;
    private static final int POSTS_ID = 201;

    private static UriMatcher buildMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RssContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "channels", CHANNELS);
        matcher.addURI(authority, "channels/#", CHANNELS_ID);

        matcher.addURI(authority, "posts", POSTS);
        matcher.addURI(authority, "posts/#", POSTS_ID);

        return matcher;
    }

    public RssProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        final int rows;
        switch (match) {
            case CHANNELS_ID:
                selection = selection + " AND " + Channels._ID + " = " + uri.getLastPathSegment();
                rows = db.delete(Tables.CHANNELS, selection, selectionArgs);
                break;
            case POSTS_ID:
                selection = selection + " AND " + Posts._ID + " = " + uri.getLastPathSegment();
                rows = db.delete(Tables.POSTS, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        notifyChange(uri);
        return rows;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CHANNELS:
                return Channels.CONTENT_TYPE;
            case CHANNELS_ID:
                return Channels.CONTENT_ITEM_TYPE;
            case POSTS:
                return Posts.CONTENT_TYPE;
            case POSTS_ID:
                return Posts.CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (match) {
            case CHANNELS:
                db.insert(Tables.CHANNELS, null, values);
                notifyChange(uri);
                return Channels.buildChannelUri(values.getAsString(Channels._ID));
            case POSTS:
                db.insert(Tables.POSTS, null, values);
                notifyChange(uri);
                return Posts.buildPostsUri(values.getAsString(Posts._ID));
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }

    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new RssDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = sUriMatcher.match(uri);
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (match) {
            case CHANNELS:
                builder.setTables(Tables.CHANNELS);
                break;
            case CHANNELS_ID:
                builder.setTables(Tables.CHANNELS);
                builder.appendWhere(Channels._ID + " = " + uri.getLastPathSegment());
                break;
            case POSTS:
                builder.setTables(Tables.POSTS);
                break;
            case POSTS_ID:
                builder.setTables(Tables.POSTS);
                builder.appendWhere(Posts.POST_CHANNEL + " = " + uri.getLastPathSegment());
                break;
        }
        return builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }

}
