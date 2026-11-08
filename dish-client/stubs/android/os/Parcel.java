package android.os;

public final class Parcel {
    public static Parcel obtain() { return null; }

    public void recycle() {}

    public void writeInt(int v) {}

    public int readInt() { return 0; }

    public void writeString(String v) {}

    public String readString() { return null; }

    public void writeStringArray(String[] v) {}

    public String[] createStringArray() { return null; }

    public IBinder readStrongBinder() { return null; }

    public void writeStrongBinder(IBinder b) {}
}
