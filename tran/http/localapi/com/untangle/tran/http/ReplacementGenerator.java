/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpStateMachine.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.tran.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.EndMarker;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
import com.untangle.tran.util.NonceFactory;

public abstract class ReplacementGenerator<T>
{
    private static final byte[] WHITE_GIF = new byte[]
        {
            0x47, 0x49, 0x46, 0x38,
            0x37, 0x61, 0x01, 0x00,
            0x01, 0x00, (byte)0x80, 0x00,
            0x00, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff, (byte)0xff, 0x2c,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x01, 0x00,
            0x00, 0x02, 0x02, 0x44,
            0x01, 0x00, 0x3b
        };

    private static final Pattern IMAGE_PATTERN
        = Pattern.compile(".*((jpg)|(jpeg)|(gif)|(png)|(ico))",
                          Pattern.CASE_INSENSITIVE);

    private final NonceFactory<T> nonceFactory = new NonceFactory<T>();
    private final Tid tid;

    // constructors -----------------------------------------------------------

    public ReplacementGenerator(Tid tid)
    {
        this.tid = tid;
    }

    // public methods ---------------------------------------------------------

    public String generateNonce(T o)
    {
        return nonceFactory.generateNonce(o);
    }

    public T getNonceData(String nonce)
    {
        return nonceFactory.getNonceData(nonce);
    }

    public T removeNonce(String nonce)
    {
        return nonceFactory.removeNonce(nonce);
    }

    public Token[] generateResponse(String nonce, TCPSession session,
                                    boolean persistent)
    {
        return generateResponse(nonce, session, null, null, persistent);
    }

    public Token[] generateResponse(String nonce, TCPSession session,
                                    String uri, Header requestHeader,
                                    boolean persistent)
    {
        if (imagePreferred(uri, requestHeader)) {
            return generateSimplePage(nonce, persistent, true);
        } else {
            InetAddress addr = MvvmContextFactory.context().networkManager()
                .getInternalHttpAddress(session);
            if (null == addr) {
                return generateSimplePage(nonce, persistent, false);
            } else {
                String host = addr.getHostAddress();
                return generateRedirect(nonce, host, persistent);
            }
        }
    }

    // protected methods ------------------------------------------------------

    protected abstract String getReplacement(T data);
    protected abstract String getRedirectUrl(String nonce, String host, Tid tid);

    // private methods --------------------------------------------------------

    private Token[] generateSimplePage(String nonce, boolean persistent,
                                       boolean gif)
    {

        Chunk chunk;
        if (gif) {
            byte[] buf = new byte[WHITE_GIF.length];
            System.arraycopy(WHITE_GIF, 0, buf, 0, buf.length);
            ByteBuffer bb = ByteBuffer.wrap(buf);
            chunk = new Chunk(bb);
        } else {
            String replacement = getReplacement(nonceFactory.getNonceData(nonce));
            ByteBuffer buf = ByteBuffer.allocate(replacement.length());
            buf.put(replacement.getBytes()).flip();
            chunk = new Chunk(buf);
        }

        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");
        response[0] = sl;

        Header h = new Header();
        h.addField("Content-Length", Integer.toString(chunk.getSize()));
        h.addField("Content-Type", gif ? "image/gif" : "text/html");
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response[1] = h;

        response[2] = chunk;

        response[3] = EndMarker.MARKER;

        return response;
    }

    private Token[] generateRedirect(String nonce, String host,
                                     boolean persistent)
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 307, "Temporary Redirect");
        response[0] = sl;

        Header h = new Header();
        h.addField("Location", getRedirectUrl(nonce, host, tid));
        h.addField("Content-Type", "text/plain");
        h.addField("Content-Length", "0");
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response[1] = h;

        response[2] = Chunk.EMPTY;

        response[3] = EndMarker.MARKER;

        return response;
    }

    private boolean imagePreferred(String uri, Header header)
    {
        if (null != uri && IMAGE_PATTERN.matcher(uri).matches()) {
            return true;
        } else if (null != header) {
            String accept = header.getValue("accept");

            // firefox uses "image/png, */*;q=0.5" when expecting an image
            // ie uses "*/*" no matter what it expects
            return null != accept && accept.startsWith("image/png");
        } else {
            return false;
        }
    }
}
