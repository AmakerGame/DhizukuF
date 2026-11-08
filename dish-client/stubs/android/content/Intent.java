package android.content;

import android.os.Bundle;

public class Intent {
    public Intent setComponent(ComponentName component) { return this; }

    public Intent addFlags(int flags) { return this; }

    public Intent putExtra(String name, Bundle value) { return this; }
}
