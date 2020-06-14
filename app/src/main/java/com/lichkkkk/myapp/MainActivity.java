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

        findViewById(R.id.open_text_classifier_demo_btn).setOnClickListener(v -> onOpenTextClassifierDemo());
    }

    private void onOpenTextClassifierDemo() {
        Intent intent = new Intent(this, TextClassifierDemoActivity.class);
        startActivity(intent);
    }
}
