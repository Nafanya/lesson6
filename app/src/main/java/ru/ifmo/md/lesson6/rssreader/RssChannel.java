package ru.ifmo.md.lesson6.rssreader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nikita Yaschenko on 17.11.14.
 */
public class RssChannel {
    private String mUrl;
    private String mTitle;
    private String mDescription;
    private List<RssPost> mPosts;

    public RssChannel() {
        mPosts = new ArrayList<RssPost>();
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public List<RssPost> getPosts() {
        return mPosts;
    }

    public void addPost(RssPost post) {
        mPosts.add(post);
    }

}
