package ru.ifmo.md.lesson6.rssreader;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.BaseColumns;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RssLoaderService extends IntentService {
    private static final String ACTION_LOAD_ONE = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ONE";
    private static final String ACTION_LOAD_ALL = "ru.ifmo.md.lesson6.rssreader.action.LOAD_ALL";

    private static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.rssreader.extra.CHANNEL_ID";
    public static final String EXTRA_NEW_POSTS = "ru.ifmo.md.lesson6.rssreader.extra.new_posts";

    public static final int RESULT_NO_INTERNET = 0;
    public static final int RESULT_BAD_CHANNEL = 1;
    public static final int RESULT_OK = 2;
    public static final int RESULT_FAIL = 3;
    public static final int RESULT_ALREADY_EXISTS = 4;

    private ResultReceiver mReceiver;

    public static void startActionAddChannel(Context context, String url, RssResultReceiver receiver) {
        Cursor cursor = context.getContentResolver().query(
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.URL_COLUMNS,
                RssContract.Channels.CHANNEL_LINK + " = ?",
                new String[]{url},
                null
        );
        if (cursor.getCount() > 0) {
            receiver.send(RESULT_ALREADY_EXISTS, Bundle.EMPTY);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RssContract.Channels.CHANNEL_TITLE, "Loading");
        values.put(RssContract.Channels.CHANNEL_LINK, url);
        Uri uri = context.getContentResolver().insert(RssContract.Channels.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        context.getContentResolver().notifyChange(RssContract.Channels.CONTENT_URI, null);

        startActionLoadOne(context, id, receiver);
    }

    public static void startActionLoadOne(Context context, long channelId, RssResultReceiver receiver) {
        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ONE);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(Constants.EXTRA_RECEIVER, receiver);
        context.startService(intent);
    }

    public static void startActionLoadAll(Context context, RssResultReceiver receiver) {
        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ALL);
        intent.putExtra(Constants.EXTRA_RECEIVER, receiver);
        context.startService(intent);
    }

    public RssLoaderService() {
        super("RssLoaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mReceiver = intent.getParcelableExtra(Constants.EXTRA_RECEIVER);
            if (!isOnline()) {
                mReceiver.send(RESULT_NO_INTERNET, Bundle.EMPTY);
                return;
            }
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
                RssContract.Channels.URL_COLUMNS,
                BaseColumns._ID + " = ?",
                new String[]{channelId},
                null
        );
        if (cursor.getCount() == 0) {
            return;
        }
        cursor.moveToFirst();
        final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
        final String chId = Long.toString(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
        int newPosts = 0;
        try {
            newPosts = loadChannel(url, chId);
        } catch (IOException e) {
            if (e.getClass() == MalformedURLException.class) {
                mReceiver.send(RESULT_BAD_CHANNEL, Bundle.EMPTY);
            } else {
                mReceiver.send(RESULT_FAIL, Bundle.EMPTY);
            }
        }
        mReceiver.send(RESULT_OK, createResultBundle(newPosts));
    }

    private void handleActionLoadAll() {
        Cursor cursor = getContentResolver().query(
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.URL_COLUMNS,
                null, null, null
        );

        int newPosts = 0;

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
            final String id = Long.toString(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
            int result = 0;
            try {
                result = loadChannel(url, id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result > 0) {
                newPosts += result;
            }
            cursor.moveToNext();
        }

        mReceiver.send(RESULT_OK, createResultBundle(newPosts));
    }

    private int loadChannel(final String tUrl, String channelId) throws IOException {
        URL url;
        int newPosts = 0;
        try {
            url = new URL(tUrl);
        } catch (MalformedURLException e) {
            getContentResolver().delete(
                    RssContract.Channels.buildChannelUri(channelId), null, null);
            throw e;
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.connect();
        InputStream is = connection.getInputStream();
        String encoding = "utf-8";
        final String contentType = connection.getHeaderField("Content-type");
        if (contentType != null && contentType.contains("charset=")) {
            Matcher m = Pattern.compile("charset=([^\\s]+)").matcher(contentType);
            if (m.find()) {
                encoding = m.group(1);
            }
        }
        InputStreamReader isr = new InputStreamReader(is, encoding);
        RssChannel channel;
        try {
            channel = RssParser.parse(isr);
        } catch (IOException e) {
            throw e;
        } catch (SAXException e) {
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            getContentResolver().delete(RssContract.Channels.buildChannelUri(channelId), null, null);
            throw new MalformedURLException("Invalid RSS url");
        }

        ContentValues values = new ContentValues();
        values.put(RssContract.ChannelsColumns.CHANNEL_TITLE, channel.getTitle());
        values.put(RssContract.ChannelsColumns.CHANNEL_LINK, channel.getUrl());
        getContentResolver().update(
                RssContract.Channels.buildChannelUri(channelId), values, null, null);

        Cursor cursor = getContentResolver().query(
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.URL_COLUMNS,
                RssContract.Channels.CHANNEL_LINK + " = ?",
                new String[]{channel.getUrl()},
                null
        );
        /*
            if insert already existing channel, delete current channel
         */
        if (cursor.getCount() >= 2) {
            getContentResolver().delete(
                    RssContract.Channels.buildChannelUri(channelId), null, null);
            mReceiver.send(RESULT_ALREADY_EXISTS, Bundle.EMPTY);
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final String curChannelId = Long.toString(cursor.getLong(cursor.getColumnIndex(RssContract.Channels._ID)));
            if (!curChannelId.equals(channelId)) {
                channelId = curChannelId;
                break;
            }
            cursor.moveToNext();
        }

        for (RssPost post : channel.getPosts()) {
            cursor = getContentResolver().query(
                    RssContract.Posts.buildPostUrlUri(),
                    RssContract.Posts.URL_COLUMNS,
                    RssContract.Posts.POST_LINK + " = ?",
                    new String[]{post.getUrl()},
                    null
            );
            if (cursor.getCount() > 0) {
                cursor.close();
                continue;
            }

            values = new ContentValues();
            values.put(RssContract.Posts.POST_LINK, post.getUrl());
            values.put(RssContract.Posts.POST_TITLE, post.getTitle());
            values.put(RssContract.Posts.POST_DESCRIPTION, post.getDescription());
            values.put(RssContract.Posts.POST_CHANNEL, channelId);

            newPosts++;
            getContentResolver().insert(RssContract.Posts.CONTENT_URI, values);
        }
        Log.d("TAG", "Add " + newPosts + " posts to channel (" + channelId + ") " + channel.getTitle());

        return newPosts;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private Bundle createResultBundle(int posts) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_NEW_POSTS, posts);
        return bundle;
    }

    private static class RssParser {
        private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";

        private static final RootElement root = new RootElement("rss");
        private static final Element channel = root.getChild("channel");
        private static final Element channelUrl = channel.getChild(ATOM_NAMESPACE, "link");
        private static final Element channelTitle = channel.getChild("title");
        private static final Element channelDescription = channel.getChild("description");

        private static final Element post = channel.getChild("item");
        private static final Element postUrl = post.getChild("link");
        private static final Element postTitle = post.getChild("title");
        private static final Element postDescription = post.getChild("description");
        private static final Element postDate = post.getChild("pubDate");
        private static final Element postGuid = post.getChild("guid");

        private static RssChannel curChannel;
        private static RssPost curPost;

        static {
            channel.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    curChannel = new RssChannel();
                }
            });

            channelUrl.setElementListener(new ElementListener() {
                @Override
                public void end() {

                }

                @Override
                public void start(Attributes attributes) {
                    String url = attributes.getValue("href");
                    String[] parts = url.split("\\s+");
                    if (parts.length > 0) {
                        url = parts[0];
                    }
                    Log.d("TAG", url);

                    if (url != null) {
                        curChannel.setUrl(url.trim());
                    }
                }
            });

            channelTitle.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curChannel.setTitle(s.trim());
                }
            });

            channelDescription.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curChannel.setDescription(s.trim());
                }
            });

            channel.setEndElementListener(new ElementListener() {
                @Override
                public void end() {
                    //TODO: add new channel
                }

                @Override
                public void start(Attributes attributes) {

                }
            });

            post.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    curPost = new RssPost();
                }
            });

            postUrl.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setUrl(s.trim());
                }
            });

            postTitle.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setTitle(s.trim());
                }
            });

            postDate.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setDate(s);
                }
            });

            postDescription.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setDescription(s.trim());
                }
            });

            postGuid.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setGuid(s.trim());
                }
            });

            post.setEndElementListener(new ElementListener() {
                @Override
                public void end() {
                    curChannel.addPost(curPost);
                }

                @Override
                public void start(Attributes attributes) {

                }
            });
        }

        public static RssChannel parse(InputStreamReader isr) throws IOException, SAXException {
            Xml.parse(isr, root.getContentHandler());
            return curChannel;
        }
    }
}
