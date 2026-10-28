package com.EdS.DhizukuF.hub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Takes the place of the original Dhizuku provider. A call (method, argument and extras, which carry
 * the client's Binder) is handed to DhizukuF unchanged and DhizukuF's answer, which carries the
 * service Binder, is returned unchanged. The client then talks to DhizukuF's service directly, so
 * DhizukuF still sees the real calling uid of every request.
 */
public class HubProvider extends ContentProvider {
    private static final String TAG = "DhizukuHub";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (getContext() == null || !Target.trusted(getContext())) {
            Log.w(TAG, "DhizukuF is missing or is signed with another key, call refused");
            return null;
        }
        for (String authority : Target.AUTHORITIES) {
            try {
                Bundle result = getContext().getContentResolver()
                        .call(Uri.parse("content://" + authority), method, arg, extras);
                if (result != null) return result;
            } catch (RuntimeException e) {
                Log.w(TAG, "forwarding to " + authority + " failed", e);
            }
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
