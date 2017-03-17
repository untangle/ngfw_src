/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Set;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
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

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanner; 

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.TomcatManager;
import com.untangle.uvm.util.I18nUtil;

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
        
        ctx = loadServlet("/webui", "webui", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/admin", "admin", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/setup", "setup", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/blockpage", "blockpage");
    }

    public ServletContext loadServlet(String urlBase, String rootDir)
    {
        return loadServlet(urlBase, rootDir, null, null);
    }

    public ServletContext loadServlet(String urlBase, String rootDir, boolean requireAdminPrivs)
    {
        return loadServlet(urlBase, rootDir, null, null, new AdministrationValve());
    }

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

    protected void apacheReload()
    {
        writeIncludes();

        UvmContextFactory.context().execManager().exec("/usr/sbin/service apache2 reload");
    }
    
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

    private ServletContext loadServlet(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth)
    {
        return loadServlet(urlBase, rootDir, realm, auth, null);
    }

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
            ctx.setSessionCookieName(getCookieName());
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
     */
    private static String getCookieName()
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

        cookieName = "session-" + cookieName.substring(0,8);
        return cookieName;
    }

    private class AdministrationValve extends ValveBase
    {
        private final Logger logger = Logger.getLogger(getClass());

        public AdministrationValve() { }

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

        private String administrationDenied()
        {
            Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle");
            return I18nUtil.tr("HTTP administration is disabled.", i18n_map);
        }

        private boolean isAccessAllowed( ServletRequest request )
        {
            String address = request.getRemoteAddr();
            boolean isHttpAllowed = UvmContextFactory.context().systemManager().getSettings().getHttpAdministrationAllowed();

            logger.debug("isAccessAllowed( " + request + " ) [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 

            /**
             * Always allow HTTP from 127.0.0.1
             */
            try {
                if (address != null && InetAddress.getByName( address ).isLoopbackAddress())
                    return true;
            } catch (UnknownHostException e) {
                logger.warn( "Unable to parse the internet address: " + address );
            }
        
            /**
             * Otherwise only allow HTTP if enabled
             */
            if (request.getScheme().equals("http")) {
                if (!isHttpAllowed)
                    logger.warn("isAccessAllowed( " + request + " ) denied. [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 
                return isHttpAllowed;
            }
            else
                return true; /* https always allowed */
        }
    }
}
