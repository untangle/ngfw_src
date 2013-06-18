/**
 * $Id$
 */

package com.untangle.uvm.engine;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import com.untangle.uvm.CertificateInformation;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.DownloadHandler;

@SuppressWarnings("deprecation")
public class CertificateManagerImpl implements CertificateManager
{
    private static final String CERTIFICATE_GENERATOR_SCRIPT = "/usr/share/untangle/bin/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = "/usr/share/untangle/bin/ut-rootgen";
    private static final String SERVER_CSR_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.csr";
    private static final String ROOT_CERT_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.crt";
    private static final String ROOT_KEY_FILE = "/usr/share/untangle/settings/untangle-certificates/untangle.key";
    private static final String LOCAL_PEM_FILE = "/usr/share/untangle/settings/untangle-certificates/apache.pem";
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private final Logger logger = Logger.getLogger(getClass());

    protected CertificateManagerImpl()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler( new ServerCertificateUploadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new RootCertificateDownloadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new CertificateRequestDownloadHandler() );

        File certCheck = new File(ROOT_CERT_FILE);
        File keyCheck = new File(ROOT_KEY_FILE);

        // if either of the root CA files are missing create the thing now
        if ((certCheck.exists() == false) || (keyCheck.exists() == false))
        {
            logger.info("Creating default root certificate authority");
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " DEFAULT");
        }
    }

    // called by the UI to upload a signed server certificate
    private class ServerCertificateUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "server_certificate_upload";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            logger.info("CERT_UPLOAD FILE=" + fileItem.getName() + " ARG=" + argument);
            return new ExecManagerResult(0,"Whatever buddy... whatever.");
        }
    }

    // called by the UI to download the root CA certificate file
    private class RootCertificateDownloadHandler implements DownloadHandler
    {
        @Override
        public String getName()
        {
            return "root_certificate_download";
        }

        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            try
            {
            File certFile = new File(ROOT_CERT_FILE);
            FileInputStream certStream = new FileInputStream(certFile);
            byte[] certData = new byte[(int)certFile.length()];
            certStream.read(certData);
            certStream.close();

            // set the headers.
            resp.setContentType("application/x-download");
            resp.setHeader("Content-Disposition", "attachment; filename=root_authority.crt");

            OutputStream webStream = resp.getOutputStream();
            webStream.write(certData);
            }

            catch (Exception exn)
            {
            logger.warn("Exception during certificate download",exn);
            }
        }
    }

    // called by the UI to generate and download a certificate signing request
    private class CertificateRequestDownloadHandler implements DownloadHandler
    {
        @Override
        public String getName()
        {
            return "certificate_request_download";
        }

        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            String certSubject = req.getParameter("arg1");

            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " REQUEST " + certSubject);

            try
            {
            File certFile = new File(SERVER_CSR_FILE);
            FileInputStream certStream = new FileInputStream(certFile);
            byte[] certData = new byte[(int)certFile.length()];
            certStream.read(certData);
            certStream.close();

            // set the headers.
            resp.setContentType("application/x-download");
            resp.setHeader("Content-Disposition", "attachment; filename=server_certificate.csr");

            OutputStream webStream = resp.getOutputStream();
            webStream.write(certData);
            }

            catch (Exception exn)
            {
            logger.warn("Exception during certificate download",exn);
            }
        }
    }

// ----------------------------------------------------------------------------
// Public functions called by the administration.js certificates tab
// ----------------------------------------------------------------------------

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
            byte[] certData = new byte[(int)certFile.length()];
            certStream.read(certData);
            certStream.close();

            // look for the header and trailer strings
            String pemString = new String(certData);
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

    public boolean generateCertificateAuthority(String certSubject)
    {
        logger.info("Creating new root certificate authority: " + certSubject);
        UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " " + certSubject);
        return(true);
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
