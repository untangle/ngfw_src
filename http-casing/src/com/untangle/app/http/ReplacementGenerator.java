/**
 * $Id$
 */
package com.untangle.app.http;

import java.lang.Class;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.NonceFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Generates a replacement page for Apps that block traffic.
 */
public abstract class ReplacementGenerator<T extends RedirectDetails>
{
    private final Logger logger = Logger.getLogger(getClass());

    private static HashMap<Class<?>,Map<String,Method>> ParameterClassMethodMap = new HashMap<>();
    private static final String PARAMETER_NAME_NONCE = "nonce";
    private static final String PARAMETER_NAME_APPID = "appid";
    private static final String PARAMETER_NAME_APPTITLE = "appTitle";
    private static final String PARAMETER_PREFIX_GET = "get";

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

    private final NonceFactory<T> nonceFactory = new NonceFactory<>();
    protected final AppSettings appSettings;

    /**
     * Create a ReplacementGenerator
     * @param appSettings - the app settings for the blocking app
     */
    public ReplacementGenerator( AppSettings appSettings )
    {
        this.appSettings = appSettings;
    }

    /**
     * Generate a nonce
     * @param o - the blockdetails
     * @return the string nonce
     */
    public String generateNonce( T o )
    {
        return nonceFactory.generateNonce(o);
    }

    /**
     * Get the BlockDetails for the specified nonce
     * @param nonce
     * @return the BlockDetails
     */
    public T getNonceData( String nonce )
    {
        return nonceFactory.getNonceData(nonce);
    }

    /**
     * Remove the BlockDetails for the specified nonce
     * @param nonce
     * @return the BlockDetails
     */
    public T removeNonce( String nonce )
    {
        return nonceFactory.removeNonce(nonce);
    }

    /**
     * Generate a response (as a token array)
     * for the specified BlockDetails and session
     * @param o - the BlockDetails
     * @param session
     * @param uri
     * @param requestHeader
     * @return the token array
     */
    public Token[] generateResponse( T o, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        String n = generateNonce(o);
        return generateResponse(n, session, uri, requestHeader );
    }

    /**
     * Generate a response (as a token array)
     * for the specified nonce and session
     * @param o - Details
     * @param session
     * @return the token array
     */
    public Token[] generateResponse( T o, AppTCPSession session )
    {
        String n = generateNonce(o);
        return generateResponse(n, session, null, null );
    }

    /**
     * Generate a response (as a token array)
     * for the specified nonce and session
     * @param nonce
     * @param session
     * @return the token array
     */
    public Token[] generateResponse( String nonce, AppTCPSession session )
    {
        return generateResponse(nonce, session, null, null );
    }

    /**
     * Generate a response (as a token array)
     * for the specified nonce and session
     * @param nonce
     * @param session
     * @param uri
     * @param requestHeader
     * @return the token array
     */
    public Token[] generateResponse( String nonce, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        if (imagePreferred(uri, requestHeader)) {
            return generateSimplePage(nonce, null, true);
        } else {
            InetAddress addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress( session.getClientIntf() );
                
            if (addr == null) {
                return generateSimplePage(nonce, null, false);
            } else {
                String host = addr.getHostAddress();
                int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
                if ( httpPort != 80 ) {
                    host = host + ":" + httpPort;
                }
                
                return generateRedirect( nonce, null, host );
            }
        }
    }

    /**
     * Generate a response (as a token array)
     * for the specified nonce and session
     * @param details
     * @param session
     * @return the token array
     */
    public Token[] generateResponseFromDetails( T details, AppTCPSession session )
    {
        return generateResponseFromDetails(details, session, null, null );
    }

    /**
     * Generate a response (as a token array)
     * for the specified nonce and session
     * @param details
     * @param session
     * @param uri
     * @param requestHeader
     * @return the token array
     */
    public Token[] generateResponseFromDetails( T details, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        String host = "";
        InetAddress addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress( session.getClientIntf() );

        if (addr != null) {
            host = addr.getHostAddress();
            int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
            if ( httpPort != 80 ) {
                host = host + ":" + httpPort;
            }
        }

        return generateRedirect( null, details, host );
    }

    /**
     * Generate the "Simple" (text-only) response for the specified
     * nonce and session
     * @param nonce
     * @param session
     * @param uri
     * @param requestHeader
     * @return the token array
     */
    public Token[] generateSimpleResponse( String nonce, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        return generateSimplePage(nonce, null, imagePreferred(uri, requestHeader));
    }

