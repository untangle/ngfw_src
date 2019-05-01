/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;
import com.untangle.app.directory_connector.DirectoryConnectorApp;
import com.untangle.app.directory_connector.LoginEvent;

/**
 * Registration
 */
@SuppressWarnings("serial")
public class Registration extends HttpServlet
{
    private static final String AD_FILE_NAME = "user_notification.vbs";
    private static final String AD_DOWNLOAD_NAME = System.getProperty( "uvm.home" ) + "/web/userapi/untangle_user.vbs";
    private static final String AD_DOWNLOAD_TYPE = "application/download";
    private static final String AD_REPLACE_ADDRESS = "%UNTANGLE_REPLACE_WITH_ADDRESS%";
    private static final String AD_REPLACE_SECRET = "%UNTANGLE_REPLACE_WITH_SECRET%";

    private final Logger logger = Logger.getLogger( this.getClass());

    /**
     * Handle GET from user API request.
     *
     * @param request
     *  HttpServelnetRequest object.
     * @param response
     *  HttpServletResponse object.
     * @throws ServletException
     *  If problem handling requeust.
     * @throws IOException
     *  If problem acceping request.
     */
    protected void doGet( HttpServletRequest request,  HttpServletResponse response ) throws ServletException, IOException
    {
        DirectoryConnectorApp directoryConnector = (DirectoryConnectorApp)UvmContextFactory.context().appManager().app("directory-connector");

        String download = request.getParameter( "download" );
        if ( download != null && download.equals( "download" ) ) {
            generateInstaller( request, response );
            return;
        }

        if ( ! directoryConnector.getSettings().getApiEnabled() ) {
            logger.warn("API not enabled.");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        response.setContentType( "text/html" );
        response.setHeader( "Content-Disposition", "text/html");

        String requiredSecretKey = directoryConnector.getSettings().getApiSecretKey();

        String username = null;
        String hostname = null;
        String action = null;
        String secretKey = null;
        String clientIp = null;
        String domain = null;
        InetAddress inetAddress;

        try {
            inetAddress = InetAddress.getByName(request.getRemoteAddr());
        } catch (Exception e) {
            logger.warn( "Unable to parse the internet address: " + request.getRemoteAddr());
            return;
        }

        Map<String, String[]> parameters = request.getParameterMap();

        for ( String keyitr : parameters.keySet() ) {
            String key = keyitr.toLowerCase();
            switch ( key ) {
            case "username": username = parameters.get( keyitr )[0].toLowerCase(); break;
            case "domain": domain = parameters.get( keyitr )[0].toLowerCase(); break;
            case "hostname": hostname = parameters.get( keyitr )[0].toLowerCase(); break;
            case "action": action = parameters.get( keyitr )[0]; break;
            case "secretkey": secretKey = parameters.get( keyitr )[0]; break;
            case "clientip": clientIp = parameters.get( keyitr )[0]; break;
            default:
                // do nothing
            }
        }

        if (requiredSecretKey != null && ! "".equals(requiredSecretKey)) {
            if (!requiredSecretKey.equals(secretKey)) {
                logger.warn("Secret key does not match.");
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        if ( clientIp != null && !clientIp.equals("") ) {
            /**
             * Only allow the client IP to be specified manually if the spoofing is allowed
             * Or the secretkey is specified (and correct)
             */
            if ( directoryConnector.getSettings().getApiManualAddressAllowed() || (secretKey != null && !secretKey.equals("")) ) {
                try {
                    inetAddress = InetAddress.getByName(clientIp);
                } catch (Exception e) {
                    logger.warn( "Unable to parse the internet address: " + request.getRemoteAddr());
                    return;
                }
            }
        }

        logger.debug("User API ( action=" + action + " username=" + username + " domain=" + domain + " hostname=" + hostname + " clientIp=" + clientIp + " secretKey=" + secretKey + " )");
        
        //String remoteHost = request.getRemoteHost();
        if ( username == null ) {
             logger.warn( "Invalid registration request, missing username" );
             return;
        }
        if (action == null ) {
            //only two possible actions are logout and login
            //if not specified assume login
            action = "login"; 
        }


        if (action.equals("logout")) {
            logger.debug( "logout   user: " + username + " hostname: " + hostname + " clientIp: " + clientIp );

            UvmContextFactory.context().hostTable().getHostTableEntry( inetAddress, true ).setUsernameDirectoryConnector( null );

            LoginEvent evt = new LoginEvent( inetAddress, username, domain, LoginEvent.EVENT_LOGOUT );
            UvmContextFactory.context().logEvent( evt );
            
        } else if (action.equals("login")) {
            String eventAction;

            logger.debug( "register user: " + username + " hostname: " + hostname + " clientIp: " + clientIp );
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( inetAddress, true );

            if (entry.getUsernameDirectoryConnector() != null && entry.getUsernameDirectoryConnector().equals(username))
                eventAction = LoginEvent.EVENT_UPDATE;
            else
                eventAction = LoginEvent.EVENT_LOGIN;
            
            entry.setUsernameDirectoryConnector( username );

            LoginEvent evt = new LoginEvent( inetAddress, username, domain, eventAction );
            UvmContextFactory.context().logEvent( evt );

            /* If the hostname was specified and is not already known - set it */
            if ( hostname != null )
                entry.setHostnameDirectoryConnector( hostname );
        } else if (action.equals("groupcache")) {
            logger.debug( "refresh groupcache: " + username + " clientIp: " + clientIp );
            directoryConnector.refreshGroupCache();
        }
        
    }

    /**
     * Generate installer script.
     *
     * @param request
     *  HttpServelnetRequest object.
     * @param response
     *  HttpServletResponse object.
     * @throws ServletException
     *  If problem handling requeust.
     */
    private void generateInstaller( HttpServletRequest request, HttpServletResponse response ) throws ServletException
    {
        logger.info( "Generating installer" );
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File file = new File( AD_DOWNLOAD_NAME );
            fr = new FileReader( file );
            br = new BufferedReader( fr );

            //long length = file.length();
            response.setContentType( AD_DOWNLOAD_TYPE );
            response.setHeader( "Content-Disposition", "attachment; filename=\""+AD_FILE_NAME+"\"" );
            //Since we substitute and other things the length of file != equal length outputted
            //response.setHeader( "Content-Length", "" + length );
            DirectoryConnectorApp directoryConnector = (DirectoryConnectorApp)UvmContextFactory.context().appManager().app("directory-connector");

            PrintWriter out = response.getWriter();
            String line = null;
            while( (line = br.readLine()) != null ) {
                StringBuffer chunk = new StringBuffer( line );

                int replaceLocation = chunk.indexOf(AD_REPLACE_ADDRESS);
                int replaceSecret = chunk.indexOf(AD_REPLACE_SECRET);
                if (replaceLocation != -1) {
                    // int port = request.getServerPort(); // this won't work because admin is often done via http
                    int port = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
                    String name = request.getServerName();
                    String url;
                    if ( port == 80 )
                        url = name;
                    else
                        url = name + ":" + port;
                    
                    // replace %UNTANGLE_REPLACE_WITH_ADDRESS% with a best guess URL
                    chunk.replace( replaceLocation, replaceLocation+AD_REPLACE_ADDRESS.length(), url);

                    out.print( chunk.toString() );
                } else if ( replaceSecret != -1 ) {
                    String secretKey = directoryConnector.getSettings().getApiSecretKey();
                    secretKey = secretKey == null ? "" : secretKey;
                    chunk.replace( replaceSecret, replaceSecret+AD_REPLACE_SECRET.length(), secretKey);
                    out.print( chunk.toString() );
                } else {
                    out.print( line );
                }
                out.print( "\r\n" );
            }
            response.flushBuffer();
        } catch ( FileNotFoundException e ) {
            logger.info( "The file " + AD_DOWNLOAD_NAME +  " does not exist" );
        } catch ( IOException e ) {
            logger.info( "IOError while reading from file " + AD_DOWNLOAD_NAME);
        }finally{
            if( fr != null ){
                try{
                    fr.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
            if( br != null ){
                try{
                    br.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
        }
    }

}
