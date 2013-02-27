/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.CertInfo;
import com.untangle.uvm.DistinguishedName;
import com.untangle.node.util.OpenSSLWrapper;

/**
 * TODO A work in progress (currently a disorganized mess of crap taken
 * from the old "main" and "TomcatManager" code.
 */
class CertificateManagerImpl implements CertificateManager
{
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContextImpl uvmContext;
    private final TomcatManagerImpl tomcatManager;

    CertificateManagerImpl(UvmContextImpl uvmContext)
    {
        this.uvmContext = uvmContext;
        this.tomcatManager = (TomcatManagerImpl) uvmContext.tomcatManager();
    }

    public void postInit()
    {
        UvmRepositorySelector.instance().setLoggingUvm();

        try {
            tomcatManager.startTomcat();
        } catch (Exception exn) {
            logger.warn("could not start Tomcat", exn);
        }
    }

    public boolean regenCert(DistinguishedName dn, int durationInDays)
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
        try {
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
            DistinguishedName currentDN = ci.subjectDN;
            return OpenSSLWrapper.createCSR("/"+currentDN.toString().replace(",","/"), new File(APACHE_PEM_FILE));
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

    private String getFQDN()
    {
        String fqdn = uvmContext.networkManager().getNetworkSettings().getHostName();
        if (fqdn == null || fqdn.equals("")) {
            return "example.com";
        }
        return fqdn;
    }
}
