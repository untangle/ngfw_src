/**
 * $Id$
 */
package com.untangle.node.openvpn.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.IPAddress;
import com.untangle.node.openvpn.Constants;
import com.untangle.node.openvpn.VpnNode;
import com.untangle.uvm.util.ServletStreamer;

class Util
{
    private static final Util INSTANCE = new Util();

    private static final String BASE_DIRECTORY = Constants.PACKAGES_DIR;

    public static final  String DISTRIBUTION_KEY_PARAM = "key";

    static final String ATTR_BASE = "com.untangle.node.openvpn.servlet.";

    private static final String EXPIRATION_SESSION_ATTR  = ATTR_BASE + "expiration";
    private static final String COMMON_NAME_SESSION_ATTR = ATTR_BASE + "common-name";

    static final String COMMON_NAME_ATTR = ATTR_BASE + "common-name";
    static final String REASON_ATTR      = ATTR_BASE + "reason";
    static final String DEBUGGING_ATTR   = ATTR_BASE + "debugging";
    static final String VALID_ATTR       = ATTR_BASE + "valid";

    /* Download in at least a half hour, seems reasonable */
    private static final long TIMEOUT = 1000 * 60 * 30;

    private final Logger logger = Logger.getLogger( this.getClass());

    private Util()
    {
    }

    /* Returns true if this page requires a secure redirect */
    boolean requiresSecure( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        /* Let the admin do whatever they want */
        try {
            if ( isAdmin( request )) return false;
        } catch ( Exception e ) {
            logger.warn( "Unable to determine if the user is an admin.", e );
        }

        if ( request.getScheme().equals( "https" )) return false;
             
        /* Otherwise, reject the page, they definitely didn't use the link to get it */
        rejectFile( request, response );
        return true;
    }

    /* Returns the commonName for the request, or null if the request is not valid */
    String getCommonName(HttpServlet servlet, HttpServletRequest request )
    {
        VpnNode node = null;

        try {
            node = getNode();
        } catch ( Exception e ) {
            logger.warn( "Unable to get common name.", e );
            return null;
        }

        /* Handle admin users that want to download the client directly. */
        if ( isAdmin( request, node )) {
            logger.debug( "Authenticated a valid administative login" );

            String client = request.getParameter( Constants.ADMIN_DOWNLOAD_CLIENT_PARAM );
            if ( client != null ) {
                logger.debug( "Sending the client: <" + client + "> to an administrator." );
                return client;
            }
        }
        
        String key = request.getParameter( DISTRIBUTION_KEY_PARAM );

        if ( key != null ) key = key.trim();

        if (logger.isDebugEnabled()) logger.debug( "key is : " + key );

        if ( key != null && key.length() > 0 ) {
            String commonName = null;

            try {
                IPAddress address = IPAddress.parse( request.getRemoteAddr());
                commonName = node.lookupClientDistributionKey( key, address );
            } catch ( Exception e ) {
                logger.error( "Error connecting to the openvpn node", e );
                request.setAttribute( REASON_ATTR, "Error connnecting to the openvpn node " + e );
                return null;
            }

            if ( null == commonName ) {
                request.setAttribute( REASON_ATTR, "Key doesn't exist" );
                return null;
            }

            HttpSession session = request.getSession( true );

            session.setAttribute( COMMON_NAME_SESSION_ATTR, commonName );
            session.setAttribute( EXPIRATION_SESSION_ATTR, new Date( System.currentTimeMillis() + TIMEOUT ));

            return commonName;
        } else {
            /* Look into the session to see if there is data about the user */
            HttpSession session = request.getSession( false );

            /* If they have no session information, then there is nothing to do */
            if ( session == null ) {
                request.setAttribute( REASON_ATTR, "User doesn't have a session" );
                return null;
            }

            Date expirationDate = (Date)session.getAttribute( EXPIRATION_SESSION_ATTR );
            String commonName = (String)session.getAttribute( COMMON_NAME_SESSION_ATTR );
            if ( commonName == null || expirationDate == null ) {
                request.setAttribute( REASON_ATTR, "Common name or expiration is null" );
                return null;
            }

            /* If the session is expired, kill it */
            Date now = new Date();
            if ( now.after( expirationDate )) {
                request.setAttribute( REASON_ATTR, "Session expired at " + expirationDate );
                return null;
            }

            return commonName;
        }
    }

    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    void streamFile( HttpServletRequest request, HttpServletResponse response,
                     String fileName, String downloadFileName, String type )
        throws ServletException, IOException
    {
        fileName = BASE_DIRECTORY + "/" + fileName;

        InputStream fileData;

        long length = 0;

        logger.debug( "Streaming '" + fileName + "'" );

        try {
            File file = new File( fileName );            
            fileData  = new FileInputStream( file );
            length = file.length();
        } catch ( FileNotFoundException e ) {
            logger.info( "The file '" + fileName + "' does not exist" );
            request.setAttribute( Util.REASON_ATTR, "The file '" + fileName + "' does not exist" );
            rejectFile( request, response );
            return;
        }

        ServletStreamer ss = ServletStreamer.getInstance();

        ss.stream( request, response, fileData, downloadFileName, type, length );
    }

    void rejectFile( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        request.setAttribute( DEBUGGING_ATTR, "" );
        request.setAttribute( VALID_ATTR, false );

        /* Indicate that the response was not rejected */
        response.setStatus( HttpServletResponse.SC_FORBIDDEN );
        request.getRequestDispatcher( "/Index.jsp" ).forward( request, response );
    }

    VpnNode getNode()  throws Exception
    {
        UvmContext ctx = UvmContextFactory.context();
        NodeSettings tid = ctx.nodeManager().nodeInstances( "untangle-node-openvpn" ).get( 0 );
        if ( tid == null ) throw new Exception( "OpenVPN is not loaded." );
        VpnNode node = (VpnNode) ctx.nodeManager().node( tid.getId() );

        if ( node == null ) throw new Exception( "OpenVPN is not loaded." );
        return node;
    }
    
    boolean isAdmin( HttpServletRequest request ) throws Exception
    {
        return isAdmin( request, getNode());
    }

    boolean isAdmin( HttpServletRequest request, VpnNode node )
    {
        String key = request.getParameter( Constants.ADMIN_DOWNLOAD_CLIENT_KEY );
        if ( key == null ) return false;
        if ( !node.isAdminKey( key )) return false;

        return true;
    }

    static Util getInstance()
    {
        return INSTANCE;
    }
}
