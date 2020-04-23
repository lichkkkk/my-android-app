package com.lichkkkk.myapp;

import android.os.Bundle;
import android.text.SpannableString;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button linkifyButton;
    private Button resetButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linkifyButton = findViewById(R.id.linkify_button);
        linkifyButton.setOnClickListener(view -> onLinkify());
        resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> onReset());
    }

    void onLinkify() {
        TextClassificationManager textClassificationManager = getSystemService(TextClassificationManager.class);
        if (textClassificationManager == null) {
            return;
        }
        TextClassifier textClassifier = textClassificationManager.getTextClassifier();
        TextView textView = findViewById(R.id.linkify_textview);
        SpannableString text = SpannableString.valueOf(textView.getText());
        TextLinks.Request textLinksRequest = new TextLinks.Request.Builder(text).build();
        // TODO: Call generateLinks() on a worker thread.
        TextLinks textLinks = textClassifier.generateLinks(textLinksRequest);
        textLinks.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null);
        textView.setText(text);
        // TODO: It seems we cannot click the links?
    }

    void onReset() {
        TextView textView = findViewById(R.id.linkify_textview);
        textView.setText(R.string.linkify_test_text);
    }
}
