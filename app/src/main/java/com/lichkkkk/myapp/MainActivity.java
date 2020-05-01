package com.lichkkkk.myapp;

import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLanguage;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import javax.annotation.ParametersAreNonnullByDefault;

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
        findViewById(R.id.detect_language_button).setOnClickListener(v -> onDetectLanguage());
        // Check https://developer.android.com/guide/topics/ui/accessibility/custom-views.html#custom-touch-events
        // And https://stackoverflow.com/questions/24952312/ontouchlistener-warning-ontouch-should-call-viewperformclick-when-a-click-is-d
        // Otherwise there will be a warning: onTouch should call View#performClick when a click is detected
        findViewById(R.id.suggest_selection_test_textview).setOnTouchListener(new View.OnTouchListener() {
            boolean downTouch = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Listening for the down and up touch events
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTouch = true;
                        onSuggestSelection(event);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (downTouch) {
                            downTouch = false;
                            v.performClick(); // Call this method to handle the response, and
                            // thereby enable accessibility services to
                            // perform this action for a user who cannot
                            // click the touchscreen.
                            return true;
                        }
                }
                return false; // Return false for other touch events
            }
        });
        findViewById(R.id.disable_textclassifier_button).setOnClickListener(v -> onDisableTextClassifier());
        findViewById(R.id.enable_textclassifier_button).setOnClickListener(v -> onEnableTextClassifier());

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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void onDetectLanguage() {
        TextView textView = findViewById(R.id.detect_language_input_textview);
        CharSequence inputText = textView.getText();
        TextLanguage.Request request = new TextLanguage.Request.Builder(inputText).build();
        TextClassifier textClassifier = textView.getTextClassifier();
        ListenableFuture<TextLanguage> textLanguageFuture = executorService.submit(
                () -> textClassifier.detectLanguage(request)
        );
        Futures.addCallback(textLanguageFuture, new FutureCallback<TextLanguage>() {
            @Override
            public void onSuccess(@ParametersAreNonnullByDefault TextLanguage result) {
                if (result.getLocaleHypothesisCount() == 0) {
                    return;
                }
                TextView resultTextView = findViewById(R.id.detect_language_result_textview);
                String detectedLang = result.getLocale(0).getDisplayLanguage();
                String output = getString(R.string.detect_language_result_text) + detectedLang;
                runOnUiThread(() -> resultTextView.setText(output));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "error detecting language", t);
            }
        }, MoreExecutors.directExecutor());
    }

    void onSuggestSelection(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            TextView textView = findViewById(R.id.suggest_selection_test_textview);
            Layout layout = textView.getLayout();
            int line = layout.getLineForVertical((int) motionEvent.getY());
            int offset = layout.getOffsetForHorizontal(line, (int) motionEvent.getX());
            Log.d(TAG, "" + line + ":" + offset + ":" + layout.getText().subSequence(offset, offset + 1));
            TextSelection.Request request = new TextSelection.Request.Builder(
                    layout.getText(), offset, offset + 1).build();
            ListenableFuture<TextSelection> textSelectionFuture = executorService.submit(
                    () -> textView.getTextClassifier().suggestSelection(request)
            );
            Futures.addCallback(textSelectionFuture, new FutureCallback<TextSelection>() {
                @Override
                public void onSuccess(@ParametersAreNonnullByDefault TextSelection result) {
                    SpannableString content = new SpannableString(
                            getString(R.string.suggest_selection_test_text));
                    content.setSpan(
                            new BackgroundColorSpan(
                                    getResources().getColor(R.color.colorPrimaryDark, null)),
                            result.getSelectionStartIndex(),
                            result.getSelectionEndIndex(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    runOnUiThread(() -> textView.setText(content));
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Log.e(TAG, "error suggesting selection", t);
                }
            }, executorService);
        }
    }

    void onDisableTextClassifier() {
        TextView textView = findViewById(R.id.disable_textclassifier_test_textview);
        textView.setTextClassifier(TextClassifier.NO_OP);
        findViewById(R.id.disable_textclassifier_button).setEnabled(false);
        findViewById(R.id.enable_textclassifier_button).setEnabled(true);
    }

    void onEnableTextClassifier() {
        TextView textView = findViewById(R.id.disable_textclassifier_test_textview);
        textView.setTextClassifier(null);
        findViewById(R.id.disable_textclassifier_button).setEnabled(true);
        findViewById(R.id.enable_textclassifier_button).setEnabled(false);
    }

    // TODO: Add suggestConversationActions
    // TODO: Add onTextClassifierEvent
}
