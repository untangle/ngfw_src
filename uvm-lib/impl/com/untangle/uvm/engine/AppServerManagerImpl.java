/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.catalina.Valve;
import org.apache.log4j.Logger;

import com.untangle.node.util.OpenSSLWrapper;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.networking.AddressSettingsListener;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;
import com.untangle.uvm.util.QuarantineOutsideAccessValve;

/**
 * TODO A work in progress (currently a disorganized mess of crap taken
 * from the old "main" and "TomcatManager" code.
 */
class AppServerManagerImpl implements LocalAppServerManager
{
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private final int externalHttpsPort;

    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContextImpl mctx;
    private final TomcatManagerImpl tomcatManager;

    AppServerManagerImpl(UvmContextImpl mctx)
    {
        this.mctx = mctx;
        //TODO Clean up stuff ported from "main"
        this.tomcatManager = (TomcatManagerImpl) mctx.tomcatManager();

        Properties networkingProperties = new Properties();

        File f = new File(System.getProperty("uvm.conf.dir")
                          + "/uvm.networking.properties");
        if (f.exists()) {
            FileInputStream fis = null;
            try {
                logger.info("Loading uvm.networking.properties from " + f);
                fis = new FileInputStream(f);
                networkingProperties.load(fis);
            } catch (Exception exn) {
                logger.error("", exn);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (Exception exn) {
                        logger.warn("could not close file", exn);
                    }
                }
            }
        }

        int p = DEFAULT_HTTPS_PORT;

        String t = networkingProperties.getProperty("uvm.https.port");
        if (null != t) {
            try {
                p = Integer.parseInt(t);
            } catch (NumberFormatException exn) {
                logger.warn( "Invalid port: " + t);
                p = DEFAULT_HTTPS_PORT;
            }
        } else {
            p = DEFAULT_HTTPS_PORT;
        }

        /* Illegal range */
        if (p <= 0 || p >= 0xFFFF || p == 80 ) {
            p = DEFAULT_HTTPS_PORT;
        }

