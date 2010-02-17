/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.virus;

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
