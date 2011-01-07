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

package com.untangle.node.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.util.NonceFactory;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Generates a replacement page for Nodes that block traffic.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class ReplacementGenerator<T extends BlockDetails>
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
    private final NodeId tid;

    // constructors -----------------------------------------------------------

    public ReplacementGenerator(NodeId tid)
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
        if ( "untangle-test-nonce".equalsIgnoreCase( nonce )) {
            return this.getTestData();
        }

        return nonceFactory.getNonceData(nonce);
    }

    public T removeNonce(String nonce)
    {
        return nonceFactory.removeNonce(nonce);
    }

    public Token[] generateResponse(T o, TCPSession session,
                                    boolean persistent)
    {
        return generateResponse(o, session, null, null, persistent);
    }

    public Token[] generateResponse(T o, TCPSession session,
                                    String uri, Header requestHeader,
                                    boolean persistent)
    {
        String n = generateNonce(o);
        return generateResponse(n, session, uri, requestHeader, persistent);
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
            NetworkManager nm = LocalUvmContextFactory.context().networkManager();
            InetAddress addr = nm.getInternalHttpAddress(session);
                
            if (null == addr) {
                return generateSimplePage(nonce, persistent, false);
            } else {
                String host = addr.getHostAddress();
                int port = nm.getAccessSettings().getBlockPagePort();

                if ( port != 80 ) {
                    host = host + ":" + port;
                }
                
                return generateRedirect(nonce, host, persistent);
            }
        }
    }

    public Token[] generateSimpleResponse(String nonce, TCPSession session,
            String uri, Header requestHeader, boolean persistent) {
        return generateSimplePage(nonce, persistent, imagePreferred(uri, requestHeader));
    }
    
    // protected methods ------------------------------------------------------

    protected abstract String getReplacement(T data);
    protected abstract String getRedirectUrl(String nonce, String host, NodeId tid);

    /* This returns a test nonce that can be used to test the blockpage */
    protected abstract T getTestData();
    
    protected NodeId getNodeId()
    {
        return this.tid;
    }

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
