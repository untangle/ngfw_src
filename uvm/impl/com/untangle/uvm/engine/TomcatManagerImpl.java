/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.util.Properties;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.log4j.Logger;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanner; 

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.TomcatManager;
import com.untangle.uvm.util.AdministrationValve;

/**
 * Wrapper around the Tomcat server embedded within the UVM.
 */
public class TomcatManagerImpl implements TomcatManager
{
    private static final int  TOMCAT_NUM_RETRIES = 15; //  5 minutes total
    private static final long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    private static final int  TOMCAT_MAX_POST_SIZE = 16777216; // 16MB
    private static final String WELCOME_URI = "/setup/welcome.do";
    private static final String WELCOME_FILE = System.getProperty("uvm.conf.dir") + "/apache2/conf.d/homepage.conf";

    private final Logger logger = Logger.getLogger(getClass());

    private final Tomcat emb;
    private final StandardHost baseHost;
    private final String webAppRoot;
    
    private static final String[] tldScanTargets = {"untangle-libuvm-taglib.jar","standard.jar","untangle-casing-smtp-servlet-quarantine.jar"};

    // constructors -----------------------------------------------------------

    protected TomcatManagerImpl(UvmContextImpl uvmContext, InheritableThreadLocal<HttpServletRequest> threadRequest, String catalinaHome, String webAppRoot, String logDir)
    {
        this.webAppRoot = webAppRoot;
        String hostname = "localhost";

        emb = new Tomcat();
        emb.setBaseDir(catalinaHome);
        logger.info("Catalina Home:" + catalinaHome);
        logger.info("Tomcat start/stop threads:" + Runtime.getRuntime().availableProcessors());
        //Don't need HTTP endpoint, only AJP will be used
        Connector httpConnector = emb.getConnector();
        try {
            httpConnector.stop();
            httpConnector.destroy();
        } catch (Exception ex) {
            logger.error("Exception while stopping http connector", ex);
        }

        // create an Engine
        StandardEngine baseEngine = (StandardEngine)emb.getEngine();

        // set Engine properties
        //baseEngine.setName("tomcat");
        baseEngine.setDefaultHost(hostname);
        baseEngine.setParentClassLoader( Thread.currentThread().getContextClassLoader() );

        // create Host
        baseHost = (StandardHost)emb.getHost();
        baseHost.setUnpackWARs(true);
        baseHost.setDeployOnStartup(true);
        baseHost.setAutoDeploy(true);
        baseHost.setErrorReportValveClass("com.untangle.uvm.engine.UvmErrorReportValve");
        baseHost.setStartStopThreads(Runtime.getRuntime().availableProcessors());


        loadServlet("/blockpage", "blockpage");
        ServletContext ctx = loadServlet("/webui", "webui", true );
        ctx.setAttribute("threadRequest", threadRequest);

        ctx = loadServlet("/setup", "setup", true );
        ctx.setAttribute("threadRequest", threadRequest);
        writeWelcomeFile();
    }

    // package protected methods ----------------------------------------------

    public ServletContext loadServlet(String urlBase, String rootDir)
    {
        return loadServlet(urlBase, rootDir, null, null);
    }

    public ServletContext loadServlet(String urlBase, String rootDir, boolean requireAdminPrivs)
    {
        return loadServlet(urlBase, rootDir, null, null, new WebAppOptions(new AdministrationValve()));
    }

