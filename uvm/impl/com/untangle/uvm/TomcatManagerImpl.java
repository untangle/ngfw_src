/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Map;

import javax.naming.Name;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ValveBase;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.TomcatManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.IPMatcher;

/**
 * Wrapper around the Tomcat server embedded within the UVM.
 */
public class TomcatManagerImpl implements TomcatManager
{
    private static final int     TOMCAT_MAX_POST_SIZE = 16777216; // 16MB
    private static final String  TOMCAT_AJP_MIN_THREADS = "2"; 
    private static final String  TOMCAT_AJP_MAX_THREADS = "10"; 

    private static final String WELCOME_URI = "/setup/welcome.do";
    private static final String WELCOME_FILE = System.getProperty("uvm.conf.dir") + "/apache2/conf.d/homepage.conf";

    private static final Logger logger = Logger.getLogger(TomcatManagerImpl.class);

    private final Tomcat tomcat;
    private final StandardHost baseHost;
    private final String webAppRoot;
    
    private static final String[] tldScanTargets = {"untangle-libuvm-taglib.jar","standard.jar","smtp-servlet-quarantine.jar"};

    /**
     * constructor
     * @param uvmContext
     * @param threadRequest
     * @param catalinaHome
     * @param webAppRoot
     * @param logDir
     */
    protected TomcatManagerImpl(UvmContextImpl uvmContext, InheritableThreadLocal<HttpServletRequest> threadRequest, String catalinaHome, String webAppRoot, String logDir)
    {
        this.webAppRoot = webAppRoot;
        String hostname = "localhost";

        tomcat = new Tomcat();
        tomcat.setBaseDir(catalinaHome);
        logger.info("Catalina Home:" + catalinaHome);
        logger.info("Tomcat start/stop threads:" + Runtime.getRuntime().availableProcessors());
        //Don't need HTTP endpoint, only AJP will be used
        Connector httpConnector = tomcat.getConnector();
        try {
            httpConnector.stop();
            httpConnector.destroy();
        } catch (Exception ex) {
            logger.error("Exception while stopping http connector", ex);
        }

        // create an Engine
        StandardEngine baseEngine = (StandardEngine)tomcat.getEngine();

        // set Engine properties
        //baseEngine.setName("tomcat");
        baseEngine.setDefaultHost(hostname);
        baseEngine.setParentClassLoader( Thread.currentThread().getContextClassLoader() );

        // create Host
        baseHost = (StandardHost)tomcat.getHost();
        baseHost.setUnpackWARs(true);
        baseHost.setDeployOnStartup(true);
        baseHost.setAutoDeploy(true);
        baseHost.setErrorReportValveClass("com.untangle.uvm.UvmErrorReportValve");
        baseHost.setStartStopThreads(Runtime.getRuntime().availableProcessors());

        ServletContext ctx;
        
        ctx = loadServlet("/admin", "admin", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/setup", "setup", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/blockpage", "blockpage");
    }

    /**
     * loadServlet loads a servlet
     * @param urlBase 
     * @param rootDir
     * @return ServletContext
     */
    public ServletContext loadServlet(String urlBase, String rootDir)
    {
        return loadServlet(urlBase, rootDir, null, null);
    }

    /**
     * loadServlet loads a servlet
     * @param urlBase
     * @param rootDir
     * @param requireAdminPrivs - true if servlet is an admin-privilege servlet
     * @return ServletContext
     */
    public ServletContext loadServlet(String urlBase, String rootDir, boolean requireAdminPrivs)
    {
        return loadServlet(urlBase, rootDir, null, null, new AdministrationValve());
    }

