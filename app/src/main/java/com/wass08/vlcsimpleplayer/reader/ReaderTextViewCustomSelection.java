package com.wass08.vlcsimpleplayer.reader;

import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.react.bridge.Callback;

/**
 * Created by ewnd9 on 21.07.16.
 */
// https://github.com/SmallSteps/CodeViewer/blob/9c1b91b2e7999ad7eaed53ff6c35bcec9dbfe621/AndroidViewer/app/src/main/java/com/ozzzzz/bogdan/androidviewer/utils/textselection/MarkTextSelectionActionModeCallback.java
public class ReaderTextViewCustomSelection implements ActionMode.Callback {

    private static final String TAG = "ReactNative";
    private final static int DEFINITION = 1;

    private ReaderListViewAdapter.ViewHolder holder;
    private TextView fullText;
    private Callback onSendSelection;

    public ReaderTextViewCustomSelection(ReaderListViewAdapter.ViewHolder holder, Callback onSendSelection) {
        this.holder = holder;
        this.fullText = holder.fullText;

        this.onSendSelection = onSendSelection;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        menu.add(0, DEFINITION, 0, "GETTEXT");
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case DEFINITION:
                int min = 0;
                int max = fullText.getText().length();

                if (fullText.isFocused()) {
                    final int selStart = fullText.getSelectionStart();
                    final int selEnd = fullText.getSelectionEnd();

                    min = Math.max(0, Math.min(selStart, selEnd));
                    max = Math.max(0, Math.max(selStart, selEnd));
                }

                // Perform your definition lookup with the selected text

                String text = fullText.getText().toString();
                final CharSequence selectedText = text.subSequence(min, max);

                Log.v(TAG, "selection \"" + selectedText.toString() + "\"");
                Log.v(TAG, "selection text length " + fullText.getText().length());

                onSendSelection.invoke(holder.position, min, max, fullText.getText().toString());

                mode.finish();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
}
