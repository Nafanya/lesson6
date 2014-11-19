package ru.ifmo.md.lesson6.rssreader;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Nikita Yaschenko on 16.11.14.
 */
public class RssContract {

    interface ChannelsColumns {
        String CHANNEL_LINK = "channel_link";
        String CHANNEL_TITLE = "channel_title";
    }

    interface PostsColumns {
        String POST_TITLE = "post_title";
        String POST_LINK = "post_link";
        String POST_CHANNEL = "post_channel";
        String POST_DESCRIPTION = "post_description";
    }

    public static final String CONTENT_AUTHORITY = "ru.ifmo.md.lesson6.rssreader";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_POSTS = "posts";

    public static class Channels implements ChannelsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CHANNELS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ifmorss.channel";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ifmorss.channel";

        public static final String[] ALL_COLUMNS = {
                BaseColumns._ID,
                ChannelsColumns.CHANNEL_LINK,
                ChannelsColumns.CHANNEL_TITLE,
        };

        public static final String[] UPDATE_COLUMNS = {
                BaseColumns._ID,
                ChannelsColumns.CHANNEL_LINK,
        };

        public static Uri buildChannelUri(String channelId) {
            return CONTENT_URI.buildUpon().appendPath(channelId).build();
        }

    }

    public static class Posts implements PostsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(PATH_POSTS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ifmorss.post";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ifmorss.post";

        public static final String[] ALL_COLUMNS = {
                BaseColumns._ID,
                PostsColumns.POST_LINK,
                PostsColumns.POST_TITLE,
                PostsColumns.POST_DESCRIPTION,
        };

        public static final String[] URL_COLUMNS = {
                BaseColumns._ID,
                PostsColumns.POST_LINK
        };

        public static Uri buildPostsUri(String channelId) {
            return CONTENT_URI.buildUpon().appendPath(channelId).build();
        }

        public static Uri buildPostUrlUri() {
            return CONTENT_URI.buildUpon().appendPath("id").build();
        }
    }

}
