package ru.ifmo.md.lesson6.rssreader;

/**
 * Created by Nikita Yaschenko on 17.11.14.
 */
public class RssPost {
    private long mChannelId;
    private String mGuid;
    private String mTitle;
    private String mUrl;
    private String mDate;
    private String mDescription;

    public RssPost() {

    }

    public long getChannelId() {
        return mChannelId;
    }

    public void setChannelId(long channelId) {
        mChannelId = channelId;
    }

    public String getGuid() {
        return mGuid;
    }

    public void setGuid(String guid) {
        mGuid = guid;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

}