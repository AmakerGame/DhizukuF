package android.os;

public interface IBinder {
    int FLAG_ONEWAY = 1;

    boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException;
}
