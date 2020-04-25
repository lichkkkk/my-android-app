package com.lichkkkk.myapp;

import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyAppMainActivity";

    private ExecutorService executorService;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.linkify_button).setOnClickListener(view -> onLinkify());
        findViewById(R.id.reset_button).setOnClickListener(v -> onReset());

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void onLinkify() {
        TextClassificationManager textClassificationManager = getSystemService(TextClassificationManager.class);
        if (textClassificationManager == null) {
            return;
        }
        TextClassifier textClassifier = textClassificationManager.getTextClassifier();
        TextView textView = findViewById(R.id.linkify_textview);
        // textClassifier = textView.getTextClassifier();
        // Linkify.addLinks(textView, Linkify.ALL);
        CharSequence textViewCharSequence = textView.getText();
        SpannableString text = new SpannableString(textViewCharSequence);
        List<String> entitiesToLinkify = Arrays.asList(
                TextClassifier.TYPE_DATE_TIME,
                TextClassifier.TYPE_URL);
        TextLinks.Request.Builder builder = new TextLinks.Request.Builder(text);
        builder.setEntityConfig(
                new TextClassifier.EntityConfig.Builder()
                        .setExcludedTypes(entitiesToLinkify)
                        .build());
        TextLinks.Request textLinksRequest = builder.build();

        Future<TextLinks> textLinksFuture = executorService.submit(
                () -> textClassifier.generateLinks(textLinksRequest)
        );
        try {
            TextLinks textLinks = textLinksFuture.get();
            int status = textLinks.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null);
            textView.setLinksClickable(true);
            textView.setTextIsSelectable(false);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(text);
            textView.setFocusable(true);
            textView.setFocusableInTouchMode(true);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "error generating links", e);
        }
    }

    void onReset() {
        TextView textView = findViewById(R.id.linkify_textview);
        textView.setText(R.string.linkify_test_text);
    }
}
