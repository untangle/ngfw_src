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

package com.untangle.tran.portal.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

public class TrustingSslSocketFactory implements SecureProtocolSocketFactory {
    private final SSLContext sslCtx;

    TrustingSslSocketFactory()
    {
        try {
            sslCtx = SSLContext.getInstance("SSL");
            sslCtx.init(null, new TrustManager[] { new TrustingX509TrustManager() },
                        null);
        } catch (NoSuchAlgorithmException exn) {
            throw new RuntimeException(exn);
        } catch (KeyManagementException exn) {
            throw new RuntimeException(exn);
        }
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
        throws IOException, UnknownHostException
    {
        return sslCtx.getSocketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
                               HttpConnectionParams params)
        throws IOException, UnknownHostException
    {
        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = sslCtx.getSocketFactory();

        if (0 ==  timeout) {
            return socketfactory.createSocket(host, port, localAddress, localPort);
        } else {
            Socket socket = socketfactory.createSocket();
            SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
            SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            return socket;
        }
    }

    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return sslCtx.getSocketFactory().createSocket(host, port);
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
        throws IOException, UnknownHostException
    {
        return sslCtx.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    public boolean equals(Object obj)
    {
        return ((null != obj) && obj.getClass().equals(getClass()));
    }

    public int hashCode()
    {
        return getClass().hashCode();
    }
}