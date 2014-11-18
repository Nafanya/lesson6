package ru.ifmo.md.lesson6.rssreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WebViewActivity extends Activity {

    public static final String EXTRA_URL = "ru.ifmo.md.lesson6.rssreader.extra.URL";

    private WebView mWebView;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Intent intent = getIntent();
        final String url = intent.getStringExtra(EXTRA_URL);

        mDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        mDialog.setTitle("Loading");

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setWebViewClient(new WebViewClient() {
            /*
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.dismiss();
                        finish();
                    }
                }, 15000);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mDialog.dismiss();
            }
            */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        mWebView.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
