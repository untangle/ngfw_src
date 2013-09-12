/*
 * $Id$
 */

package com.untangle.uvm.engine;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final String CERTIFICATE_GENERATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen";
    private static final String ROOT_CRT_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/untangle.crt";
    private static final String ROOT_KEY_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/untangle.key";
    private static final String LOCAL_CSR_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.csr";
    private static final String LOCAL_CRT_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.crt";
    private static final String LOCAL_KEY_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.key";
    private static final String LOCAL_PEM_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.pem";
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    private final Logger logger = Logger.getLogger(getClass());

    protected CertificateManagerImpl()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new ServerCertificateUploadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new RootCertificateDownloadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new CertificateRequestDownloadHandler());

        File keyCheck = new File(ROOT_KEY_FILE);
        File crtCheck = new File(ROOT_CRT_FILE);
        File localKey = new File(LOCAL_KEY_FILE);
        File localCrt = new File(LOCAL_CRT_FILE);
        File localPem = new File(LOCAL_PEM_FILE);

        // in the development environment check to load files from the backup
        // area so we avoid recreating CA/cert/pem after every rake clean
        if (UvmContextFactory.context().isDevel()) {
            logger.info("Restoring dev enviroment CA, crt, key, and pem files...");
            UvmContextFactory.context().execManager().exec("mkdir -p " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/");
            if (!keyCheck.exists()) {
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.key " + ROOT_KEY_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.crt " + ROOT_CRT_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/index.txt* " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/");
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/serial.txt* " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/");
            }
        }

        // if either of the root CA files are missing create the thing now
        if ((crtCheck.exists() == false) || (keyCheck.exists() == false)) {
            logger.info("Creating default root certificate authority");
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " DEFAULT");

            // in the development enviroment save these to a global location
            // so they will survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CRT_FILE + " /etc/untangle/untangle.crt");
                UvmContextFactory.context().execManager().exec("cp -fa " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/index.txt* /etc/untangle/");
                UvmContextFactory.context().execManager().exec("cp -fa " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/serial.txt* /etc/untangle/");
            }
        }

        // now that we know we have a root CA we check for the local
        // apache.pem and create it here if it doesn't yet exist
        if (localPem.exists() == false) {
            String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
            logger.info("Creating default locally signed apache certificate for " + hostName);
            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + hostName);

            // If in the development enviroment save these to a global location
            // so they will
            // survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_PEM_FILE + " /etc/untangle/apache.pem");
            }
        }

        File apachePem = new File(APACHE_PEM_FILE);

        // if the apache.pem in the settings directory is newer than
        // the one used by apache we copy the new file and restart
        if (localPem.lastModified() > apachePem.lastModified()) {
            logger.info("Copying newer apache cert from " + LOCAL_PEM_FILE + " to " + APACHE_PEM_FILE);
            UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
            UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");
        }
    }

    // called by the UI to upload a signed server certificate
    private class ServerCertificateUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "server_cert";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            String certString = new String(fileItem.get());
            int certFlag = 0;

            // look for the required header and trailer stuff
            if (certString.contains("BEGIN CERTIFICATE") == true)
                certFlag++;
            if (certString.contains("END CERTIFICATE") == true)
                certFlag++;

            if (certFlag != 2)
                return new ExecManagerResult(1, "The uploaded file does not contain a valid certificate");

            // first we write the uploaded file to the local CRT file
            File certFile = new File(LOCAL_CRT_FILE);
            FileOutputStream certStream = new FileOutputStream(certFile);
            certStream.write(fileItem.get());
            certStream.close();

            // next we create the local PEM file from the local KEY and CRT
            // files
            UvmContextFactory.context().execManager().exec("cat " + LOCAL_KEY_FILE + " " + LOCAL_CRT_FILE + " > " + LOCAL_PEM_FILE);

            // now copy the local PEM file to the apache directory and restart
            UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
            UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");

            return new ExecManagerResult(0, "Certificate successfully uploaded");
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
            try {
                File certFile = new File(ROOT_CRT_FILE);
                FileInputStream certStream = new FileInputStream(certFile);
                byte[] certData = new byte[(int) certFile.length()];
                certStream.read(certData);
                certStream.close();

                // set the headers.
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=root_authority.crt");

                OutputStream webStream = resp.getOutputStream();
                webStream.write(certData);
            }

            catch (Exception exn) {
                logger.warn("Exception during certificate download", exn);
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
            String argList[] = new String[3];
            argList[0] = "REQUEST"; // create CSR for server
            argList[1] = req.getParameter("arg1"); // cert subject
            argList[2] = req.getParameter("arg2"); // alt names
            String argString = UvmContextFactory.context().execManager().argBuilder(argList);
            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);

            try {
                File certFile = new File(LOCAL_CSR_FILE);
                FileInputStream certStream = new FileInputStream(certFile);
                byte[] certData = new byte[(int) certFile.length()];
                certStream.read(certData);
                certStream.close();

                // set the headers.
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=server_certificate.csr");

                OutputStream webStream = resp.getOutputStream();
                webStream.write(certData);
            }

            catch (Exception exn) {
                logger.warn("Exception during certificate download", exn);
            }
        }
    }

    // called by the UI to get info about the root and server certificates
    public CertificateInformation getCertificateInformation()
    {
        CertificateInformation certInfo = new CertificateInformation();
        FileInputStream certStream;
        X509Certificate certObject;

        try {
            // get an instance of the X509 certificate factory that we can
            // use to create X509Certificates from which we can grab info
            CertificateFactory factory = CertificateFactory.getInstance("X.509");

            // Grab the info from our root CA certificate. We can use the file
            // as is since it only contains the DER cert encoded in Base64
            certStream = new FileInputStream(ROOT_CRT_FILE);
            certObject = (X509Certificate) factory.generateCertificate(certStream);
            certStream.close();

            certInfo.setRootcaDateValid(new Date(certObject.getNotBefore().toString()));
            certInfo.setRootcaDateExpires(new Date(certObject.getNotAfter().toString()));
            certInfo.setRootcaSubject(certObject.getSubjectDN().toString());

            // Now grab the info from the Apache certificate. This is a little
            // more complicated because we have to skip over the private key
            // and look for the certificate portion of the file
            File certFile = new File(APACHE_PEM_FILE);
            certStream = new FileInputStream(certFile);
            byte[] certData = new byte[(int) certFile.length()];
            certStream.read(certData);
            certStream.close();

            // look for the header and trailer strings
            String pemString = new String(certData);
            int certTop = pemString.indexOf("-----BEGIN CERTIFICATE-----");
            int certEnd = pemString.indexOf("-----END CERTIFICATE-----");
            int certLen = (certEnd - certTop + 25);

            // if either certTop or certEnd returned an error something is
            // wrong so just return what we have so far
            if ((certTop < 0) || (certEnd < 0))
                return certInfo;

            // create a new String with just the certificate we isolated
            // and pass it to the certificate factory generatory function
            String certString = new String(pemString.getBytes(), certTop, certLen);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(certString.getBytes());
            certObject = (X509Certificate) factory.generateCertificate(byteStream);

            certInfo.setServerDateValid(new Date(certObject.getNotBefore().toString()));
            certInfo.setServerDateExpires(new Date(certObject.getNotAfter().toString()));
            certInfo.setServerSubject(certObject.getSubjectDN().toString());
            certInfo.setServerIssuer(certObject.getIssuerDN().toString());
        }

        catch (Exception exn) {
            logger.error("Exception in getCertificateInformation()", exn);
        }

        return certInfo;
    }

    // called by the UI to generate a new root certificate authority
    // the dummy argument is not used and makes the JavaScript a little simpler
    public boolean generateCertificateAuthority(String certSubject, String dummy)
    {
        logger.info("Creating new root certificate authority: " + certSubject);
        String argList[] = new String[1];
        argList[0] = certSubject;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + argString);

        // in the development enviroment save these to a global location
        // so they will survive a rake clean
        if (UvmContextFactory.context().isDevel()) {
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CRT_FILE + " /etc/untangle/untangle.crt");
            UvmContextFactory.context().execManager().exec("cp -fa " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/index.txt* /etc/untangle/");
            UvmContextFactory.context().execManager().exec("cp -fa " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/serial.txt* /etc/untangle/");
        }

        return (true);
    }

    // called by the UI to generate a new server certificate
    public boolean generateServerCertificate(String certSubject, String altNames)
    {
        logger.info("Creating new locally signed apache certificate: " + certSubject);

        String argList[] = new String[3];
        argList[0] = "APACHE"; // puts cert file in settings directory
        argList[1] = certSubject;
        argList[2] = altNames;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);

        // now copy the pem file to the apache directory and restart
        UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
        UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");

        return (true);
    }
}
