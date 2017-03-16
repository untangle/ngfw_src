/**
 * $Id$
 */
package com.untangle.app.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.NonceFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Generates a replacement page for Apps that block traffic.
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

    private static final Pattern IMAGE_PATTERN = Pattern.compile(".*((jpg)|(jpeg)|(gif)|(png)|(ico))", Pattern.CASE_INSENSITIVE);

    private final NonceFactory<T> nonceFactory = new NonceFactory<T>();
    private final AppSettings appId;

    // constructors -----------------------------------------------------------

    public ReplacementGenerator( AppSettings appId )
    {
        this.appId = appId;
    }

    // public methods ---------------------------------------------------------

    public String generateNonce( T o )
    {
        return nonceFactory.generateNonce(o);
    }

    public T getNonceData( String nonce )
    {
        return nonceFactory.getNonceData(nonce);
    }

    public T removeNonce( String nonce )
    {
        return nonceFactory.removeNonce(nonce);
    }

    public Token[] generateResponse( T o, AppTCPSession session )
    {
        return generateResponse(o, session, null, null );
    }

    public Token[] generateResponse( T o, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        String n = generateNonce(o);
        return generateResponse(n, session, uri, requestHeader );
    }

    public Token[] generateResponse( String nonce, AppTCPSession session )
    {
        return generateResponse(nonce, session, null, null );
    }

    public Token[] generateResponse( String nonce, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        if (imagePreferred(uri, requestHeader)) {
            return generateSimplePage(nonce, true);
        } else {
            InetAddress addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress( session.getClientIntf() );
                
            if (addr == null) {
                return generateSimplePage(nonce, false);
            } else {
                String host = addr.getHostAddress();
                int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
                if ( httpPort != 80 ) {
                    host = host + ":" + httpPort;
                }
                
                return generateRedirect( nonce, host );
            }
        }
    }

    public Token[] generateSimpleResponse( String nonce, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        return generateSimplePage(nonce, imagePreferred(uri, requestHeader));
    }
    
    // protected methods ------------------------------------------------------

    protected abstract String getReplacement( T data );
    protected abstract String getRedirectUrl( String nonce, String host, AppSettings appId );

    protected AppSettings getAppSettings()
    {
        return this.appId;
    }

    // private methods --------------------------------------------------------

    private Token[] generateSimplePage( String nonce, boolean gif )
    {
        ChunkToken chunk;
        if (gif) {
            byte[] buf = new byte[WHITE_GIF.length];
            System.arraycopy(WHITE_GIF, 0, buf, 0, buf.length);
            ByteBuffer bb = ByteBuffer.wrap(buf);
            chunk = new ChunkToken(bb);
        } else {
            String replacement = getReplacement(nonceFactory.getNonceData(nonce));
            ByteBuffer buf = ByteBuffer.allocate(replacement.length());
            buf.put(replacement.getBytes()).flip();
            chunk = new ChunkToken(buf);
        }

        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");
        response[0] = sl;

        HeaderToken h = new HeaderToken();
        h.addField("Content-Length", Integer.toString(chunk.getSize()));
        h.addField("Content-Type", gif ? "image/gif" : "text/html");
        h.addField("Connection", "Close");
        response[1] = h;

        response[2] = chunk;

        response[3] = EndMarkerToken.MARKER;

        return response;
    }

    private Token[] generateRedirect( String nonce, String host )
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 307, "Temporary Redirect");
        response[0] = sl;

        HeaderToken h = new HeaderToken();
        h.addField("Location", getRedirectUrl(nonce, host, appId));
        h.addField("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        h.addField("Pragma", "no-cache");
        h.addField("Expires", "Mon, 10 Jan 2000 00:00:00 GMT");
        h.addField("Content-Type", "text/plain");
        h.addField("Content-Length", "0");
        h.addField("Connection", "Close");
        response[1] = h;

        response[2] = ChunkToken.EMPTY;

        response[3] = EndMarkerToken.MARKER;

        return response;
    }

    private boolean imagePreferred( String uri, HeaderToken header )
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
