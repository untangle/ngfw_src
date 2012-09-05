/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.TomcatManager;
import com.untangle.uvm.util.AdministrationOutsideAccessValve;
import com.untangle.uvm.util.ReportingOutsideAccessValve;

/**
 * Wrapper around the Tomcat server embedded within the UVM.
 */
public class TomcatManagerImpl implements TomcatManager
{
    private static final int  TOMCAT_NUM_RETRIES = 15; //  5 minutes total
    private static final long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    private static final int  TOMCAT_MAX_POST_SIZE = 16777216; // 16MB
    private static final String WELCOME_URI = "/setup/welcome.do";
    private static final String WELCOME_FILE = System.getProperty("uvm.home") + "/apache2/conf.d/homepage.conf";

    private final Logger logger = Logger.getLogger(getClass());

    private final Embedded emb;
    private final StandardHost baseHost;
    private final String webAppRoot;

    private String keystoreFile = "conf/keystore";
    private String keystorePass = "changeit";
    private String keyAlias     = "tomcat";


    // constructors -----------------------------------------------------------

    protected TomcatManagerImpl(UvmContextImpl uvmContext, InheritableThreadLocal<HttpServletRequest> threadRequest, String catalinaHome, String webAppRoot, String logDir)
    {
        this.webAppRoot = webAppRoot;

        ClassLoader uvmCl = Thread.currentThread().getContextClassLoader();
        ClassLoader tomcatParent = uvmCl;

        try {
            // Entering Tomcat ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(tomcatParent);

            // jdi 8/30/04 -- canonical host name depends on ordering of
            // /etc/hosts
            String hostname = "localhost";

            emb = new Embedded();
            emb.setCatalinaHome(catalinaHome);

            // create an Engine
            Engine baseEngine = emb.createEngine();

            // set Engine properties
            baseEngine.setName("tomcat");
            baseEngine.setDefaultHost(hostname);
            baseEngine.setParentClassLoader(tomcatParent);

            // create Host
            baseHost = (StandardHost)emb.createHost(hostname, webAppRoot);
            baseHost.setUnpackWARs(true);
            baseHost.setDeployOnStartup(true);
            baseHost.setAutoDeploy(true);
            baseHost.setErrorReportValveClass("com.untangle.uvm.engine.UvmErrorReportValve");
            OurSingleSignOn ourSsoWorkaroundValve = new OurSingleSignOn();
            SingleSignOn ssoValve = new SpecialSingleSignOn(uvmContext, "", "/reports", "/library" );

            // ssoValve.setRequireReauthentication(true);
            baseHost.getPipeline().addValve(ourSsoWorkaroundValve);
            baseHost.getPipeline().addValve(ssoValve);

            // add host to Engine
            baseEngine.addChild(baseHost);

            // add new Engine to set of
            // Engine for embedded server
            emb.addEngine(baseEngine);

            loadSystemApp("/blockpage", "blockpage");
            loadSystemApp("/alpaca", "alpaca", new WebAppOptions(true, new AdministrationOutsideAccessValve()));
            ServletContext ctx = loadSystemApp("/webui", "webui", new WebAppOptions(new AdministrationOutsideAccessValve()));
            ctx.setAttribute("threadRequest", threadRequest);

            ctx = loadSystemApp("/setup", "setup", new WebAppOptions(new AdministrationOutsideAccessValve()));
            ctx.setAttribute("threadRequest", threadRequest);
            loadSystemApp("/library", "library", new WebAppOptions(true,new AdministrationOutsideAccessValve()));
            
        } finally {
            Thread.currentThread().setContextClassLoader(uvmCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        writeWelcomeFile();
    }

    // package protected methods ----------------------------------------------

    String getKeystoreFileName()
    {
        return this.keystoreFile;
    }

    String getKeystorePassword()
    {
        return this.keystorePass;
    }

    String getKeyAlias()
    {
        return this.keyAlias;
    }

    /**
     * Method sets the security info (cert) for this Tomcat.  If the
     * server is already started, this triggers a reset of the HTTPS
     * sockets.
     *
     * @param ksFile the KeyStore file
     * @param ksPass the password for the keystore file
     * @param ksAlias the alias within the KeyStore file
     */
    void setSecurityInfo(String ksFile, String ksPass, String ksAlias) throws Exception
    {
        this.keystoreFile = ksFile;
        this.keystorePass = ksPass;
        this.keyAlias = ksAlias;
    }

    ServletContext loadSystemApp(String urlBase, String rootDir, WebAppOptions options)
    {
        return loadWebApp(urlBase, rootDir, null, null, options);
    }

    public ServletContext loadSystemApp(String urlBase, String rootDir, Valve valve)
    {
        return loadSystemApp(urlBase, rootDir, new WebAppOptions(true, valve));
    }

    ServletContext loadSystemApp(String urlBase, String rootDir) {
        return loadSystemApp(urlBase, rootDir, new WebAppOptions());
    }

    ServletContext loadGlobalApp(String urlBase, String rootDir, WebAppOptions options)
    {
        return loadWebApp(urlBase, rootDir, null, null, options);
    }

    ServletContext loadGlobalApp(String urlBase, String rootDir, Valve valve)
    {
        return loadGlobalApp(urlBase, rootDir, new WebAppOptions(true, valve));
    }

    ServletContext loadGlobalApp(String urlBase, String rootDir)
    {
        return loadGlobalApp(urlBase, rootDir, new WebAppOptions());
    }

    public ServletContext loadInsecureApp(String urlBase, String rootDir)
    {
        return loadWebApp(urlBase, rootDir, null, null);
    }

    public ServletContext loadInsecureApp(String urlBase, String rootDir, Valve valve)
    {
        return loadWebApp(urlBase, rootDir, null, null, valve);
    }

    public boolean unloadWebApp(String contextRoot)
    {
        try {
            if (null != baseHost) {
                Container c = baseHost.findChild(contextRoot);
                if (null != c) {
                    logger.info("Removing web app " + contextRoot);
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
            w.write("RedirectMatch 302 ^/index.html " + WELCOME_URI + "\n");
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

    synchronized void startTomcat() throws Exception
    {
        Connector jkConnector = new Connector("org.apache.jk.server.JkCoyoteHandler");
        jkConnector.setProperty("port", "8009");
        jkConnector.setProperty("address", "127.0.0.1");
        jkConnector.setProperty("tomcatAuthentication", "false");
        jkConnector.setMaxPostSize(TOMCAT_MAX_POST_SIZE);
        jkConnector.setMaxSavePostSize(TOMCAT_MAX_POST_SIZE);

        String secret = getSecret();
        if (null != secret) {
            jkConnector.setProperty("request.secret", secret);
        }
        emb.addConnector(jkConnector);

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
                                    logger.warn("could not start Tomcat (address in use), sleeping 20 and trying again");
                                    Thread.sleep(TOMCAT_SLEEP_TIME);
                                    try {
                                        emb.stop();
                                    } catch (LifecycleException exn) {
                                        logger.warn(exn, exn);
                                    }
                                    emb.start();
                                    logger.info("Tomcat successfully started");
                                    break;
                                } catch (InterruptedException x) {
                                    logger.warn( "Interrupted while trying to start tomcat, returning.", x);
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
                // Something else, just die die die.
                logger.warn("Exception starting Tomcat",exn);
                throw exn;
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
            this(false, DEFAULT_SESSION_TIMEOUT);
        }

        WebAppOptions(Valve valve) {
            this(false, valve);
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
    private synchronized ServletContext loadWebApp(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, WebAppOptions options)
    {
        return loadWebAppImpl(urlBase, rootDir, realm, auth, options);
    }

    private ServletContext loadWebApp(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth)
    {
        return loadWebApp(urlBase, rootDir, realm, auth, new WebAppOptions());
    }

    private ServletContext loadWebApp(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, Valve valve)
    {
        return loadWebApp(urlBase, rootDir, realm, auth, new WebAppOptions(valve));
    }

    private ServletContext loadWebAppImpl(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth, WebAppOptions options)
    {
        String fqRoot = webAppRoot + "/" + rootDir;

        logger.info("Adding web app " + fqRoot);
        try {
            StandardContext ctx = (StandardContext)emb.createContext(urlBase, fqRoot);
            if (options.allowLinking)
                ctx.setAllowLinking(true);
            ctx.setCrossContext(true);
            ctx.setSessionTimeout(options.sessionTimeout);
            if (null != realm) {
                ctx.setRealm(realm);
            }

            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);

            ctx.setResources(new StrongETagDirContext());

            /* This should be the first valve */
            if (null != options.valve) ctx.addValve(options.valve);

            if (null != auth) {
                Pipeline pipe = ctx.getPipeline();
                auth.setDisableProxyCaching(false);
                pipe.addValve(auth);
            }
            baseHost.addChild(ctx);

            return ctx.getServletContext();
        } catch(Exception ex) {
            logger.error("Unable to deploy webapp \"" + urlBase + "\" from directory \"" + fqRoot + "\"", ex);
            return null;
        }
    }

    private boolean isAIUExn(LifecycleException exn)
    {
        Throwable wrapped = exn.getThrowable();
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
        String bh = System.getProperty("uvm.home");
        if (null == bh) {
            bh = "/usr/share/untangle";
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter("/etc/apache2/untangle-conf.d/uvm.conf");
            fw.write("Include " + bh + "/apache2/conf.d/*.conf\n");
        } catch (IOException exn) {
            logger.warn("could not write includes: conf.d");
        } finally {
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException exn) {
                    logger.warn("could not close file", exn);
                }
            }
        }

        try {
            fw = new FileWriter("/etc/apache2/untangle-unrestricted-conf.d/uvm.conf");
            fw.write("Include " + bh + "/apache2/unrestricted-conf.d/*.conf\n");
        } catch (IOException exn) {
            logger.warn("could not write includes: conf.d");
        } finally {
            if (null != fw) {
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
}
