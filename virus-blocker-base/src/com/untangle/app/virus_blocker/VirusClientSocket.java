/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public final class VirusClientSocket {
    public final static String CLAMD_DEFHOST = "127.0.0.1"; // default host
    public final static int CLAMD_DEFPORT = 3310; // default port

    private Socket socket = null;
    private OutputStream oSocketStream;
    private InputStream iSocketStream;
    private BufferedOutputStream bufOutputStream;
    private BufferedReader bufReader;

    private VirusClientSocket(Socket socket, OutputStream oSocketStream, InputStream iSocketStream) {
        this.socket = socket;
        this.oSocketStream = oSocketStream;
        this.iSocketStream = iSocketStream;
        bufOutputStream = new BufferedOutputStream(oSocketStream);
        bufReader = new BufferedReader(new InputStreamReader(iSocketStream));
    }

    public static VirusClientSocket create(String host, int port) throws Exception {
        try {
            Socket socket = new Socket(host, port);
            OutputStream oSocketStream = socket.getOutputStream();
            InputStream iSocketStream = socket.getInputStream();

            VirusClientSocket vSocket = new VirusClientSocket(socket, oSocketStream, iSocketStream);
            return vSocket;
        } catch (Exception e) {
            throw new Exception("Could not connect to socket: " + host + "/" + port, e);
        }
    }

    // for writes to socket (lowest level)
    public OutputStream getOutputStream() {
        return oSocketStream;
    }

    // for writes to socket
    public BufferedOutputStream getBufferedOutputStream() {
        return bufOutputStream;
    }

    // for reads from socket (lowest level)
    public InputStream getInputStream() {
        return iSocketStream;
    }

    // for reads from socket
    public BufferedReader getBufferedReader() {
        return bufReader;
    }

    public void close(String host, int port) throws Exception {
        if (null != socket) {
            try {
                if (true == socket.isClosed())
                    return; // return after finally

                // we can't close streams attached to socket
                // until we are ready to close socket
                bufOutputStream.close();
                bufReader.close();
                socket.shutdownOutput();
                socket.shutdownInput();
                socket.close();
            } catch (Exception e) {
                throw new Exception("Could not close socket: " + host + ":" + port, e);
            } finally {
                bufOutputStream = null;
                bufReader = null;
                oSocketStream = null;
                iSocketStream = null;
                socket = null;
            }
        }

        return;
    }

    public String toString() {
        if (null != socket)
            return socket.toString();

        return "<no socket>";
    }
}
