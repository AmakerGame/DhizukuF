package com.EdS.DhizukuF.hub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Takes the place of the original "request permission" screen. The request, which carries the
 * client's uid and its listener Binder, is handed to DhizukuF's own dialog and this activity closes
 * at once, so the user sees the normal DhizukuF dialog.
 */
public class HubRequestActivity extends Activity {
    private static final String TAG = "DhizukuHub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = getIntent();
        if (in != null && Target.trusted(this)) {
            Intent out = new Intent(Target.PACKAGE + ".action.request.permission");
            out.setComponent(new ComponentName(Target.PACKAGE, Target.REQUEST_ACTIVITY));
            if (in.getExtras() != null) out.putExtras(in.getExtras());
            // if the client waits for a result, DhizukuF answers it directly
            out.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            try {
                startActivity(out);
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(TAG, "cannot open the DhizukuF dialog", e);
                setResult(RESULT_CANCELED);
            }
        } else {
            Log.w(TAG, "DhizukuF is missing or is signed with another key, request refused");
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