        this.externalHttpsPort = p;
    }

    public void postInit()
    {
        //TODO check for expiration and call regenCert if expired
        try {
            String disableTomcat = System.getProperty("uvm.devel.notomcat");
            if (null == disableTomcat || !Boolean.valueOf(disableTomcat)) {
                tomcatManager.startTomcat(DEFAULT_HTTP_PORT,
                                          DEFAULT_HTTPS_PORT,
                                          externalHttpsPort,
                                          NetworkUtil.INTERNAL_OPEN_HTTPS_PORT);
            }
        } catch (Exception exn) {
            logger.warn("could not start Tomcat", exn);
        }

        mctx.localNetworkManager().registerListener(new AddressSettingsListener() {
                public void event(AddressSettingsInternal settings)
                {
                    String existingAlias = getCurrentServerCertInfo().getSubjectCN();
                    String currentHostname = settings.getHostName().toString();
                    if (currentHostname == null || currentHostname.equals("")) { return; }
                    if (null == existingAlias
                        || !(existingAlias.equals(currentHostname))) {
                        hostnameChanged(currentHostname);
                    }
                }
            });
    }

    public void rebindExternalHttpsPort(int port) throws Exception
    {
        // XXX IMPLEMENT ME!!!
    }

    public ServletContext loadSystemApp(String urlBase, String rootDir)
    {
        return tomcatManager.loadSystemApp(urlBase, rootDir);
    }

    public ServletContext loadSystemApp(String urlBase, String rootDir,
                                        Valve valve)
    {
        return tomcatManager.loadSystemApp(urlBase, rootDir, valve);
    }

    public ServletContext loadGlobalApp(String urlBase, String rootDir)
    {
        return tomcatManager.loadGlobalApp(urlBase, rootDir);
    }

    public ServletContext loadGlobalApp(String urlBase, String rootDir,
                                        Valve valve)
    {
        return tomcatManager.loadGlobalApp(urlBase, rootDir, valve);
    }

    public ServletContext loadInsecureApp(String urlBase, String rootDir)
    {
        return tomcatManager.loadInsecureApp(urlBase, rootDir);
    }

    public ServletContext loadInsecureApp(String urlBase, String rootDir,
                                          Valve valve)
    {
        return tomcatManager.loadInsecureApp(urlBase, rootDir, valve);
    }

    public ServletContext loadQuarantineApp(String urlBase, String rootDir)
    {
        return tomcatManager.loadInsecureApp(urlBase, rootDir,
                                             new QuarantineOutsideAccessValve());
    }

    public boolean unloadWebApp(String contextRoot)
    {
        return tomcatManager.unloadWebApp(contextRoot);
    }

    public void resetRootWelcome()
    {
        tomcatManager.resetRootWelcome();
    }

    public void setRootWelcome(String welcomeFile)
    {
        tomcatManager.setRootWelcome(welcomeFile);
    }


    public String getRootWelcome()
    {
        return tomcatManager.getRootWelcome();
    }

    //TODO bscott Sometime in the next two years we need a way for them
    //     to roll to a new key while maintaing their existing signed cert.

    public boolean regenCert(RFC2253Name dn, int durationInDays)
    {
        try {
            //The goal of this is to take the existing DN minus the hostname (leaving company, city, state, etc.)
            // and append the current hostname
            String dnString = "/CN="+getFQDN();
            if ( dn != null && dn.toString().length() > 0) {
                dnString = "/"+dn.toString().replace(",","/").replaceAll("/?CN=.*","")+dnString;
            }
            //if the old DN only had CN= then we get two slashes at the beginning so we should handle that case
            dnString = dnString.replace("//","/");
            logger.warn("Generating a new cert with dn " + dnString);
            OpenSSLWrapper.generateSelfSignedCert(dnString, APACHE_PEM_FILE);
            return true;
        } catch (Exception ex) {
            logger.error("Unable to regen cert", ex);
            return false;
        }
    }

    public boolean importServerCert(byte[] cert, byte[] caCert)
    {
        CertInfo localCertInfo = null;

        try {
            localCertInfo = OpenSSLWrapper.getCertInfo(cert);
        } catch (Exception ex) {
            logger.error("Unable to get info from cert", ex);
            return false;
        }

        // This is a hack, but if they don't have a CN what the heck
        // are they doing
        String cn = localCertInfo.getSubjectCN();
        if (null == cn) {
            logger.error("Received a cert without a CN? \"" +
                         new String(cert) + "\"");
            return false;
        }

        String reason = "";
        try {
            OpenSSLWrapper.importCert(cert, caCert, new File(APACHE_PEM_FILE));
        } catch (Exception ex) {
            logger.error(reason, ex);
            return false;
        }

        return true;

    }

    public byte[] getCurrentServerCert()
    {
    // FIXME ? our apache cert only handles one hostname right now
//         String eHost = getFQDN();

//         try {
//             if (!keyStore.containsAlias(eHost)) {
//                 hostnameChanged(eHost);
//             }
//         } catch (Exception ex) {
//             logger.error("Unable to list key store", ex);
//         }

        try {
        // FIXME: not sure about getBytes; probably B. Scott intended to
        // read the cert/key bytes without the header/footer lines, but
        // his code didn't seem to do it: "keytool whatever", which he
        // invokes, does returns the headers, and he doesn't seem to weed
        // them out...
            return OpenSSLWrapper.getCertFromPEM(APACHE_PEM_FILE).getBytes();
        } catch (Exception ex) {
            logger.error("Unable to retreive current cert", ex);
            return null;
        }
    }

    public byte[] generateCSR()
    {
        try {
            byte[] oldCert = getCurrentServerCert();
            CertInfo ci = OpenSSLWrapper.getCertInfo(oldCert);
            RFC2253Name currentDN = ci.subjectDN;
            return OpenSSLWrapper.createCSR("/"+currentDN.toString().replace(",","/"),
                        new File(APACHE_PEM_FILE));
        } catch (Exception ex) {
            logger.error("Exception generating a CSR", ex);
            return null;
        }
    }

    public CertInfo getCertInfo(byte[] certBytes)
    {
        if (certBytes == null) {
            logger.warn("Can not get get info from a null cert");
            return null;
        }

        try {
            return OpenSSLWrapper.getCertInfo(certBytes);
        } catch (Exception ex) {
            logger.error("Unable to get info from cert \"" +
                         new String(certBytes) + "\"", ex);
            return null;
        }
    }

    public CertInfo getCurrentServerCertInfo() {
        return getCertInfo(getCurrentServerCert());
    }

    /**
     * Callback indicating that the hostname has changed
     * @TODO This does nothing for now. Maybe we want to keep it that way
     * but we need to call regenCert from somewhere else
     */
    private void hostnameChanged(String newHostName)
    {
        return;
        //        String reason = "";
        //        try {
        //  reason = "Unable to export current cert";
        //  byte[] oldCert = getCurrentServerCert();
        //  reason = "Unable to get current cert info";
        //  CertInfo ci = OpenSSLWrapper.getCertInfo(oldCert);
        //  RFC2253Name oldDN = ci.subjectDN;
        //  regenCert(oldDN, 365 * 5 +1);
        //} catch (Exception ex) {
        //  logger.error(reason, ex);
        //}
    }

    private String getFQDN()
    {
        String fqdn = mctx.localNetworkManager().getHostname().toString();
        if (fqdn == null || fqdn.equals("")) {
            return "untangle.example.com";
        }
        return fqdn;
    }
}
