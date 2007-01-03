/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;
import javax.net.ssl.*;

public class VncInvoker implements SocketFactory {

    public static final String VNC_PROXY_PATH = "/proxy/forward";
    public static final String TARGET_HEADER = "Target";
    public static final String COOKIE_HEADER = "Cookie";

    SSLSocketFactory sslFactory;

    protected void initFactory() throws IOException {
        try {
            SSLContext sc = null;
            // create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

            // install the all-trusting trust manager
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslFactory = (SSLSocketFactory)sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Socket createSocket(String host, int port, java.applet.Applet applet)
      throws IOException {
      throw new IOException("Not yet supported");
    }

    public Socket createSocket(String host, int port, String[] args)
        throws IOException {
        Socket vncsock = null;
        try {
            String target_header = null;
            String cookie_header = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("-q".equals(arg)) {
                    target_header = args[++i];
                } else if ("-e".equals(arg)) {
                    cookie_header = args[++i];
                }
            }
            if (target_header == null)
                throw new IOException("No target for vnc portal");

            initFactory();

            Socket sslsock = sslFactory.createSocket();
            InetSocketAddress isa = new InetSocketAddress(host, port);
            sslsock.connect(isa, 5000);

            // vncsock.setTcpNoDelay(Options.low_latency);
            // this.in = new InputStreamReader(vncsock.getInputStream());
            OutputStream sockOut = sslsock.getOutputStream();
            InputStream sockIn = sslsock.getInputStream();
            DataInputStream din = new DataInputStream(sockIn);
            StringBuilder sb = new StringBuilder();
            sb.append("GET ").append(VNC_PROXY_PATH).append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(host).append("\r\n");
            sb.append("Transfer-Encoding: chunked").append("\r\n");
            sb.append(TARGET_HEADER).append(": ").append(target_header).append("\r\n");
            if (cookie_header != null)
                sb.append(COOKIE_HEADER).append(": ").append("$Version=0; JSESSIONIDSSO=").append(cookie_header).append("\r\n");
            sb.append("\r\n");
            sockOut.write(sb.toString().getBytes());
            sockOut.flush();
            boolean good = false;
            String statusLine = din.readLine();
            StringTokenizer st = new StringTokenizer(statusLine);
            if (st.hasMoreTokens()) {
                st.nextToken(); // HTTP/1.X
                if (st.hasMoreTokens()) {
                    String status = st.nextToken();
                    if (status.equals("200")) {
                        // Read until header done.
                        String line = din.readLine();
                        while (line != null && line.length() > 0)
                            line = din.readLine();
                        if (line != null)
                            good = true;
                    }
                }
            }
            if (!good)
                throw new IOException("Bad response from server: " + statusLine);
            vncsock = new PortaledSocket(sslsock);
        } catch (IOException x) {
            System.err.println("Unable to connect" + x.getMessage());
            throw x;
        }
        return vncsock;
    }

    // This is an ugly fucking hack.  We only override the functions that VNC will
    // actually use.  XXXX
    class PortaledSocket extends Socket {
        // The socked we're wrapping.
        Socket sock;

        PortaledSocket(Socket sock) {
            this.sock = sock;
        }

        public InputStream getInputStream()
            throws IOException {
            return new ChunkedInputStream(sock.getInputStream());
        }

        public OutputStream getOutputStream()
            throws IOException {
            return new ChunkedOutputStream(sock.getOutputStream());
        }

        public void close() throws IOException {
            sock.close();
        }
    }

    class ChunkedInputStream extends InputStream {

        /** The data receiver that we're wrapping */
        private InputStream in;

        private char lineBuffer[];

        /** The chunk size */
        private int chunkSize;

        /** The current position within the current chunk */
        private int pos;

        /** True if we'are at the beginning of stream */
        private boolean bof = true;

        /** True if we've reached the end of stream */
        private boolean eof = false;

        /** True if this stream is closed */
        private boolean closed = false;

        public ChunkedInputStream(final InputStream in) {
            super();
            if (in == null) {
                throw new IllegalArgumentException("InputStream parameter may not be null");
            }
            this.in = in;
            this.pos = 0;
        }

        public int read() throws IOException {
            if (this.closed) {
                throw new IOException("Attempted read from closed stream.");
            }
            if (this.eof) {
                return -1;
            }
            if (this.pos >= this.chunkSize) {
                nextChunk();
                if (this.eof) {
                    return -1;
                }
            }
            pos++;
            return in.read();
        }

        public int read (byte[] b, int off, int len) throws IOException {

            if (closed) {
                throw new IOException("Attempted read from closed stream.");
            }

            if (eof) {
                return -1;
            }
            if (pos >= chunkSize) {
                nextChunk();
                if (eof) {
                    return -1;
                }
            }
            len = Math.min(len, chunkSize - pos);
            int count = in.read(b, off, len);
            pos += count;
            return count;
        }

        public int read (byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        private void nextChunk() throws IOException {
            chunkSize = getChunkSize();
            if (chunkSize < 0) {
                throw new IOException("Negative chunk size");
            }
            bof = false;
            pos = 0;
            if (chunkSize == 0) {
                eof = true;
            }
        }

        private int getChunkSize() throws IOException {
            // skip CRLF
            if (!bof) {
                int cr = in.read();
                int lf = in.read();
                if ((cr != '\r') || (lf != '\n')) {
                    throw new IOException(
                                          "CRLF expected at end of chunk");
                }
            }
            //parse data
            String line = readLine();
            if (line == null) {
                throw new IOException(
                                      "Chunked stream ended unexpectedly");
            }
            int separator = line.indexOf(';');
            if (separator < 0) {
                separator = line.length();
            }
            try {
                return Integer.parseInt(line.substring(0, separator).trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Bad chunk header");
            }
        }

        private String readLine() throws IOException {
            char buf[] = lineBuffer;

            if (buf == null) {
                buf = lineBuffer = new char[128];
            }

            int room = buf.length;
            int offset = 0;
            int c;

            loop:   while (true) {
                switch (c = in.read()) {
                case -1:
                case '\n':
                    break loop;

                case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            this.in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream)in).unread(c2);
                    }
                    break loop;

                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
                }
            }
            if ((c == -1) && (offset == 0)) {
                return null;
            }
            return String.copyValueOf(buf, 0, offset);
        }

        public void close() throws IOException {
            if (!closed) {
                try {
                    if (!eof) {
                        exhaustInputStream(this);
                    }
                } finally {
                    eof = true;
                    closed = true;
                }
            }
        }

        void exhaustInputStream(final InputStream inStream) throws IOException {
            // read and discard the remainder of the message
            byte buffer[] = new byte[1024];
            while (inStream.read(buffer) >= 0) {
                ;
            }
        }
    }