    /**
     * unloadServlet unloads a servlet
     * @param contextRoot 
     * @return true if success, false otherwise
     */
    public boolean unloadServlet(String contextRoot)
    {
        try {
            if ( baseHost != null ) {
                Container c = baseHost.findChild(contextRoot);
                if ( c != null ) {
                    logger.info("Unloading Servlet: " + contextRoot);

                    baseHost.removeChild(c);

                    // this seems to cause a warning about destroy() being called twice
                    // try {
                    //     ((StandardContext)c).destroy();
                    // } catch (Exception x) {
                    //     logger.warn("Exception destroying web app \"" + contextRoot + "\"", x);
                    // }

                    return true;
                }
            }
        } catch(Exception ex) {
            logger.error("Unable to unload web app \"" + contextRoot + "\"", ex);
        }
        return false;
    }

    /**
     * Gives no exceptions, even if Tomcat was never started.
     */
    public void stopTomcat()
    {
        try {
            // if development environment - delete special file
            if (UvmContextImpl.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("rm -f /etc/apache2/uvm-dev.conf");
            }

            if (null != tomcat) {
                tomcat.stop();
            }
        } catch (LifecycleException exn) {
            logger.debug(exn);
        }
    }

    /**
     * writeWelcomeFile writes an apache rewrite conf file to redirect / to the welcome URL
     */
    protected void writeWelcomeFile()
    {
        FileWriter w = null;
        try {
            w = new FileWriter(WELCOME_FILE);

            // RewriteRule doesnt handle HTTPS port correctly
            //w.write("RewriteEngine On\n");
            //w.write("RewriteRule ^/index.html$ " + WELCOME_URI + " [R=302]\n");
            //w.write("RewriteRule ^/$ " + WELCOME_URI + " [R=302]\n");

            // old apache way
            // w.write("RedirectMatch 302 ^/index.html " + WELCOME_URI + "\n");
            w.write("RedirectMatch 302 ^/$ " + WELCOME_URI + "\n");
        } catch (IOException exn) {
            logger.warn("could not write homepage redirect", exn);
        } finally {
            if (null != w) {
                try {
                    w.close();
                } catch (IOException exn) {
                    logger.warn("could not close FileWriter", exn);
                }
            }
        }

    }

    /**
     * apacheReload reloads apache
     */
    protected void apacheReload()
    {
        writeIncludes();
        writeModPythonConf();

        UvmContextFactory.context().execManager().exec("systemctl reload apache2");
    }
    
    /**
     * startTomcat starts tomcat server
     */
    protected void startTomcat()
    {
        logger.info("Tomcat starting...");

        Connector jkConnector = new Connector("org.apache.coyote.ajp.AjpNioProtocol");
        jkConnector.setPort(8009);
        jkConnector.setDomain("127.0.0.1");

        jkConnector.setProperty("port", "8009");
        jkConnector.setProperty("address", "127.0.0.1");
        jkConnector.setProperty("tomcatAuthentication", "false");
        jkConnector.setProperty("minSpareThreads", TOMCAT_AJP_MIN_THREADS);
        jkConnector.setProperty("maxThreads", TOMCAT_AJP_MAX_THREADS);
        jkConnector.setMaxPostSize(TOMCAT_MAX_POST_SIZE);
        jkConnector.setMaxSavePostSize(TOMCAT_MAX_POST_SIZE); 

        String secret = getSecret();
        if (null != secret) {
            jkConnector.setProperty("requiredSecret", secret);
        }
        tomcat.getService().addConnector(jkConnector);

        // start operation
        try {
            tomcat.start();
            logger.info("jkConnector started (maxPostSize = " + TOMCAT_MAX_POST_SIZE + " bytes)");
        } catch ( Exception exn ) {
            logger.warn( "Exception starting tomcat:", exn );
            UvmContextImpl.getInstance().fatalError("Failed to start Tomcat", exn);
            return;
        }

        logger.info("Tomcat started");
    }

    /**
     * loadServlet loads a servlet
     * @param urlBase
     * @param rootDir
     * @param realm
     * @param auth
     * @return ServletContext
     */
    private ServletContext loadServlet(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth)
    {
        return loadServlet(urlBase, rootDir, realm, auth, null);
    }

    /**
     * loadServlet loads a servlet
     * @param urlBase 
     * @param rootDir
     * @param realm
     * @param auth
     * @param valve
     * @return ServletContext
     */
    private ServletContext loadServlet(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, Valve valve)
    {
        return loadServletImpl(urlBase, rootDir, realm, auth, valve);
    }

