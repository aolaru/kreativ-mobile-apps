package com.kreativ.paperbuild;

import android.content.Context;

/** Review grants are separate from Google Play ownership and never acknowledge a purchase. */
final class ReviewAccess {
    private static final String PREFERENCES = "paperbuild_review";
    private static final String CODE = "signed_code";
    private ReviewAccess() { }

    static boolean active(Context context) {
        return ReviewCode.valid(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(CODE, null));
    }

    static boolean activate(Context context, String code) {
        if (!ReviewCode.valid(code)) return false;
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(CODE, code.trim()).commit();
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().remove(CODE).commit();
    }
}
