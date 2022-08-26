package com.jack.bookshelf.widget.prefs;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.jack.bookshelf.utils.theme.ThemeStore;

/**
 * Preference Category
 * Adapt to Huawei MatePad Paper
 * Edited by Jack251970
 */

@SuppressWarnings("unused")
public class ATEPreferenceCategory extends PreferenceCategory {

    public ATEPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ATEPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ATEPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ATEPreferenceCategory(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (view.isInEditMode()) {
            return;
        }
        if (view instanceof TextView) {
            // 设置title文本
            TextView tv = (TextView) view;
            tv.setTextColor(ThemeStore.accentColor(view.getContext()));
            tv.setTextSize(2,17);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }
}
