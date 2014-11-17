package ru.ifmo.md.lesson6.rssreader;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.StartElementListener;
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


    public static void startActionAddChannel(Context context, String url) {
        ContentValues values = new ContentValues();
        values.put(RssContract.Channels.CHANNEL_TITLE, "Loading");
        values.put(RssContract.Channels.CHANNEL_LINK, url);
        Uri uri = context.getContentResolver().insert(RssContract.Channels.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        context.getContentResolver().notifyChange(RssContract.Channels.CONTENT_URI, null);

        startActionLoadOne(context, id);
    }

    public static void startActionLoadOne(Context context, long channelId) {
        Intent intent = new Intent(context, RssLoaderService.class);
        intent.setAction(ACTION_LOAD_ONE);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
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
        final String channelId = Long.toString(id);
        Cursor cursor = getContentResolver().query(
                RssContract.Channels.buildChannelUri(channelId),
                RssContract.Channels.UPDATE_COLUMNS,
                RssContract.ChannelsColumns.CHANNEL_LINK + " = ?",
                new String[]{channelId},
                null
        );
        cursor.moveToNext();
        if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return;
        }
        final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
        final String chId = Long.toString(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
        loadChannel(url, chId);
    }

    private void handleActionLoadAll() {
        Cursor cursor = getContentResolver().query(
                RssContract.Channels.CONTENT_URI,
                RssContract.Channels.UPDATE_COLUMNS,
                null, null, null
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final String url = cursor.getString(cursor.getColumnIndex(RssContract.Channels.CHANNEL_LINK));
            final String id = Long.toString(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
            loadChannel(url, id);
        }
    }

    private void loadChannel(final String surl, String channelId) {
        URL url;
        try {
            url = new URL(surl);
        } catch (MalformedURLException e) {
            getContentResolver().delete(
                    RssContract.Channels.buildChannelUri(channelId), null, null);
            return;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream is = connection.getInputStream();
            String encoding = "utf-8";
            final String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("charset=")) {
                Matcher m = Pattern.compile("charset=([^\\s]+)").matcher(contentType);
                if (m.find()) {
                    encoding = m.group(1);
                }
            }
            InputStreamReader isr = new InputStreamReader(is, encoding);
            RssChannel channel = RssParser.parse(isr);

            ContentValues values = new ContentValues();
            values.put(RssContract.ChannelsColumns.CHANNEL_TITLE, channel.getTitle());
            values.put(RssContract.ChannelsColumns.CHANNEL_LINK, channel.getUrl());

            getContentResolver().update(
                    RssContract.Channels.buildChannelUri(channelId), values, null, null);

            for (RssPost post : channel.getPosts()) {
                values = new ContentValues();
                values.put(RssContract.Posts.POST_LINK, post.getUrl());
                values.put(RssContract.Posts.POST_TITLE, post.getTitle());
                values.put(RssContract.Posts.POST_CHANNEL, channelId);


                //TODO: insert if not exist post
                //getContentResolver().insert(
                //        RssContract.Posts.buildPostsUri()
                //)
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class RssParser {
        private static final RootElement root = new RootElement("rss");
        private static final Element channel = root.getChild("channel");
        private static final Element channelUrl = channel.getChild("link");
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

            channelUrl.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curChannel.setUrl(s);
                }
            });

            channelTitle.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curChannel.setTitle(s);
                }
            });

            channelDescription.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curChannel.setDescription(s);
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
                    curPost.setUrl(s);
                }
            });

            postTitle.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setTitle(s);
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
                    curPost.setDescription(s);
                }
            });

            postGuid.setEndTextElementListener(new EndTextElementListener() {
                @Override
                public void end(String s) {
                    curPost.setGuid(s);
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

        public static RssChannel parse(InputStreamReader isr) {
            try {
                Xml.parse(isr, root.getContentHandler());
                return curChannel;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
