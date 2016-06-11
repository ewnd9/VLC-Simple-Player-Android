package com.wass08.vlcsimpleplayer.translator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.wass08.vlcsimpleplayer.R;

/**
 * Created by ewnd9 on 11.06.16.
 */
public class TranslationArrayAdapter extends ArrayAdapter<TranslationResult.Translation> {

    private LayoutInflater inflater;

    public TranslationArrayAdapter(Context ctx) {
        super(ctx, 0);
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TranslationResult.Translation item = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_translation, parent, false);
            holder = new ViewHolder(convertView);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(item.text);
        return convertView;
    }


    private class ViewHolder {
        TextView text;

        public ViewHolder(View view) {
            text = (TextView) view.findViewById(R.id.textView);
            view.setTag(this);
        }
    }
}
