package com.kreativ.paperbuild;

import android.net.Uri;

/** Immutable page: an export snapshot cannot change while the user edits a draft. */
final class PageItem {
    final Uri uri;
    final String name;
    final int rotation;

    PageItem(Uri uri, String name) { this(uri, name, 0); }
    PageItem(Uri uri, String name, int rotation) {
        this.uri = uri;
        this.name = name;
        this.rotation = rotation;
    }
}
