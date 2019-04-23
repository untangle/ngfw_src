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

/**
 * Class to manage client connection to a virus scanning server
 */
public final class VirusClientSocket
{
    public final static String CLAMD_DEFHOST = "127.0.0.1"; // default host
    public final static int CLAMD_DEFPORT = 3310; // default port

    private Socket socket = null;
    private OutputStream oSocketStream;
    private InputStream iSocketStream;
    private BufferedOutputStream bufOutputStream;
    private BufferedReader bufReader;

    /**
     * Constructor
     * 
     * @param socket
     *        The network socket
     * @param oSocketStream
     *        The output stream
     * @param iSocketStream
     *        The input stream
     */
    private VirusClientSocket(Socket socket, OutputStream oSocketStream, InputStream iSocketStream)
    {
        this.socket = socket;
        this.oSocketStream = oSocketStream;
        this.iSocketStream = iSocketStream;
        bufOutputStream = new BufferedOutputStream(oSocketStream);
        bufReader = new BufferedReader(new InputStreamReader(iSocketStream));
    }

    /**
     * Create a client socket
     * 
     * @param host
     *        The server address
     * @param port
     *        The server port
     * @return The client socket
     * @throws Exception
     */
    public static VirusClientSocket create(String host, int port) throws Exception
    {
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

    /**
     * Gets the output stream
     * 
     * @return The output stream
     */
    public OutputStream getOutputStream()
    {
        return oSocketStream;
    }

    /**
     * Gets the buffered output stream
     * 
     * @return The buffered output stream
     */
    public BufferedOutputStream getBufferedOutputStream()
    {
        return bufOutputStream;
    }

    /**
     * Gets the input stream
     * 
     * @return The input stream
     */
    public InputStream getInputStream()
    {
        return iSocketStream;
    }

    /**
     * Gets the buffered input stream
     * 
     * @return The buffered input stream
     */
    public BufferedReader getBufferedReader()
    {
        return bufReader;
    }

    /**
     * Closes the client socket
     * 
     * @param host
     *        The server address
     * @param port
     *        The server port
     * @throws Exception
     */
    public void close(String host, int port) throws Exception
    {
        if (null != socket) {
            try {
                if (true == socket.isClosed()) return; // return after finally

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

    /**
     * Creates a string representation of a virus client socket
     * 
     * @return The string representation
     */
    public String toString()
    {
        if (null != socket) return socket.toString();

        return "<no socket>";
    }
}