    class ChunkedOutputStream extends OutputStream {

        private final OutputStream out;

        private byte[] cache;

        private int cachePosition = 0;

        private boolean wroteLastChunk = false;

        private boolean closed = false;

        public ChunkedOutputStream(final OutputStream out, int bufferSize)
            throws IOException {
            super();
            this.cache = new byte[bufferSize];
            this.out = out;
        }

        public ChunkedOutputStream(final OutputStream datatransmitter)
            throws IOException {
            this(datatransmitter, 2048);
        }

        protected void flushCache() throws IOException {
            if (this.cachePosition > 0) {
                writeBytes(Integer.toHexString(this.cachePosition));
                writeBytes("\r\n");
                this.out.write(this.cache, 0, this.cachePosition);
                writeBytes("\r\n");
                this.cachePosition = 0;
            }
        }

        protected void flushCacheWithAppend(byte bufferToAppend[], int off, int len) throws IOException {
            writeBytes(Integer.toHexString(this.cachePosition + len));
            writeBytes("\r\n");
            this.out.write(this.cache, 0, this.cachePosition);
            this.out.write(bufferToAppend, off, len);
            writeBytes("\r\n");
            this.cachePosition = 0;
        }

        protected void writeClosingChunk() throws IOException {
            // Write the final chunk.
            writeBytes("0\r\n");
            writeBytes("\r\n");
        }

        protected void writeBytes(String s) throws IOException {
            int len = s.length();
            for (int i = 0 ; i < len ; i++) {
                this.out.write((byte)s.charAt(i));
            }
        }

        public void finish() throws IOException {
            if (!this.wroteLastChunk) {
                flushCache();
                writeClosingChunk();
                this.wroteLastChunk = true;
            }
        }

        public void write(int b) throws IOException {
            if (this.closed) {
                throw new IOException("Attempted write to closed stream.");
            }
            this.cache[this.cachePosition] = (byte) b;
            this.cachePosition++;
            if (this.cachePosition == this.cache.length) flushCache();
            flush();
        }

        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte src[], int off, int len) throws IOException {
            if (this.closed) {
                throw new IOException("Attempted write to closed stream.");
            }
            if (len >= this.cache.length - this.cachePosition) {
                flushCacheWithAppend(src, off, len);
            } else {
                System.arraycopy(src, off, cache, this.cachePosition, len);
                this.cachePosition += len;
            }
            flush();
        }

        public void flush() throws IOException {
            flushCache();
            this.out.flush();
        }

        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                finish();
                this.out.flush();
            }
        }
    }
}
