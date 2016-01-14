/*
 * $Id$
 */

package com.untangle.uvm;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;

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

public class CertificateManagerImpl implements CertificateManager
{
    private static final String CERTIFICATE_GENERATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen";
    private static final String ROOT_CA_INSTALLER_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen-installer";

    private static final String ROOT_CERT_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/untangle.crt";
    private static final String ROOT_KEY_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/untangle.key";
    private static final String LOCAL_REQUEST_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.csr";
    private static final String LOCAL_TEMP_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.tmp";
    private static final String LOCAL_CERT_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.crt";
    private static final String LOCAL_KEY_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.key";
    private static final String LOCAL_PEM_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.pem";
    private static final String LOCAL_PFX_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/apache.pfx";
    private static final String ROOT_CERT_INSTALLER_FILE = System.getProperty("uvm.settings.dir") + "/untangle-certificates/RootCAInstaller.exe";
    private static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";

    File rootCertFile = new File(ROOT_CERT_FILE);
    File rootKeyFile = new File(ROOT_KEY_FILE);
    File localRequestFile = new File(LOCAL_REQUEST_FILE);
    File localTempFile = new File(LOCAL_TEMP_FILE);
    File localCertFile = new File(LOCAL_CERT_FILE);
    File localKeyFile = new File(LOCAL_KEY_FILE);
    File localPemFile = new File(LOCAL_PEM_FILE);
    File apachePemFile = new File(APACHE_PEM_FILE);
    File rootCertInstallerFile = new File(ROOT_CERT_INSTALLER_FILE);

    private static final String MARKER_CERT_HEAD = "-----BEGIN CERTIFICATE-----";
    private static final String MARKER_CERT_TAIL = "-----END CERTIFICATE-----";
    private static final String MARKER_RKEY_HEAD = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String MARKER_RKEY_TAIL = "-----END RSA PRIVATE KEY-----";
    private static final String MARKER_GKEY_HEAD = "-----BEGIN PRIVATE KEY-----";
    private static final String MARKER_GKEY_TAIL = "-----END PRIVATE KEY-----";

    private final Logger logger = Logger.getLogger(getClass());
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");

    // This is the list of subject alternative name types we extract from
    // the server certificate and use when generating our fake certificate
    private static HashMap<Integer, String> validAlternateList = new HashMap<Integer, String>();

    static {
        validAlternateList.put(0x01, "email");
        validAlternateList.put(0x02, "DNS");
        validAlternateList.put(0x06, "URI");
        validAlternateList.put(0x07, "IP");
        validAlternateList.put(0x08, "RID");
    }

