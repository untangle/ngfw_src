/*
 * Copyright (c) 2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: $
 */

package com.untangle.tran.spamassassin;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public final class SpamAssassinClientSocket {
    public final static String SPAMD_DEFHOST = "127.0.0.1"; // default host
    public final static int SPAMD_DEFPORT = 783; // default port

    private Socket socket = null;
    private OutputStream oSocketStream;
    private InputStream iSocketStream;
    private BufferedOutputStream bufOutputStream;
    private BufferedReader bufReader;

    private SpamAssassinClientSocket(Socket socket, OutputStream oSocketStream, InputStream iSocketStream) {
        this.socket = socket;
        this.oSocketStream = oSocketStream;
        this.iSocketStream = iSocketStream;
        bufOutputStream = new BufferedOutputStream(oSocketStream);
        bufReader = new BufferedReader(new InputStreamReader(iSocketStream));
    }

    public static SpamAssassinClientSocket create(String host, int port) throws Exception {
        try {
            Socket socket = new Socket(host, port);
            OutputStream oSocketStream = socket.getOutputStream();
            InputStream iSocketStream = socket.getInputStream();

            SpamAssassinClientSocket sSocket = new SpamAssassinClientSocket(socket, oSocketStream, iSocketStream);
            return sSocket;
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