    /**
     * Loads the web application.  If Tomcat is not yet running,
     * schedules it for later loading.
     *
     * @param urlBase a <code>String</code> value
     * @param rootDir a <code>String</code> value
     * @param realm a <code>Realm</code> value
     * @param auth an <code>AuthenticatorBase</code> value
     * @param valve 
     * @return a <code>boolean</code> value
     */
    private synchronized ServletContext loadServletImpl(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, Valve valve)
    {
        String fqRoot = webAppRoot + "/" + rootDir;

        logger.info("Loading Servlet: " + urlBase);

        try {
            StandardContext ctx = (StandardContext)tomcat.addWebapp(urlBase,fqRoot);
            final Logger log = logger;

            ctx.setCrossContext(true);
            ctx.setSessionTimeout(30); // 30 minutes
            ctx.setSessionCookieName("session-"+getCookieSuffix());
            ctx.setSessionCookiePath("/");
            if ( realm != null ) {
                ctx.setRealm(realm);
            }
            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); // disable session persistence
            ctx.setManager(mgr);

            if ( valve != null )
                ctx.addValve(valve);
            if ( auth != null ) {
                Pipeline pipe = ctx.getPipeline();
                auth.setDisableProxyCaching(false);
                pipe.addValve(auth);
            }
            return ctx.getServletContext(); 
        } catch(Exception ex) {
            logger.error("Unable to deploy webapp \"" + urlBase + "\" from directory \"" + fqRoot + "\"", ex);
            return null;
        }
    }

    /**
     * writeIncludes writes the apache includes conf files
     * Default is /etc/apache2/uvm.conf
     * If this is devel its /etc/apache2/uvm-dev.conf
     * That file will tell it to load all the
     * PREFIX/usr/share/untangle/apache2/conf.d/*.conf
     */
    private void writeIncludes()
    {
        String confDir = System.getProperty("uvm.conf.dir");
        FileWriter fw = null;

        String filename = "/etc/apache2/uvm.conf";
        // If the dev environment - write to a different file
        if (UvmContextImpl.context().isDevel()) {
            filename = "/etc/apache2/uvm-dev.conf";
        }

        try {
            fw = new FileWriter(filename);
            fw.write("Include " + confDir + "/apache2/conf.d/*.conf\n");
        } catch (IOException exn) {
            logger.warn("could not write includes: conf.d");
        } finally {
            if ( fw != null ) {
                try {
                    fw.close();
                } catch (IOException exn) {
                    logger.warn("could not close file", exn);
                }
            }
        }
    }

    /**
     * writeModPythonConf writes the mod_python apache2 conf file
     */
    private void writeModPythonConf()
    {
        try {
            Path path = Paths.get("/etc/apache2/mods-available/python.load");
            Charset charset = StandardCharsets.UTF_8;

            /**
             * Change the standard cookie name to something unique per server
             */
            String content = new String(Files.readAllBytes(path), charset);
            String newContent = content.replaceAll("authsession", "auth-"+getCookieSuffix());
            if (content.hashCode() != newContent.hashCode()) {
                logger.info("Writing new /etc/apache2/mods-available/python.load");
                Files.write(path, newContent.getBytes(charset));
            }
        } catch (Exception e) {
            logger.warn("Failed to set python cookie name",e);
        }
    }

    /**
     * getSecret gets the AJP secret from /etc/apache2/workers.properties
     * @return the secret
     */
    private String getSecret()
    {
        Properties p = new Properties();

        FileReader r = null;
        try {
            r = new FileReader("/etc/apache2/workers.properties");
            p.load(r);
        } catch (IOException exn) {
            logger.warn("could not read secret", exn);
        } finally {
            if (null != r) {
                try {
                    r.close();
                } catch (IOException exn) {
                    logger.warn("could not close file", exn);
                }
            }
        }

        return p.getProperty("worker.uvmWorker.secret");
    }

    /**
     * In an effort to keep cookie names unique we speficy our own cookie name
     * Tomcat uses "JSESSIONID" by default to store the session state.
     *
     * Since cookies are stored per domain name, if you proxy several Untangle
     * admin connections through a centrail domain, they conflict because they
     * all use the "JSESSIONID" state to store session.
     *
     * This takes a unique identifier (the UID) and md5s it and takes the first
     * 8 characters of that md5 and returns "session-"+md5
     * So the cookie for this machine will be stored in "session-3e9f381d", for
     * example.
     * @return String
     */
    private static String getCookieSuffix()
    {
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.warn( "Unknown Algorith MD5", e);
            return "session";
        }

        String uid = UvmContextImpl.context().getServerUID().trim();
        if ( uid == null ) {
            logger.warn( "Missing UID!");
            return "session";
        }
        byte[] digest = md.digest(uid.getBytes());
        String cookieName = "";
        for (byte b : digest) {
            int c = b;
            if (c < 0) c = c + 0x100;
            cookieName += String.format("%02x", c);
        }
        return cookieName.substring(0,8);
    }

    /**
     * AdministrationValve is a valve that protects admin-priv servlets
     */
    private class AdministrationValve extends ValveBase
    {
        private final Logger logger = Logger.getLogger(getClass());

        /**
         * constructor
         */
        public AdministrationValve() { }

        /**
         * invoke invokes the request
         * @param request
         * @param response
         * @throws IOException
         * @throws ServletException
         */
        public void invoke( Request request, Response response ) throws IOException, ServletException
        {
            if ( !isAccessAllowed( request )) {
                logger.warn( "The request: " + request + " denied by AdministrationValve." );
                String msg = administrationDenied();
                request.setAttribute(TomcatManager.UVM_WEB_MESSAGE_ATTR, msg);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ( logger.isDebugEnabled()) {
                logger.debug( "The request: " + request + " allowed by AdministrationValve." );
            }

            /* If necessary call the next valve */
            Valve nextValve = getNext();
            if ( nextValve != null ) nextValve.invoke( request, response );
        }

        /**
         * administrationDenied returns the admin denied message
         * @return String
         */
        private String administrationDenied()
        {
            Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle");
            return I18nUtil.tr("HTTP administration is disabled.", i18n_map);
        }

        /**
         * isAccessAllowed checks to see if access to this admin servlet should be allowed
         * via the specified request. This checks If HTTP is allowed and if its coming from an allowed subnet.
         * @param request
         * @return true if allowed, false otherwise
         */
        private boolean isAccessAllowed( ServletRequest request )
        {
            try {
                InetAddress address = null;
                boolean isHttpAllowed = UvmContextFactory.context().systemManager().getSettings().getHttpAdministrationAllowed();
                String administrationSubnets = UvmContextFactory.context().systemManager().getSettings().getAdministrationSubnets();

                try {
                    address = InetAddress.getByName(request.getRemoteAddr());
                } catch (Exception e) {
                    logger.warn( "Unable to parse the internet address: " + address );
                    return true;
                }

                logger.debug("isAccessAllowed( " + request + " ) [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 

                /**
                 * Always allow from 127.0.0.1
                 */
                if (address.isLoopbackAddress())
                    return true;

                /**
                 * Otherwise only allow HTTP if enabled
                 */
                if (request.getScheme().equals("http") && !isHttpAllowed) {
                    logger.warn("isAccessAllowed( " + request + " ) denied. [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 
                    return false;
                }

                /**
                 * Always allow from admin subnets
                 */
                if (administrationSubnets != null) {
                    IPMatcher subnets = new IPMatcher(administrationSubnets);
                    if (!subnets.isMatch( address )) {
                        logger.warn("isAccessAllowed( " + request + " ) denied for " + address + ". [scheme: " + request.getScheme() + " subnet allowed: " + administrationSubnets + "]");
                        return false;
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception", e);
                return true;
            }

            return true;
        }
    }
}
