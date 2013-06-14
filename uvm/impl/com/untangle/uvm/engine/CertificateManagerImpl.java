/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.io.FileInputStream;
import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;
import com.untangle.uvm.CertificateInformation;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;

@SuppressWarnings("deprecation")
public class CertificateManagerImpl implements CertificateManager
{
    private static final String ROOT_CA_CREATOR_SCRIPT = "/usr/share/untangle/bin/ut-rootgen";
    private static final String ROOT_CERT_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.crt";
    private static final String ROOT_KEY_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.key";
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private final Logger logger = Logger.getLogger(getClass());

    protected CertificateManagerImpl()
    {
        // make sure the root CA files exist since we need this to
        // generate our on fake certificates on the fly
        File certCheck = new File(ROOT_CERT_FILE);
        File keyCheck = new File(ROOT_KEY_FILE);

        if ((certCheck.exists() != true) || (keyCheck.exists() != true))
        {
            logger.info("Creating new root certificate authority");
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " DEFAULT");
        }
    }

    public CertificateInformation getCertificateInformation()
    {
        CertificateInformation certInfo = new CertificateInformation();
        
        try
        {
            // get an instance of the X509 certificate factory
            CertificateFactory factory = CertificateFactory.getInstance("X.509");


            File certFile = new File(ROOT_CERT_FILE);
            FileInputStream certStream = new FileInputStream(certFile);
            X509Certificate certObject = (X509Certificate)factory.generateCertificate(certStream);

            certInfo.setRootcaDateValid(new Date(certObject.getNotBefore().toString()));
            certInfo.setRootcaDateExpires(new Date(certObject.getNotBefore().toString()));
            certInfo.setRootcaSubject(certObject.getSubjectDN().toString());
        }

        catch (Exception exn)
        {
            logger.error("Exception in getCertificateInformation()",exn);
        }

        return certInfo;
    }
}
