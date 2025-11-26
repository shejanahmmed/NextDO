package com.shejan.nextdo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CardSpinnerAdapter extends ArrayAdapter<String> {

    public CardSpinnerAdapter(@NonNull Context context, @NonNull String[] objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_spinner_dropdown, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        String item = getItem(position);
        if (item != null) {
            textView.setText(item);
        }

        return convertView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }
}
