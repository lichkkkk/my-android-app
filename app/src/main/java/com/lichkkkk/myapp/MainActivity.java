package com.lichkkkk.myapp;

import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyAppMainActivity";

    private ListeningExecutorService executorService;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.linkify_button).setOnClickListener(view -> onLinkify());
        findViewById(R.id.linkify_reset_button).setOnClickListener(v -> onLinkifyReset());
        findViewById(R.id.classify_button).setOnClickListener(v -> onClassify());
        findViewById(R.id.classify_reset_button).setOnClickListener(v -> onClassifyReset());

        executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void onLinkify() {
        TextClassificationManager textClassificationManager =
                getSystemService(TextClassificationManager.class);
        if (textClassificationManager == null) {
            return;
        }
        TextClassifier textClassifier = textClassificationManager.getTextClassifier();
        TextView textView = findViewById(R.id.linkify_textview);
        // textClassifier = textView.getTextClassifier();
        // Linkify.addLinks(textView, Linkify.ALL);
        CharSequence textViewCharSequence = textView.getText();
        SpannableString text = new SpannableString(textViewCharSequence);
        TextLinks.Request textLinksRequest = new TextLinks.Request.Builder(text).build();
        ListenableFuture<TextLinks> textLinksFuture = executorService.submit(
                () -> textClassifier.generateLinks(textLinksRequest)
        );
        textLinksFuture.addListener(() -> runOnUiThread(() -> {
            try {
                TextLinks textLinks = textLinksFuture.get();
                textLinks.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setText(text);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "error generating links", e);
            }
        }), MoreExecutors.directExecutor());
    }

    void onLinkifyReset() {
        TextView textView = findViewById(R.id.linkify_textview);
        textView.setText(R.string.linkify_test_text);
    }

    void onClassify() {
        TextView inputTextView = findViewById(R.id.classify_input_textview);
        TextClassifier textClassifier = inputTextView.getTextClassifier();
        CharSequence inputText = inputTextView.getText();
        TextClassification.Request request = new TextClassification.Request.Builder(
                inputText, 0, inputText.length() - 1
        ).build();
        ListenableFuture<TextClassification> textClassificationFuture = executorService.submit(
                () -> textClassifier.classifyText(request)
        );
        textClassificationFuture.addListener(() -> runOnUiThread(() -> {
            try {
                TextClassification textClassification = textClassificationFuture.get();
                if (textClassification.getEntityCount() == 0) {
                    return;
                }
                Button classifiedActionButton = findViewById(R.id.classify_action_button);
                classifiedActionButton.setText(textClassification.getEntity(0));
                classifiedActionButton.setOnClickListener(v -> {
                    try {
                        textClassification.getActions().get(0).getActionIntent().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "error sending intent");
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "error classifying text", e);
            }
        }), MoreExecutors.directExecutor());
    }

    void onClassifyReset() {
        Button classifiedActionButton = findViewById(R.id.classify_action_button);
        classifiedActionButton.setText(R.string.classify_action_button_default_name);
        classifiedActionButton.setOnClickListener(v -> {
        });
    }
}
