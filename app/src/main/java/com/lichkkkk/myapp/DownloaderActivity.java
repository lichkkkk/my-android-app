package com.lichkkkk.myapp;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DownloaderActivity extends Activity {
    public static final String TAG = "MyAppDownloaderActivity";

    private long downloadId;

    private Executor executor;

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Toast.makeText(DownloaderActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
            }
            updateFilesList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        executor = Executors.newSingleThreadExecutor();

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        findViewById(R.id.downloader_download_with_dm_button).setOnClickListener(v -> onDownloadWithDM());
        findViewById(R.id.downloader_download_with_cronet_button).setOnClickListener(v -> onDownloadWithCronet());
        updateFilesList();
    }

    private void onDownloadWithDM() {
        String url = ((EditText) findViewById(R.id.downloader_url_textview)).getText().toString();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // files will be downloaded into the public download folder
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "test.txt");
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(request);
    }

    private void onDownloadWithCronet() {
        String url = ((EditText) findViewById(R.id.downloader_url_textview)).getText().toString();
        CronetEngine.Builder myBuilder = new CronetEngine.Builder(this);
        CronetEngine cronetEngine = myBuilder.build();
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                url, new MyUrlRequestCallback(findViewById(R.id.downloader_files_textview)), executor);
        UrlRequest request = requestBuilder.build();
        request.start();

    }

    // TODO: Does not really work.
    private void updateFilesList() {
        TextView fileListTextView = findViewById(R.id.downloader_files_textview);
        String[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).list();
        fileListTextView.setText("Files Downloaded: \n" + String.join(";\n", files));
    }

    class MyUrlRequestCallback extends UrlRequest.Callback {

        private TextView textView;
        private ByteBuffer buffer;

        public MyUrlRequestCallback(TextView textView) {
            this.textView = textView;
            this.buffer = ByteBuffer.allocateDirect(102400);
        }

        @Override
        public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            Log.i(TAG, "onRedirectReceived");
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "onResponseStarted");
            Log.i(TAG, info.getHttpStatusCode() + " " + info.getHttpStatusText());
            request.read(buffer);
        }

        @Override
        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            Log.i(TAG, "onReadCompleted method called.");
            runOnUiThread(() -> {
                String downloadedText;
                try {
                    // TODO: empty cells are also read here
                    downloadedText = new String(buffer.array(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "unsupported encoding", e);
                    downloadedText = "error";
                }
                String text = textView.getText() + " | " + downloadedText;
                ;
                textView.setText(text);
            });
            byteBuffer.clear();
            // You should keep reading the request until there's no more data.
            request.read(buffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "onSucceeded method called.");
            buffer.clear();
            runOnUiThread(() -> {
                String text = textView.getText() + " | Finished!";
                textView.setText(text);
            });
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException ce) {
            Log.e(TAG, "onFailed", ce);
        }
    }
}