    public boolean unloadServlet(String contextRoot)
    {
        try {
            if (null != baseHost) {
                Container c = baseHost.findChild(contextRoot);
                if (null != c) {
                    logger.info("Unloading Servlet: " + contextRoot);
                    baseHost.removeChild(c);
                    try {
                        ((StandardContext)c).destroy();
                    } catch (Exception x) {
                        logger.warn("Exception destroying web app \"" + contextRoot + "\"", x);
                    }
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
            if (null != emb) {
                emb.stop();
            }
        } catch (LifecycleException exn) {
            logger.debug(exn);
        }
    }

    public void writeWelcomeFile()
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

        apacheReload();
    }

    synchronized void startTomcat()
    {
        logger.info("Tomcat starting...");

        Connector jkConnector = new Connector("org.apache.coyote.ajp.AjpNioProtocol");
        jkConnector.setPort(8009);
        jkConnector.setDomain("127.0.0.1");

        jkConnector.setProperty("port", "8009");
        jkConnector.setProperty("address", "127.0.0.1");
        jkConnector.setProperty("tomcatAuthentication", "false");
        jkConnector.setMaxPostSize(TOMCAT_MAX_POST_SIZE);
        jkConnector.setMaxSavePostSize(TOMCAT_MAX_POST_SIZE); 

        String secret = getSecret();
        if (null != secret) {
            jkConnector.setProperty("requiredSecret", secret);
        }
        emb.getService().addConnector(jkConnector);

        // start operation
        try {
            emb.start();
            logger.info("jkConnector started (maxPostSize = " + TOMCAT_MAX_POST_SIZE + " bytes)");
        } catch (LifecycleException exn) {
            // Note -- right now wrapped is always null!  Thus the
            // following horror:
            boolean isAddressInUse = isAIUExn(exn);
            if (isAddressInUse) {
                Runnable tryAgain = new Runnable() {
                        public void run() {
                            int i;
                            for (i = 0; i < TOMCAT_NUM_RETRIES; i++) {
                                try {
                                    logger.error("could not start Tomcat (address in use), sleeping 20 and trying again");
                                    Thread.sleep(TOMCAT_SLEEP_TIME);
                                    try {
                                        emb.stop();
                                    } catch (LifecycleException exn) {
                                        logger.error("Lifecycle Exception: ", exn);
                                    }
                                    emb.start();
                                    logger.info("Tomcat successfully started");
                                    break;
                                } catch (InterruptedException x) {
                                    logger.error( "Interrupted while trying to start tomcat, returning.", x);
                                    return;
                                } catch (LifecycleException x) {
                                    boolean isAddressInUse = isAIUExn(x);
                                    if (!isAddressInUse) {
                                        UvmContextImpl.getInstance().fatalError("Starting Tomcat", x);
                                        return;
                                    }
                                }
                            }
                            if (i == TOMCAT_NUM_RETRIES)
                                UvmContextImpl.getInstance().fatalError("Unable to start Tomcat after " +
                                                                        TOMCAT_NUM_RETRIES
                                                                        + " tries, giving up",
                                                                        null);
                        }
                    };
                new Thread(tryAgain, "Tomcat starter").start();
            } else {
                logger.error("Exception starting Tomcat",exn);
            }
        }

        logger.info("Tomcat started");
    }

    // private classes --------------------------------------------------------

    private static class WebAppOptions
    {
        public static final int DEFAULT_SESSION_TIMEOUT = 30;

        final boolean allowLinking;
        final int sessionTimeout; // Minutes
        final Valve valve;

        WebAppOptions() {
            this(true, DEFAULT_SESSION_TIMEOUT);
        }

        WebAppOptions(Valve valve) {
            this(true, valve);
        }

        WebAppOptions(boolean allowLinking, Valve valve) {
            this(allowLinking, DEFAULT_SESSION_TIMEOUT, valve);
        }

        WebAppOptions(boolean allowLinking, int sessionTimeout) {
            this(allowLinking, sessionTimeout, null);
        }

        WebAppOptions(boolean allowLinking, int sessionTimeout, Valve valve) {
            this.allowLinking = allowLinking;
            this.sessionTimeout = sessionTimeout;
            this.valve  = valve;

        }
    }

    // private methods --------------------------------------------------------

    private ServletContext loadServlet(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth)
    {
        return loadServlet(urlBase, rootDir, realm, auth, new WebAppOptions());
    }

    private ServletContext loadServlet(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, WebAppOptions options)
    {
        return loadServletImpl(urlBase, rootDir, realm, auth, options);
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
    private synchronized ServletContext loadServletImpl(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, WebAppOptions options)
    {
        String fqRoot = webAppRoot + "/" + rootDir;

        logger.info("Loading Servlet: " + urlBase);

        try {
            StandardContext ctx = (StandardContext)emb.addWebapp(urlBase,fqRoot);
            final Logger log = logger;
            JarScanner jarScanner = new JarScanner() {
                public void scan(ServletContext ctx, ClassLoader cld,  JarScannerCallback jsCallback, Set<String> jarsToSkip)  {
                    new StandardJarScanner().scan(ctx,cld, new JarScannerCallbackProxy(jsCallback,logger), jarsToSkip);
                }
            };
            ctx.setJarScanner(jarScanner);
            if (options.allowLinking)
                ctx.setAllowLinking(true);
            ctx.setCrossContext(true);
            ctx.setSessionTimeout(options.sessionTimeout);
            if (null != realm) {
                ctx.setRealm(realm);
            }
            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); // disable session persistence
            ctx.setManager(mgr);
            DirContext dc = ctx.getResources();
            if ( dc == null) {
                ctx.setResources(new StrongETagDirContext());
            } else {
                ctx.stop();
                ctx.setResources(new StrongETagDirContext());
                ctx.start();
            }
            if (null != options.valve) ctx.addValve(options.valve);
            if (null != auth) {
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

    private boolean isAIUExn(LifecycleException exn)
    {
        Throwable wrapped = exn.getCause();
        String msg = exn.getMessage();
        if (wrapped != null && wrapped instanceof java.net.BindException)
            // Never happens right now. XXX
            return true;
        if (msg.contains("address already in use"))
            return true;
        return false;
    }

    private void writeIncludes()
    {
        String dir = System.getProperty("uvm.conf.dir");
        if ( dir == null ) {
            dir = "/usr/share/untangle/conf";
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter("/etc/apache2/uvm.conf");
            fw.write("Include " + dir + "/apache2/conf.d/*.conf\n");
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

    private void apacheReload()
    {
        writeIncludes();

        UvmContextFactory.context().execManager().exec("/etc/init.d/apache2 reload");
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

    @SuppressWarnings("unchecked")
    private class StrongETagDirContext extends FileDirContext
    {
        protected Attributes doGetAttributes(String name, String[] attrIds) throws NamingException
        {
            ResourceAttributes r = (ResourceAttributes) super.doGetAttributes(name, attrIds);
            if ( r != null) {
                long cl = r.getContentLength();
                long lm = r.getLastModified();
                String strongETag = String.format("\"%s-%s\"", cl, lm);
                r.setETag(strongETag);
            }
            return r;
        }

    }
    
    private class JarScannerCallbackProxy implements JarScannerCallback 
    {

        private JarScannerCallback wrapped;
        private Logger logger;
            
        public JarScannerCallbackProxy(JarScannerCallback instance,Logger log) 
        {
            wrapped = instance;
            logger = log;
        }
        
        private boolean shouldScan(String name) 
        {
            for (String s:tldScanTargets) {
                if ( name.contains(s)) {
                    return true;
                }
            }
            return false;
        }
        
        public void scan(JarURLConnection urlConn)throws IOException 
        {
            String name = urlConn.getJarFile().getName();
            if ( shouldScan(name)) {
                //logger.info("Scanning " + name);
                wrapped.scan(urlConn);
                return;
            } 
        }

        public void scan(File file) throws IOException 
        {
            String name = file.getName();
            if ( shouldScan(name)) {
                //logger.info("Scanning " + name);
                wrapped.scan(file);
                return;
            } 
        }
    }
}
