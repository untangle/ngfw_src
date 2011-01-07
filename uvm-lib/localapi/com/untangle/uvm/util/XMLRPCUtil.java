/* $HeadURL$ */
package com.untangle.uvm.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

// Utility for calling XML-RPC utilities
public class XMLRPCUtil
{
    private static XMLRPCUtil INSTANCE = new XMLRPCUtil();

    /* This is the controller for the UVM */
    public static final String CONTROLLER_UVM = "uvm";

    /* XXX Should be able to configure these values in properties XXX */
    private static final String ALPACA_BASE_URL = "http://localhost:3000/alpaca/";
    private static final String ALPACA_NONCE_FILE = "/etc/untangle-net-alpaca/nonce";

    /* 10 seconds */
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;

    private final Logger logger = Logger.getLogger(getClass());

    private final XmlRpcClient client = new XmlRpcClient();

    /* Singleton */
    private XMLRPCUtil()
    {
    }

    /**
     * Call method at URL, using args.
     * @param url The URL to call.
     * @param method The method to call at that URL.
     * @param callback Set this value to perform an asynchronous callback, otherwise the call is synchronous.
     * @param params Parameters to pass to the method.
     */
    public Object call( String url, String method, AsyncCallback callback, Object ... params )
        throws MalformedURLException, XmlRpcException
    {
        XmlRpcClientConfig config = makeConfig( url ) ;

        if ( callback == null ) return this.client.execute( config, method, params ); 

        client.executeAsync( config, method, params, callback );

        return null;
    }

    /**
     * Call alpaca.
     * @param controller The controller in the alpaca to access.
     * @param method The method to call on that controlller.
     * @param callback Set this value to perform an asynchronous callback, otherwise the call is synchronous.
     * @param params Parameters to pass to the method.
     */
    public Object callAlpaca( String component, String method, AsyncCallback callback, Object ... params )
        throws MalformedURLException, XmlRpcException, IOException, Exception
    {
        return call( ALPACA_BASE_URL + component + "/api?argyle=" + getNonce(), method, callback, params );
    }
    
    public static XMLRPCUtil getInstance()
    {
        return INSTANCE;
    }

    /* Just make a call, and ignore when it returns */
    public static class NullAsyncCallback implements AsyncCallback
    {
        private final Logger logger;

        public NullAsyncCallback( Logger logger )
        {
            this.logger = logger;
        }
        
        public void handleError( XmlRpcRequest request, Throwable error )
        {
            if ( this.logger != null ) this.logger.warn( "Call failed: " + request, error );
        }

        public void handleResult( XmlRpcRequest request, Object result )
        {
            if ( this.logger != null ) {
                this.logger.debug( "Call completed: " + request + " result: " + result );
            }
        }
    }

    /* --------- private --------- */
    private String getNonce() throws IOException, Exception 
    {
        BufferedReader stream = null;
        try {
            stream = new BufferedReader( new FileReader( ALPACA_NONCE_FILE ));
            
            String nonce = stream.readLine();
            if ( nonce.length() < 3 ) {
                throw new Exception( "Invalid nonce in the file [" + ALPACA_NONCE_FILE + "]: ', " + nonce + "'" );
            }

            return nonce;
        } finally {
            try {
                if ( stream != null ) stream.close();
            } catch ( IOException e ) {
                logger.warn( "Unable to close the nonce file: " + ALPACA_NONCE_FILE, e );
            }
        }
    }

    private XmlRpcClientConfig makeConfig( String url )
        throws MalformedURLException
    {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL( new URL( url ));
        config.setConnectionTimeout( DEFAULT_CONNECTION_TIMEOUT_MS );
        return config;
    }
}
