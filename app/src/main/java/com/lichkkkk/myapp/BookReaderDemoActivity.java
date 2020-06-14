package com.lichkkkk.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BookReaderDemoActivity extends Activity {
    private static final int CHAR_PER_PAGE = 500;

    private String content = "";
    private int currStartPos = 0;
    private int currEndPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_reader_demo);

        content = getResources().getString(R.string.lovely_ilonka_story);
        findViewById(R.id.next_page_btn).setOnClickListener(v -> onNextPage());
        findViewById(R.id.last_page_btn).setOnClickListener(v -> onLastPage());
    }

    private void onNextPage() {
        if (currEndPos == content.length()) {
            return;
        }
        currStartPos = currEndPos;
        currEndPos = Math.min(currStartPos + CHAR_PER_PAGE, content.length());
        displayText(currStartPos, currEndPos);
    }

    private void onLastPage() {
        if (currStartPos == 0) {
            return;
        }
        currStartPos = Math.max(0, currStartPos - CHAR_PER_PAGE);
        currEndPos = Math.min(currStartPos + CHAR_PER_PAGE, content.length());
        displayText(currStartPos, currEndPos);
    }

    private void displayText(int startPos, int endPos) {
        if (startPos >= endPos || endPos > content.length() || startPos < 0) {
            return;
        }
        TextView textView = findViewById(R.id.book_reader_text_view);
        String textToDisplay = content.substring(startPos, endPos);
        textView.setText(textToDisplay);
    }
}
