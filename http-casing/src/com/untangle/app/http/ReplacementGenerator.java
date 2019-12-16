/**
 * $Id$
 */
package com.untangle.app.http;

import java.lang.Class;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.NonceFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.vnet.AppTCPSession;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

/**
 * Generates a replacement page for Apps that block traffic.
 */
public abstract class ReplacementGenerator<T extends RedirectDetails>
{
    private final Logger logger = Logger.getLogger(getClass());

    private static HashMap<Class<?>,Map<String,Method>> ParameterClassMethodMap = new HashMap<>();

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

    protected URIBuilder redirectUri;
    protected Map<String,Object> redirectParameters;

    private final NonceFactory<T> nonceFactory = new NonceFactory<>();
    protected final AppSettings appSettings;

    /**
     * Create a ReplacementGenerator
     * @param appSettings - the app settings for the blocking app
     */
    public ReplacementGenerator( AppSettings appSettings)
    {
        this.appSettings = appSettings;

        try{
            redirectUri = new URIBuilder();
        }catch(Exception e){}
        redirectParameters = new HashMap<String,Object>();
        redirectParameters.put("nonce", null);
        redirectParameters.put("appid", null);
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
     * Generate response from details and TCP session.
     * @param  details ReplacementDetails containing data to pull.
     * @param  session AppTCPSesson
     * @return         Token array of response to send back to client.
     */
    public Token[] generateResponse(T details, AppTCPSession session)
    {
        return generateResponse(details, session, null, null, null, null);
    }

    /**
     * Generate response from details and TCP session.
     * @param  details ReplacementDetails containing data to pull.
     * @param  session AppTCPSesson
     * @param  uri           String of uri to reach.
     * @param  requestHeader HeaderToken containing reuquest
     * @return         Token array of response to send back to client.
     */
    public Token[] generateResponse(T details, AppTCPSession session, String uri, HeaderToken requestHeader)
    {
        return generateResponse(details, session, uri, requestHeader, null, null);
    }

    /**
     * Generate response from details and TCP session and overriding redirectUri and redirectParameters.
     * @param  details ReplacementDetails containing data to pull.
     * @param  session AppTCPSesson
     * @param  uri           String of uri to reach.
     * @param  requestHeader HeaderToken containing reuquest.
     * @param  redirectUri        URIBuilder of redirect uri to use.  If null, use redirectUri from this generator.
     * @param  redirectParameters Map of redirectparameters to use.  If null, use redirectParameters from this generator.
     * @return         Token array of response to send back to client.
     */
    public Token[] generateResponse(T details, AppTCPSession session, String uri, HeaderToken requestHeader, URIBuilder redirectUri, Map<String,Object> redirectParameters)
    {
        if (imagePreferred(uri, requestHeader)) {
            return generateSimplePage(details, true);
        } else {
            if(redirectUri == null){
                redirectUri = getRedirectUri();
            }
            if(redirectParameters == null){
                redirectParameters = getRedirectParameters();
            }
            InetAddress addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress( session.getClientIntf() );

            if (addr == null) {
                return generateSimplePage(details, false);
            } else {
                if(redirectUri.getScheme() == null){
                    redirectUri.setScheme("http");
                }
                // if("".equals(redirectUri.getHost())){
                if(redirectUri.getHost() == null){
                    redirectUri.setHost(addr.getHostAddress());
                }
                if(redirectUri.getPort() == 0){
                    int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
                    if ( httpPort != 80 ) {
                        redirectUri.setPort(httpPort);
                    }
                }
                return generateRedirect(details, redirectUri, redirectParameters);
            }
        }
    }

    /**
     * Retrieve uribuilder of uri to redirect to.
     * @return URIBuilder of uri to redirect to..
     * redirect uri.
     */
    protected URIBuilder getRedirectUri()
    {
        URIBuilder redirectUri = null;
        try{
            redirectUri = new URIBuilder();
            redirectUri = new URIBuilder(this.redirectUri.build());
        }catch(Exception e){
            logger.warn("getRedirectUri: Unable to copy existing redirectUri", e);
        }
        return redirectUri;
    }

    /**
     * Retrieve hash of redirect parameters to add to redirectUri.
     * @return Map of string/objct of parameters.
     */
    protected Map<String,Object> getRedirectParameters()
    {
        return new HashMap<String,Object>(this.redirectParameters);
    }

    /**
     * Generate the "Simple" (text-only) response for the specified
     * nonce and session
     * @param redirectDetails
     * @param session
     * @param uri
     * @param requestHeader
     * @return the token array
     */
    public Token[] generateSimpleResponse( T redirectDetails, AppTCPSession session, String uri, HeaderToken requestHeader )
    {
        return generateSimplePage(redirectDetails, imagePreferred(uri, requestHeader));
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
        return generateSimplePage(redirectDetails, false);
    }
    
    /**
     * getReplacement - must be overridden by final class
     * @param data
     * @return the string
     */
    protected abstract String getReplacement( T data );

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
     * @param redirectDetails
     * @param gif
     * @return the token array
     */
    private Token[] generateSimplePage( T redirectDetails, boolean gif )
    {
        ChunkToken chunk;
        if (gif) {
            byte[] buf = new byte[WHITE_GIF.length];
            System.arraycopy(WHITE_GIF, 0, buf, 0, buf.length);
            ByteBuffer bb = ByteBuffer.wrap(buf);
            chunk = new ChunkToken(bb);
        } else {
            String replacement = getReplacement(redirectDetails);
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
     * @param redirectDetails
     * @param redirectUri
     * @param redirectParameters
     * @return the token array
     */
    private Token[] generateRedirect(T redirectDetails, URIBuilder redirectUri, Map<String,Object> redirectParameters)
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 307, "Temporary Redirect");
        response[0] = sl;

        HeaderToken h = new HeaderToken();
        h.addField("Location", buildRedirectUri(redirectDetails, redirectUri, redirectParameters));
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

    /**
     * Crate parmaeters to add to redirectUri from map of parameters.
     * @param  redirectDetails Details object.
     * @param  redirectParameters Map of String (key) and Object (value).
     * @return                    List of NameValuePair to be added to URIBuilder.
     */
    protected List<NameValuePair> buildRedirectParameters(T redirectDetails, Map<String,Object> redirectParameters)
    {
        List<NameValuePair> parameters = new ArrayList<>();
        String value;
        for( String key : redirectParameters.keySet()){
            /**
             * Pull the value as follows:
             */
            if(redirectParameters.get(key) != null){
                /**
                 * 1. Value is the non-null object of the map.
                 */
                value = redirectParameters.get(key).toString();
            }else{
                /**
                 * See if getKeyname is in Details object and if so, cache the method.
                 */
                value = null;
                Class<?> cls = redirectDetails.getClass();
                if(ParameterClassMethodMap.get(cls) == null){
                    ParameterClassMethodMap.put(cls, new HashMap<>());
                }
                Method method = ParameterClassMethodMap.get(cls).get(key);
                if(!ParameterClassMethodMap.get(cls).containsKey(key) && method == null){
                    logger.warn("search for method for key="+key);
                    try{
                        ParameterClassMethodMap.get(cls).put(key, null);
                        method = cls.getMethod("get" + key.substring(0, 1).toUpperCase() + key.substring(1));
                        ParameterClassMethodMap.get(cls).put(key, method);
                    }catch(Exception e){}
                }
                if(method != null){
                    /**
                     * 2. Pull value from Details.
                     */
                    try{
                        Object objValue = method.invoke(redirectDetails);
                        if(objValue != null){
                            if(objValue.getClass() == Inet4Address.class){
                                value = ((Inet4Address) objValue).getHostAddress();
                            }else{
                                value = objValue.toString();
                            }
                        }
                    }catch(Exception e){
                        logger.warn("Unable to retrieve value for key=" + key, e);
                    }
                }

                if(value == null){
                    /**
                     * 3. Special values.
                     */
                    if(key.equals("nonce")){
                        /**
                         * If the nonce is not otherwise overriden (CaptivePortal https),
                         * then serialize here and return the value.
                         */
                        value = generateNonce(redirectDetails);
                    }else if(key.equals("appid")){
                        /**
                         * Numeric applicaton id.
                         */
                        value = appSettings.getId().toString();
                    }else if(key.equals("appname")){
                        /**
                         * Application name.
                         */
                        value = appSettings.getAppName();
                    }
                }

                if(value == null){
                    /**
                     * 4. If we dont' have it here, set to epty string.
                     */
                    value = "";
                }
            }
            parameters.add( new BasicNameValuePair(key,value));
        }
        return parameters;
    }

    /**
     * getRedirectUri for the details using the redirectUrl and redirectParams
     * @param redirectDetails
     * @param redirectUri
     * @param redirectParameters
     * @return the URL
     */
    protected String buildRedirectUri(T redirectDetails, URIBuilder redirectUri, Map<String,Object> redirectParameters){
        redirectUri.setParameters(buildRedirectParameters(redirectDetails, redirectParameters));
        logger.warn("buildRedirectUri:" + redirectUri.toString());
        return redirectUri.toString();
    }

}
