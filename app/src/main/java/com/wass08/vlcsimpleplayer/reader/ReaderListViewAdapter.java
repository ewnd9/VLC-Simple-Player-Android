package com.wass08.vlcsimpleplayer.reader;

import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.wass08.vlcsimpleplayer.R;
import com.wass08.vlcsimpleplayer.vendor.selectabletextview.SelectableTextView;

/**
 * Created by ewnd9 on 21.07.16.
 */
public class ReaderListViewAdapter extends ArrayAdapter<ReaderListViewItem> {

    private static final String TAG = "ReactNative";

    private LayoutInflater inflater;
    private WindowManager manager;

    private Callback onRequestFullText;
    private Callback onSendSelection;

    public ReaderListViewAdapter(Context context, WindowManager manager, Callback onRequestFullText, Callback onSendSelection) {
        super(context, 0);

        this.inflater = LayoutInflater.from(context);
        this.manager = manager;

        this.onRequestFullText = onRequestFullText;
        this.onSendSelection = onSendSelection;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderListViewItem item = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_test, parent, false);
            holder = new ViewHolder(convertView);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Log.v(TAG, "getView " + position + " " + (item.data == null));

        if (item.data == null) {
            holder.name.setText("placeholder (" + item.text + ")");

            holder.name.setVisibility(View.VISIBLE);
            holder.fullText.setVisibility(View.GONE);
            holder.layout.setVisibility(View.GONE);

            onRequestFullText.invoke(position);

            for (int i = position + 1; (i < this.getCount()) && (i < position + 3); i++) {
                onRequestFullText.invoke(i);
            }
        } else {
            holder.layout.removeAllViews();
            populateLinks(holder.layout, item.data.getMap("text"));
//            holder.fullText.setText(item.data.getString("text"));

            holder.layout.setVisibility(View.VISIBLE);
            holder.name.setVisibility(View.GONE);
            holder.fullText.setVisibility(View.GONE);
        }

        holder.position = position;
        Log.v(TAG, "rendering " + position);

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    // do not substitute with plain text, objects
//    private String getSentences(ReadableMap map) {
//        String result = "";
//        ReadableArray sentences = map.getArray("sentences");
//
//        for (int i = 0 ; i < sentences.size() ; i++) {
//            ReadableMap sentence = sentences.getMap(i);
//            ReadableArray terms = sentence.getArray("terms");
//
//            for (int j = 0 ; j < terms.size() ; j++) {
//                ReadableMap term = terms.getMap(j);
//                result += term.getString("preceding") + term.getString("fullText") + term.getString("trailing");
//            }
//        }
//
//        return map.getString("preceding") + result + map.getString("trailing");
//    }

//    private String getText(ReadableMap map) {
//        String result = "";
//
//        if (map.hasKey("children")) {
//            ReadableArray xs = map.getArray("children");
//            result = "";
//
//            for (int i = 0 ; i < xs.size() ; i++) {
//                result = result + getText(xs.getMap(i));
//            }
//        }
//
//        if (map.hasKey("text")) {
//            result = map.getString("text");
//        }
//
//        String delimiter = map.hasKey("isBlockElement") && map.getBoolean("isBlockElement") ? "\n\n\t" : " ";
//        return result + delimiter;
//    }

    private void populateLinks(LinearLayout ll, ReadableMap text) {
        Display display = manager.getDefaultDisplay();
        int maxWidth = display.getWidth() - 10;

        LinearLayout llAlso = new LinearLayout(getContext());
        llAlso.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        llAlso.setOrientation(LinearLayout.HORIZONTAL);

        int widthSoFar = 0;

        ReadableArray paragraphs = text.getArray("paragraphs");
        for (int k = 0 ; k < paragraphs.size() ; k++) {
            ReadableMap paragraph = paragraphs.getMap(k);
            ReadableArray sentences = paragraph.getArray("sentences");

            for (int j = 0 ; j < sentences.size() ; j++) {
                ReadableMap sentence = sentences.getMap(j);
                ReadableArray terms = sentence.getArray("terms");

                for (int i = 0 ; i < terms.size() ; i++) {
                    ReadableMap term = terms.getMap(i);

                    TextView txtSamItem = new TextView(getContext());
                    txtSamItem.setText(term.getString("fullText"));

                    txtSamItem.setPadding(10, 0, 0, 0);
                    txtSamItem.setTag(term);

                    txtSamItem.measure(0, 0);
                    widthSoFar += txtSamItem.getMeasuredWidth();

                    if (widthSoFar >= maxWidth) {
                        ll.addView(llAlso);

                        llAlso = new LinearLayout(getContext());
                        llAlso.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.FILL_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT)
                        );

                        llAlso.setOrientation(LinearLayout.HORIZONTAL);
                        llAlso.addView(txtSamItem);

                        widthSoFar = txtSamItem.getMeasuredWidth();
                    } else {
                        llAlso.addView(txtSamItem);
                    }
                }
            }
        }

        ll.addView(llAlso);
    }


    public class ViewHolder {

        LinearLayout layout;
        TextView name;
        SelectableTextView fullText;

        int position;

        public ViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.textView);
            fullText = (SelectableTextView) view.findViewById(R.id.fullTextView);
            fullText.setCustomSelectionActionModeCallback(
                    new ReaderTextViewCustomSelection(this, onSendSelection)
            );
            layout = (LinearLayout) view.findViewById(R.id.layout);

            view.setTag(this);
        }
    }
}
