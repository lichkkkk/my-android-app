package com.lichkkkk.myapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Person;
import android.app.RemoteAction;
import android.content.Context;
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
import android.view.textclassifier.ConversationAction;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextClassifierEvent;
import android.view.textclassifier.TextLanguage;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.ParametersAreNonnullByDefault;

public class TextClassifierDemoActivity extends Activity {
    public static final String TAG = MainActivity.TAG;

    private ListeningExecutorService executorService;

    private static void asyncLinkify(Context context, TextView textView, Executor bgExecutor) {
        TextClassificationManager tcm = context.getSystemService(TextClassificationManager.class);
        if (tcm == null) return;
        TextClassifier textClassifier = tcm.getTextClassifier();

        CharSequence textToLinkify = textView.getText();
        if (textToLinkify.length() > textClassifier.getMaxGenerateLinksTextLength()) {
            return;
        }
        TextLinks.Request request = new TextLinks.Request.Builder(textToLinkify).build();
        Reference<TextView> textViewRef = new WeakReference<>(textView);
        bgExecutor.execute(() -> {
            TextView textView1 = textViewRef.get();
            if (textView1 == null) return;
            TextLinks textLinks = textClassifier.generateLinks(request);
            SpannableString text = new SpannableString(textView1.getText());
            textLinks.apply(text, TextLinks.APPLY_STRATEGY_IGNORE, null);
            textView1.post(() -> {
                TextView textView2 = textViewRef.get();
                if (textView2 == null) return;
                textView2.setMovementMethod(LinkMovementMethod.getInstance());
                textView2.setText(text);
            });
        });
    }

    private static void asyncClassify(Context context, Button button, CharSequence text, Executor bgExecutor) {
        TextClassificationManager tcm = context.getSystemService(TextClassificationManager.class);
        if (tcm == null) return;
        TextClassifier textClassifier = tcm.getTextClassifier();

        TextClassification.Request request = new TextClassification.Request.Builder(text, 0, text.length() - 1).build();
        Reference<Button> buttonRef = new WeakReference<>(button);
        bgExecutor.execute(() -> {
            Button button1 = buttonRef.get();
            if (button1 == null) return;
            TextClassification textClassification = textClassifier.classifyText(request);
            if (textClassification.getEntityCount() == 0 || textClassification.getActions().size() == 0) {
                return;
            }
            button1.post(() -> {
                Button button2 = buttonRef.get();
                if (button2 == null) return;
                button2.setText(textClassification.getEntity(0));
                button2.setOnClickListener(v -> {
                    try {
                        textClassification.getActions().get(0).getActionIntent().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "error sending intent");
                    }
                });
            });
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_classifier_demo);

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
        findViewById(R.id.enable_default_textclassifier_button).setOnClickListener(v -> onEnableDefaultTextClassifier());
        findViewById(R.id.enable_custom_textclassifier_button).setOnClickListener(v -> onEnableCustomTextClassifier());
        findViewById(R.id.suggest_conversation_action_button).setOnClickListener(v -> onSuggestConversationAction());

        executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    void onLinkifyReset() {
        TextView textView = findViewById(R.id.linkify_textview);
        textView.setText(R.string.linkify_test_text);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void onLinkify() {
        asyncLinkify(getApplicationContext(), findViewById(R.id.linkify_textview), executorService);
    }

    void onClassify() {
        TextView inputTextView = findViewById(R.id.classify_input_textview);
        Button classifiedActionButton = findViewById(R.id.classify_action_button);
        asyncClassify(getApplicationContext(), classifiedActionButton, inputTextView.getText(), executorService);
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
        findViewById(R.id.enable_default_textclassifier_button).setEnabled(true);
        findViewById(R.id.enable_custom_textclassifier_button).setEnabled(true);
    }

    void onEnableDefaultTextClassifier() {
        TextView textView = findViewById(R.id.disable_textclassifier_test_textview);
        textView.setTextClassifier(null);
        findViewById(R.id.disable_textclassifier_button).setEnabled(true);
        findViewById(R.id.enable_default_textclassifier_button).setEnabled(false);
        findViewById(R.id.enable_custom_textclassifier_button).setEnabled(true);
    }

    void onEnableCustomTextClassifier() {
        TextClassifier customTextClassifier = new TextClassifier() {

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @NonNull
            @Override
            public android.view.textclassifier.TextSelection suggestSelection(
                    @NonNull android.view.textclassifier.TextSelection.Request request) {
                TextClassifierEvent.TextSelectionEvent event =
                        new TextClassifierEvent.TextSelectionEvent.Builder(
                                TextClassifierEvent.TYPE_SELECTION_STARTED).build();
                onTextClassifierEvent(event);
                return TextClassifier.super.suggestSelection(request);
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onTextClassifierEvent(@NonNull TextClassifierEvent event) {
                Log.d(TAG, "onTextClassifierEvent");
                runOnUiThread(() -> Toast.makeText(
                        getApplicationContext(), "event type: " + event.getEventType(), Toast.LENGTH_SHORT).show());
            }
        };
        TextView textView = findViewById(R.id.disable_textclassifier_test_textview);
        textView.setTextClassifier(customTextClassifier);
        findViewById(R.id.disable_textclassifier_button).setEnabled(true);
        findViewById(R.id.enable_default_textclassifier_button).setEnabled(true);
        findViewById(R.id.enable_custom_textclassifier_button).setEnabled(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void onSuggestConversationAction() {
        TextView textView = findViewById(R.id.suggest_conversation_action_input_textview);
        ConversationActions.Message message =
                new ConversationActions.Message
                        .Builder(new Person.Builder().build())
                        .setText(textView.getText())
                        .build();
        ConversationActions.Request request =
                new ConversationActions.Request
                        .Builder(ImmutableList.of(message))
                        .build();

        TextClassificationManager textClassificationManager =
                getSystemService(TextClassificationManager.class);
        if (textClassificationManager == null) {
            return;
        }
        TextClassifier textClassifier = textClassificationManager.getTextClassifier();
        ListenableFuture<ConversationActions> conversationActionsFuture =
                executorService.submit(() -> textClassifier.suggestConversationActions(request));
        Futures.addCallback(conversationActionsFuture, new FutureCallback<ConversationActions>() {
            @Override
            public void onSuccess(@ParametersAreNonnullByDefault ConversationActions result) {
                runOnUiThread(() -> {
                    List<ConversationAction> actions = result.getConversationActions();
                    if (actions.size() == 0) {
                        Log.e(TAG, "no cov action");
                        return;
                    }
                    RemoteAction remoteAction = actions.get(0).getAction();
                    if (remoteAction == null) {
                        Log.e(TAG, "null remote action");
                        return;
                    }
                    Button button = findViewById(R.id.suggested_conversation_action_button);
                    button.setText(remoteAction.getTitle());
                    Toast.makeText(getApplicationContext(), remoteAction.getTitle(), Toast.LENGTH_SHORT).show();
                    button.setOnClickListener(v -> {
                        try {
                            remoteAction.getActionIntent().send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, "error sending intent");
                        }
                    });
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "error suggesting conversation action", t);
            }
        }, MoreExecutors.directExecutor());
    }
}
