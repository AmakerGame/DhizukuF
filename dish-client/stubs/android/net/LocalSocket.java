package android.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Compile-time stub only (never packaged). */
public class LocalSocket implements Closeable {
    public void connect(LocalSocketAddress address) throws IOException {}

    public InputStream getInputStream() throws IOException { return null; }

    public OutputStream getOutputStream() throws IOException { return null; }

    public void setSoTimeout(int ms) throws IOException {}

    public Credentials getPeerCredentials() throws IOException { return null; }

    @Override
    public void close() throws IOException {}
}
