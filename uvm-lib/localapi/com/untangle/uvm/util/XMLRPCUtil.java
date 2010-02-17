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

import com.untangle.uvm.networking.NetworkException;

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
        throws MalformedURLException, XmlRpcException, IOException, NetworkException
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
    private String getNonce() throws IOException, NetworkException 
    {
        BufferedReader stream = null;
        try {
            stream = new BufferedReader( new FileReader( ALPACA_NONCE_FILE ));
            
            String nonce = stream.readLine();
            if ( nonce.length() < 3 ) {
                throw new NetworkException( "Invalid nonce in the file [" + ALPACA_NONCE_FILE + "]: ', " + 
                                            nonce + "'" );
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
