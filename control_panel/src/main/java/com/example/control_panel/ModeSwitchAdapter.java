package com.example.control_panel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ModeSwitchAdapter {

    private List<String> mItems;
    private Context mContext;
    public String mCurrentModeName;

    public ModeSwitchAdapter(Context context, List<String> items) {
        this.mContext = context;
        this.mItems = items;
    }

    public int getCount() {
        if (this.mItems == null) {
            return 0;
        }
        return mItems.size();
    }

    public View getPositionView(int position, ViewGroup parent, LayoutInflater inflater) {
        TextView inflate = (TextView) inflater
                .inflate(R.layout.layout_mode_switch_item, parent, false);
        TextView textView = inflate.findViewById(R.id.tv_text);
        textView.setText(mItems.get(position));
        return inflate;
    }

    public void initView(View view) {
        TextView textView = view.findViewById(R.id.tv_text);
        textView.setTextColor(mContext.getResources()
                .getColor(R.color.mode_switch_pre_text, null));
    }

    public void selectView(View view) {
        TextView textView = view.findViewById(R.id.tv_text);
        textView.setTextColor(mContext.getResources()
                .getColor(R.color.white, null));
        mCurrentModeName = textView.getText().toString();
    }

    public void preView(View view) {
        TextView textView = view.findViewById(R.id.tv_text);
        textView.setTextColor(mContext.getResources()
                .getColor(R.color.mode_switch_pre_text, null));
    }
}
