package com.jack.bookshelf.help.media;

import android.content.Context;
import android.os.Bundle;

import androidx.loader.content.CursorLoader;

/**
 * Created by newbiechen on 2018/1/14.
 */

public class LoaderCreator {
    public static final int ALL_BOOK_FILE = 1;

    public static CursorLoader create(Context context, int id, Bundle bundle) {
        LocalFileLoader loader = null;
        if (id == ALL_BOOK_FILE) {
            loader = new LocalFileLoader(context);
        } else {
            loader = null;
        }
        if (loader != null) {
            return loader;
        }

        throw new IllegalArgumentException("The id of Loader is invalid!");
    }
}