    /**
     * Generate the "Simple" (text-only) response for the specified
     * nonce and session
     * @param redirectDetails
     * @param session
     * @return the token array
     */
    public Token[] generateSimpleResponse( T redirectDetails, AppTCPSession session)
    {
        return generateSimplePage(null, redirectDetails, false);
    }
    
    /**
     * getReplacement - must be overridden by final class
     * @param data
     * @return the string
     */
    protected abstract String getReplacement( T data );

    /**
     * getRedirectUrl for the specified nonce and host
     * must be overridden by final class
     * @param nonce 
     * @param host
     * @param appSettings
     * @return the URL
     */
    protected abstract String getRedirectUrl( String nonce, String host, AppSettings appSettings );

    /**
     * getRedirectUrl for the details using the redirectUrl and redirectParams
     * @param redirectDetails
     * @param host
     * @param appSettings
     * @return the URL
     */
    protected String getRedirectUrl(T redirectDetails, String host, AppSettings appSettings){
        String parameters = "";
        if(redirectDetails.getRedirectParameters() != null){
            String value = null;
            try{
                for( String key : redirectDetails.getRedirectParameters().keySet()){
                    if(redirectDetails.getRedirectParameters().get(key) != null){
                        value = redirectDetails.getRedirectParameters().get(key).toString();
                    }else{
                        if(key.equals(PARAMETER_NAME_NONCE)){
                            value = generateNonce(redirectDetails);
                        }else if(key.equals(PARAMETER_NAME_APPID)){
                            value = appSettings.getId().toString();
                        }else if(key.equals(PARAMETER_NAME_APPTITLE)){
                            value = appSettings.getAppName();
                        }else{
                            Class<?> cls = redirectDetails.getClass();
                            if(ParameterClassMethodMap.get(cls) == null){
                                ParameterClassMethodMap.put(cls, new HashMap<>());
                            }
                            Method method = ParameterClassMethodMap.get(cls).get(key);
                            if(method == null){
                                method = cls.getMethod(PARAMETER_PREFIX_GET + key.substring(0, 1).toUpperCase() + key.substring(1));
                                ParameterClassMethodMap.get(cls).put(key, method);
                            }else{
                            }
                            try{
                                value = method.invoke(redirectDetails).toString();
                            }catch(Exception ve){
                                logger.warn("Unable to retrieve value for key=" + key, ve);
                                value = "";
                            }
                        }
                    }
                    parameters += ( parameters.length() > 0 ? "&" : "" ) + URLEncoder.encode(key, StandardCharsets.UTF_8.name()) + "=" + URLEncoder.encode(value,StandardCharsets.UTF_8.name());
                }
            }catch(Exception e){
                logger.warn("Failed to encode value=" + value, e);
            }

        }
        return redirectDetails.getRedirectUrl() + (parameters.length() > 0 ? "?" + parameters: "");
    }

    /**
     * Get the AppSettings
     * @return AppSettings
     */
    protected AppSettings getAppSettings()
    {
        return this.appSettings;
    }

    /**
     * Create a "simple" (text-only) replacement page for the specified nonce and gif
     * @param nonce
     * @param redirectDetails
     * @param gif
     * @return the token array
     */
    private Token[] generateSimplePage( String nonce, T redirectDetails, boolean gif )
    {
        ChunkToken chunk;
        if (gif) {
            byte[] buf = new byte[WHITE_GIF.length];
            System.arraycopy(WHITE_GIF, 0, buf, 0, buf.length);
            ByteBuffer bb = ByteBuffer.wrap(buf);
            chunk = new ChunkToken(bb);
        } else {
            String replacement = getReplacement(nonce != null ? nonceFactory.getNonceData(nonce) : redirectDetails);
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

    /**
     * Create a redirect replacement page to redirect to a blockpage
     * @param nonce
     * @param redirectDetails
     * @param host
     * @return the token array
     */
    private Token[] generateRedirect( String nonce, T redirectDetails, String host )
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 307, "Temporary Redirect");
        response[0] = sl;

        HeaderToken h = new HeaderToken();
        h.addField("Location", nonce != null ? getRedirectUrl(nonce, host, appSettings) : getRedirectUrl(redirectDetails, host, appSettings) );
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

    /**
     * Returns true if an image is preferred in this case
     * For blocked images we just return an image so the browser gets what it expects
     * @param uri
     * @param header
     * @return true if image is preferred
     */
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