    protected CertificateManagerImpl()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new ServerCertificateUploadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new RootCertificateDownloadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new RootCertificateInstallerDownloadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new CertificateRequestDownloadHandler());

        // in the development environment check to load files from the backup
        // area so we avoid recreating CA/cert/pem after every rake clean
        if (UvmContextFactory.context().isDevel()) {
            logger.info("Restoring dev enviroment CA certificate and key files");
            UvmContextFactory.context().execManager().exec("mkdir -p " + System.getProperty("uvm.settings.dir") + "/untangle-certificates/");
            if (!rootKeyFile.exists()) {
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.key " + ROOT_KEY_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.crt " + ROOT_CERT_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.pem " + LOCAL_PEM_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.key " + LOCAL_KEY_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.crt " + LOCAL_CERT_FILE);
            }
        }

        // if either of the root CA files are missing create the thing now
        if ((rootCertFile.exists() == false) || (rootKeyFile.exists() == false)) {
            logger.info("Creating default root certificate authority");
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + " DEFAULT");
            UvmContextFactory.context().execManager().exec(ROOT_CA_INSTALLER_SCRIPT);

            // in the development enviroment save these to a global location
            // so they will survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CERT_FILE + " /etc/untangle/untangle.crt");
            }
        }
        // Always perform a check for the root installer.  It will determine if it needs to be rebuilt.
        UvmContextFactory.context().execManager().exec(ROOT_CA_INSTALLER_SCRIPT + " check");

        // we should have a root CA at this point so we check the local apache
        // cert files and create them now if either is missing
        if ((localCertFile.exists() == false) || (localKeyFile.exists() == false)) {
            String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
            logger.info("Creating default locally signed apache certificate for " + hostName);
            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + hostName);
        }

        // always make sure the local PFX file exists and is up to date
        UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:password -out " + LOCAL_PFX_FILE + " -in " + LOCAL_PEM_FILE);

        // if the apache.pem in the settings directory is newer than
        // the one used by apache we copy the new file and restart
        if (localPemFile.lastModified() > apachePemFile.lastModified()) {
            logger.info("Copying newer apache cert from " + LOCAL_PEM_FILE + " to " + APACHE_PEM_FILE);
            UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
            UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");

            // in the development enviroment save these to a global location
            // so they will survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_PEM_FILE + " /etc/untangle/apache.pem");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_KEY_FILE + " /etc/untangle/apache.key");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_CERT_FILE + " /etc/untangle/apache.crt");
            }
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
            FileOutputStream certStream;
            int certLen = 0;
            int keyLen = 0;

            int certTop = certString.indexOf(MARKER_CERT_HEAD);
            int certEnd = certString.indexOf(MARKER_CERT_TAIL);

            // if both cert markers found then calculate the length
            if ((certTop >= 0) && (certEnd >= 0)) certLen = (certEnd - certTop + MARKER_CERT_TAIL.length());

            if (certLen == 0) return new ExecManagerResult(1, "The uploaded file does not contain a valid certificate");

            int keyTop = certString.indexOf(MARKER_RKEY_HEAD);
            int keyEnd = certString.indexOf(MARKER_RKEY_TAIL);

            // if both key markers found then calculate the length
            if ((keyTop >= 0) && (keyEnd >= 0)) {
                keyLen = (keyEnd - keyTop + MARKER_RKEY_TAIL.length());
            }

            // didn't find the RSA style so check for generic format
            else {
                keyTop = certString.indexOf(MARKER_GKEY_HEAD);
                keyEnd = certString.indexOf(MARKER_GKEY_TAIL);

                if ((keyTop >= 0) && (keyEnd >= 0)) {
                    keyLen = (keyEnd - keyTop + MARKER_GKEY_TAIL.length());
                }
            }

            // if the uploaded file only contains a cert then we combine
            // with the existing private key and create a new pem file
            if (keyLen == 0) {
                // start by writing the uploaded cert to a temporary file
                certStream = new FileOutputStream(localTempFile);
                certStream.write(certString.getBytes(), certTop, certLen);
                certStream.close();

                // Make sure the cert they uploaded matches our private key 
                String certMod = UvmContextFactory.context().execManager().execOutput("openssl x509 -noout -modulus -in " + LOCAL_TEMP_FILE);
                logger.info(LOCAL_TEMP_FILE + certMod);
                String keyMod = UvmContextFactory.context().execManager().execOutput("openssl rsa -noout -modulus -in " + LOCAL_KEY_FILE);
                logger.info(LOCAL_KEY_FILE + keyMod);
                UvmContextFactory.context().execManager().exec("rm -f " + LOCAL_TEMP_FILE);

                // if they cert and key modulus don't match then it's garbage 
                if (certMod.compareTo(keyMod) != 0) {
                    return new ExecManagerResult(1, "The public key in the uploaded file does not match the server private key");
                }

                // the cert and key match so save the cert to the local file
                certStream = new FileOutputStream(localCertFile);
                certStream.write(certString.getBytes(), certTop, certLen);
                certStream.close();

                // next create the local PEM file from the local KEY and CRT files
                UvmContextFactory.context().execManager().exec("cat " + LOCAL_KEY_FILE + " " + LOCAL_CERT_FILE + " > " + LOCAL_PEM_FILE);
            }

            // we have a cert and a key and maybe other stuff so we just
            // use the uploaded file exactly as provided
            else {
                certStream = new FileOutputStream(localPemFile);
                certStream.write(fileItem.get());
                certStream.close();
            }

            // now convert the local PEM file to the local PFX file for apps
            // that use SSLEngine like web filter and captive portal
            UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:password -out " + LOCAL_PFX_FILE + " -in " + LOCAL_PEM_FILE);

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
                FileInputStream certStream = new FileInputStream(rootCertFile);
                byte[] certData = new byte[(int) rootCertFile.length()];
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

    // called by the UI to download the root CA certificate file
    private class RootCertificateInstallerDownloadHandler implements DownloadHandler
    {
        @Override
        public String getName()
        {
            return "root_certificate_installer_download";
        }

        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            try {
                FileInputStream certInstallerStream = new FileInputStream(rootCertInstallerFile);
                byte[] certInstallerData = new byte[(int) rootCertInstallerFile.length()];
                certInstallerStream.read(certInstallerData);
                certInstallerStream.close();

                // set the headers.
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=RootCAInstaller.exe");

                OutputStream webStream = resp.getOutputStream();
                webStream.write(certInstallerData);
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
                FileInputStream certStream = new FileInputStream(localRequestFile);
                byte[] certData = new byte[(int) localRequestFile.length()];
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
            certStream = new FileInputStream(ROOT_CERT_FILE);
            certObject = (X509Certificate) factory.generateCertificate(certStream);
            certStream.close();

            certInfo.setRootcaDateValid(simpleDateFormat.parse(certObject.getNotBefore().toString()));
            certInfo.setRootcaDateExpires(simpleDateFormat.parse(certObject.getNotAfter().toString()));
            certInfo.setRootcaSubject(certObject.getSubjectDN().toString());

            // Now grab the info from the Apache certificate. This is a little
            // more complicated because we have to skip over the private key
            // and look for the certificate portion of the file
            certStream = new FileInputStream(apachePemFile);
            byte[] certData = new byte[(int) apachePemFile.length()];
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
            String certString = new String(pemString.getBytes(), certTop, certLen);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(certString.getBytes());
            certObject = (X509Certificate) factory.generateCertificate(byteStream);

            certInfo.setServerDateValid(simpleDateFormat.parse(certObject.getNotBefore().toString()));
            certInfo.setServerDateExpires(simpleDateFormat.parse(certObject.getNotAfter().toString()));
            certInfo.setServerSubject(certObject.getSubjectDN().toString());
            certInfo.setServerIssuer(certObject.getIssuerDN().toString());

            // The SAN list is stored as a collection of List's where the
            // first entry is an Integer indicating the type of name and the
            // second entry is the String holding the actual name
            Collection<List<?>> altNames = certObject.getSubjectAlternativeNames();
            StringBuilder nameList = new StringBuilder(1024);

            if (altNames != null) {
                Iterator<List<?>> iterator = altNames.iterator();

                while (iterator.hasNext()) {
                    List<?> entry = iterator.next();
                    int value = ((Integer) entry.get(0)).intValue();

                    // check the entry type against the list we understand
                    if (validAlternateList.containsKey(value) == false) continue;

                    // use the name string from our hashmap along with the
                    // value from the certificate to build our SAN list
                    if (nameList.length() != 0) nameList.append(",");
                    nameList.append(validAlternateList.get(value) + ":" + entry.get(1).toString());
                }
                certInfo.setServerNames(nameList.toString());
            }
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
        UvmContextFactory.context().execManager().exec(ROOT_CA_INSTALLER_SCRIPT + argString);

        // in the development enviroment save these to a global location
        // so they will survive a rake clean
        if (UvmContextFactory.context().isDevel()) {
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CERT_FILE + " /etc/untangle/untangle.crt");
        }

        return (true);
    }

    // called by the UI to generate a new server certificate
    public boolean generateServerCertificate(String certSubject, String altNames)
    {
        logger.info("Creating new locally signed apache certificate: " + certSubject);

        String argList[] = new String[3];
        argList[0] = "APACHE"; // puts cert file and key in settings directory
        argList[1] = certSubject;
        argList[2] = altNames;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);

        // now convert the local PEM file to the local PFX file for apps
        // that use SSLEngine like web filter and captive portal
        UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:password -out " + LOCAL_PFX_FILE + " -in " + LOCAL_PEM_FILE);

        // now copy the pem file to the apache directory and restart
        UvmContextFactory.context().execManager().exec("cp " + LOCAL_PEM_FILE + " " + APACHE_PEM_FILE);
        UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");

        // in the development enviroment save these to a global location
        // so they will survive a rake clean
        if (UvmContextFactory.context().isDevel()) {
            UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_PEM_FILE + " /etc/untangle/apache.pem");
            UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_KEY_FILE + " /etc/untangle/apache.key");
            UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_CERT_FILE + " /etc/untangle/apache.crt");
        }
        
        return (true);
    }
}
