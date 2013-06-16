/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.io.ByteArrayInputStream;
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
    private static final String CERTIFICATE_GENERATOR_SCRIPT = "/usr/share/untangle/bin/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = "/usr/share/untangle/bin/ut-rootgen";
    private static final String ROOT_CERT_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.crt";
    private static final String ROOT_KEY_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.key";
    private static final String LOCAL_PEM_FILE = "/usr/share/untangle/settings/untangle-certificates/apache.pem";
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private final Logger logger = Logger.getLogger(getClass());

    protected CertificateManagerImpl()
    {
        File certCheck = new File(ROOT_CERT_FILE);
        File keyCheck = new File(ROOT_KEY_FILE);

        // if either of the root CA files are missing create the thing now
        if ((certCheck.exists() == false) || (keyCheck.exists() == false))
        {
            logger.info("Creating new root certificate authority");
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " DEFAULT");
        }
    }

    public CertificateInformation getCertificateInformation()
    {
        CertificateInformation certInfo = new CertificateInformation();
        FileInputStream certStream;
        X509Certificate certObject;

        try
        {
            // get an instance of the X509 certificate factory that we can
            // use to create X509Certificates from which we can grab info
            CertificateFactory factory = CertificateFactory.getInstance("X.509");


            // Grab the info from our root CA certificate.  We can use the file
            // as is since it only contains the DER cert encoded in Base64
            certStream = new FileInputStream(ROOT_CERT_FILE);
            certObject = (X509Certificate)factory.generateCertificate(certStream);
            certStream.close();

            certInfo.setRootcaDateValid(new Date(certObject.getNotBefore().toString()));
            certInfo.setRootcaDateExpires(new Date(certObject.getNotAfter().toString()));
            certInfo.setRootcaSubject(certObject.getSubjectDN().toString());

            // Now grab the info from the Apache certificate.  This is a little
            // more complicated because we have to skip over the private key
            // and look for the certificate portion of the file
            File certFile = new File(APACHE_PEM_FILE);
            certStream = new FileInputStream(certFile);
            byte[] fileData = new byte[(int)certFile.length()];
            certStream.read(fileData);
            certStream.close();

            // look for the header and trailer strings
            String pemString = new String(fileData);
            int certTop = pemString.indexOf("-----BEGIN CERTIFICATE-----");
            int certEnd = pemString.indexOf("-----END CERTIFICATE-----");
            int certLen = (certEnd - certTop + 25);

            // if either certTop or certEnd returned an error something is
            // wrong so just return what we have so far
            if ((certTop < 0) || (certEnd < 0)) return certInfo;

            // create a new String with just the certificate we isolated
            // and pass it to the certificate factory generatory function
            String certString = new String(pemString.getBytes(),certTop,certLen);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(certString.getBytes());
            certObject = (X509Certificate)factory.generateCertificate(byteStream);

            certInfo.setServerDateValid(new Date(certObject.getNotBefore().toString()));
            certInfo.setServerDateExpires(new Date(certObject.getNotAfter().toString()));
            certInfo.setServerSubject(certObject.getSubjectDN().toString());
            certInfo.setServerIssuer(certObject.getIssuerDN().toString());
        }

        catch (Exception exn)
        {
            logger.error("Exception in getCertificateInformation()",exn);
        }

        return certInfo;
    }

    public boolean generateServerCertificate(String certSubject)
    {
        logger.info("Creating locally signed apache certificate: " + certSubject);

        File apacheFile = new File(APACHE_PEM_FILE);
        File localFile = new File(LOCAL_PEM_FILE);

        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE " + certSubject);
        UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
        UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");

        return(true);
    }
}
