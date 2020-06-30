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
import android.widget.EditText;
import android.widget.Toast;

public class DownloaderActivity extends Activity {

    private long downloadId;

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Toast.makeText(DownloaderActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        findViewById(R.id.downloader_download_button).setOnClickListener(v -> onDownload());
    }

    private void onDownload() {
        String url = ((EditText) findViewById(R.id.downloader_url_textview)).getText().toString();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // files will be downloaded into the public download folder
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "test.txt");
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(request);
    }
}
