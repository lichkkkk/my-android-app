package com.lichkkkk.myapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MyAppMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.open_text_classifier_demo_btn)
                .setOnClickListener(v -> onOpenTextClassifierDemo());
        findViewById(R.id.open_book_reader_demo_btn)
                .setOnClickListener(v -> onOpenBookReaderDemo());
        findViewById(R.id.open_downloader_btn)
                .setOnClickListener(v -> onOpenDownloader());
    }

    private void onOpenBookReaderDemo() {
        Intent intent = new Intent(this, BookReaderDemoActivity.class);
        startActivity(intent);
    }

    private void onOpenTextClassifierDemo() {
        Intent intent = new Intent(this, TextClassifierDemoActivity.class);
        startActivity(intent);
    }

    private void onOpenDownloader() {
        Intent intent = new Intent(this, DownloaderActivity.class);
        startActivity(intent);
    }
}
