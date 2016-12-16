/*
 * $Id$
 */

package com.untangle.uvm;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import com.untangle.uvm.CertificateInformation;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;

public class CertificateManagerImpl implements CertificateManager
{
    private static final String CERTIFICATE_GENERATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen";
    private static final String ROOT_CERT_INSTALLER_FILE = CERT_STORE_PATH + "RootCAInstaller.exe";

    private static final String CERTIFICATE_UPLOAD_FILE = CERT_STORE_PATH + "upload.crt";
    private static final String EXTERNAL_REQUEST_FILE = CERT_STORE_PATH + "request.csr";

    private static final String ROOT_CERT_FILE = CERT_STORE_PATH + "untangle.crt";
    private static final String ROOT_KEY_FILE = CERT_STORE_PATH + "untangle.key";

    private static final String LOCAL_CSR_FILE = CERT_STORE_PATH + "apache.csr";
    private static final String LOCAL_CRT_FILE = CERT_STORE_PATH + "apache.crt";
    private static final String LOCAL_KEY_FILE = CERT_STORE_PATH + "apache.key";
    private static final String LOCAL_PEM_FILE = CERT_STORE_PATH + "apache.pem";
    private static final String LOCAL_PFX_FILE = CERT_STORE_PATH + "apache.pfx";

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
        File rootCertFile = new File(ROOT_CERT_FILE);
        File rootKeyFile = new File(ROOT_KEY_FILE);

        UvmContextFactory.context().servletFileManager().registerUploadHandler(new ServerCertificateUploadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new RootCertificateDownloadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new RootCertificateInstallerDownloadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new CertificateRequestDownloadHandler());

        // in the development environment if the root CA files are missing copy
        // them from the backup area to avoid recreating after every rake clean
        if (UvmContextFactory.context().isDevel()) {
            logger.info("Restoring dev enviroment CA certificate and key files");
            UvmContextFactory.context().execManager().exec("mkdir -p " + CERT_STORE_PATH);
            if (!rootKeyFile.exists()) {
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.key " + ROOT_KEY_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/untangle.crt " + ROOT_CERT_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.crt " + LOCAL_CSR_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.crt " + LOCAL_CRT_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.key " + LOCAL_KEY_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.pem " + LOCAL_PEM_FILE);
                UvmContextFactory.context().execManager().exec("cp -fa /etc/untangle/apache.pfx " + LOCAL_PFX_FILE);
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

        // always perform a check for the root installer.  It will determine if it needs to be rebuilt.
        UvmContextFactory.context().execManager().exec(ROOT_CA_INSTALLER_SCRIPT + " check");

        // we should have a root CA at this point so we check the local apache
        // cert files and create them now if either is missing
        File localCertFile = new File(LOCAL_CRT_FILE);
        File localKeyFile = new File(LOCAL_KEY_FILE);
        if ((localCertFile.exists() == false) || (localKeyFile.exists() == false)) {
            String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
            logger.info("Creating default locally signed apache certificate for " + hostName);
            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + hostName);
        }

        // Get the fingerprint for the configured web cert and the active
        // apache cert.  If they don't match we copy the configured cert to
        // the apache directory and restart the server to activate the cert. 
        String apacheFingerprint = UvmContextFactory.context().execManager().execOutput("openssl x509 -noout -fingerprint -in " + APACHE_PEM_FILE);
        String configFingerprint = UvmContextFactory.context().execManager().execOutput("openssl x509 -noout -fingerprint -in " + CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate());
        if (apacheFingerprint.equals(configFingerprint) == false) {
            logger.info("Replacing existing apache certificate [" + apacheFingerprint + "] with configured certificate [" + configFingerprint + "]");
            UvmContextFactory.context().systemManager().activateApacheCertificate();

            // in the development enviroment save these to a global location
            // so they will survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_CSR_FILE + " /etc/untangle/apache.csr");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_CRT_FILE + " /etc/untangle/apache.crt");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_KEY_FILE + " /etc/untangle/apache.key");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_PEM_FILE + " /etc/untangle/apache.pem");
                UvmContextFactory.context().execManager().exec("cp -fa " + LOCAL_PFX_FILE + " /etc/untangle/apache.pfx");
            }
        }
    }

    // called by the UI to upload server certificates
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
            String baseName = Long.toString(System.currentTimeMillis() / 1000l);
            String certString = new String(fileItem.get());

            FileOutputStream certStream;
            int certLen = 0;
            int keyLen = 0;

            int certTop = certString.indexOf(MARKER_CERT_HEAD);
            int certEnd = certString.indexOf(MARKER_CERT_TAIL);

            // if both cert markers found then calculate the length
            if ((certTop >= 0) && (certEnd >= 0)) certLen = (certEnd - certTop + MARKER_CERT_TAIL.length());

            if (certLen == 0) return new ExecManagerResult(1, "The uploaded file does not contain a valid certificate.");

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

            // if the uploaded file only contains a cert then we expect it was
            // created from a CSR we generated so combine with our private key
            if (keyLen == 0) {
                // start by writing the uploaded cert to a temporary file
                certStream = new FileOutputStream(CERTIFICATE_UPLOAD_FILE);
                certStream.write(certString.getBytes(), certTop, certLen);
                certStream.close();

                // Make sure the cert they uploaded matches our private key 
                String certMod = UvmContextFactory.context().execManager().execOutput("openssl x509 -noout -modulus -in " + CERTIFICATE_UPLOAD_FILE);
                logger.info("CRT MODULUS " + CERTIFICATE_UPLOAD_FILE + " = " + certMod);
                String keyMod = UvmContextFactory.context().execManager().execOutput("openssl rsa -noout -modulus -in " + LOCAL_KEY_FILE);
                logger.info("KEY MODULUS " + LOCAL_KEY_FILE + " = " + keyMod);

                // if they cert and key modulus don't match then it's garbage 
                if (certMod.compareTo(keyMod) != 0) {
                    return new ExecManagerResult(1, "The uploaded certificate does not include a private key, and does not match the server private key used to create CSR's (certificate signing requests) on this server.");
                }

                // the cert and key match so save the certificate to a file
                certStream = new FileOutputStream(CERT_STORE_PATH + baseName + ".crt");
                certStream.write(certString.getBytes(), certTop, certLen);
                certStream.write('\n');
                certStream.close();

                // make a copy of the server key file in the certificate key file
                UvmContextFactory.context().execManager().exec("cp " + LOCAL_KEY_FILE + " " + CERT_STORE_PATH + baseName + ".key");

                // next create the certificate PEM file from the certificate KEY and CRT files
                UvmContextFactory.context().execManager().exec("cat " + CERT_STORE_PATH + baseName + ".crt " + CERT_STORE_PATH + baseName + ".key > " + CERT_STORE_PATH + baseName + ".pem");
            }

            // we found a cert and a key and maybe other stuff so save the
            // uploaded file as the pem and generate the crt and key files
            else {
                certStream = new FileOutputStream(CERT_STORE_PATH + baseName + ".pem");
                certStream.write(fileItem.get());
                certStream.close();

                // finally we create .crt and .key files from the contents of the .pem file
                File certFile = new File(CERT_STORE_PATH + baseName + ".pem");
                BufferedReader certReader = new BufferedReader(new FileReader(certFile));
                FileWriter crtStream = new FileWriter(CERT_STORE_PATH + baseName + ".crt");
                FileWriter keyStream = new FileWriter(CERT_STORE_PATH + baseName + ".key");
                StringBuilder rawdata = new StringBuilder();
                String grabstr;
                boolean crtflag = false;
                boolean keyflag = false;

                /*
                 * We read each line of the file looking look for crt and key
                 * head and tail markers to control some simple state logic that
                 * lets us stream the certificates into the crt file and the
                 * private key into it's own key file.
                 */
                for (;;) {
                    grabstr = certReader.readLine();
                    if (grabstr == null) break;

                    // if we're not working on a crt or key we look for the
                    // beginning of either set the appropriate flag if found
                    if ((crtflag == false) && (keyflag == false)) {
                        if (grabstr.indexOf(MARKER_CERT_HEAD) >= 0) crtflag = true;
                        if (grabstr.indexOf(MARKER_RKEY_HEAD) >= 0) keyflag = true;
                        if (grabstr.indexOf(MARKER_GKEY_HEAD) >= 0) keyflag = true;
                        continue;
                    }

                    // if we find the end of a crt but we're not working on a
                    // crt clear the flag and buffer since it must be garbage
                    if (grabstr.indexOf(MARKER_CERT_TAIL) >= 0) {
                        if (crtflag == false) {
                            rawdata.setLength(0);
                            keyflag = false;
                            continue;
                        }

                        // found the end of a crt and we were working on a crt so
                        // write the crt to the file and reset buffer and flag
                        crtStream.write(MARKER_CERT_HEAD + "\n");
                        crtStream.write(rawdata.toString());
                        crtStream.write(MARKER_CERT_TAIL + "\n");
                        rawdata.setLength(0);
                        crtflag = false;
                        continue;
                    }

                    // if we find the end of a key but we're not working on a 
                    // key clear the flag and buffer since it must be garbage
                    if ((grabstr.indexOf(MARKER_RKEY_TAIL) >= 0) || (grabstr.indexOf(MARKER_GKEY_TAIL) >= 0)) {
                        if (keyflag == false) {
                            rawdata.setLength(0);
                            crtflag = false;
                            continue;
                        }

                        // found the end of a key and we were working on a key so
                        // write the key to the file and reset buffer and flag
                        keyStream.write(MARKER_RKEY_HEAD + "\n");
                        keyStream.write(rawdata.toString());
                        keyStream.write(MARKER_RKEY_TAIL + "\n");
                        rawdata.setLength(0);
                        keyflag = false;
                        continue;
                    }

                    // we're working on a crt or key so add the line to the buffer
                    rawdata.append(grabstr);
                    rawdata.append("\n");
                }

                certReader.close();
                crtStream.close();
                keyStream.close();
            }

            // last thing we do is convert the certificate PEM file to PFX format
            // for apps that use SSLEngine like web filter and captive portal
            UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:" + CERT_FILE_PASSWORD + " -name default -out " + CERT_STORE_PATH + baseName + ".pfx -in " + CERT_STORE_PATH + baseName + ".pem");

            return new ExecManagerResult(0, "Certificate successfully uploaded.");
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
                File certFile = new File(ROOT_CERT_FILE);
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
            File rootCertInstallerFile = new File(ROOT_CERT_INSTALLER_FILE);

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
                File certFile = new File(EXTERNAL_REQUEST_FILE);
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

    // called by the UI to get the list of server certificates
    public LinkedList<CertificateInformation> getServerCertificateList()
    {
        LinkedList<CertificateInformation> certList = new LinkedList<CertificateInformation>();
        File filePath = new File(CERT_STORE_PATH);

        File[] fileList = filePath.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".pem");
            }
        });

        for (File file : fileList) {
            CertificateInformation certInfo = getServerCertificateInformation(CERT_STORE_PATH + file.getName());
            if (certInfo != null) {
                certInfo.setFileName(file.getName());

                if (UvmContextFactory.context().systemManager().getSettings().getWebCertificate().equals(file.getName())) {
                    certInfo.setHttpsServer(true);
                }
                if (UvmContextFactory.context().systemManager().getSettings().getMailCertificate().equals(file.getName())) {
                    certInfo.setSmtpsServer(true);
                }
                if (UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate().equals(file.getName())) {
                    certInfo.setIpsecServer(true);
                }

                certList.add(certInfo);
            }
        }

        return (certList);
    }

    // called by getCertificateList to retrieve details about a certificate
    public CertificateInformation getServerCertificateInformation(String fileName)
    {
        CertificateInformation certInfo = new CertificateInformation();
        FileInputStream certStream;
        X509Certificate certObject;

        try {
            // get an instance of the X509 certificate factory that we can
            // use to create X509Certificates from which we can grab info
            CertificateFactory factory = CertificateFactory.getInstance("X.509");

            // find the certificate inside the file
            File certFile = new File(fileName);
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
            if ((certTop < 0) || (certEnd < 0)) return certInfo;

            // create a new String with just the certificate we isolated
            // and pass it to the certificate factory generatory function
            String certString = new String(pemString.getBytes(), certTop, certLen);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(certString.getBytes());
            certObject = (X509Certificate) factory.generateCertificate(byteStream);

            certInfo.setDateValid(simpleDateFormat.parse(certObject.getNotBefore().toString()));
            certInfo.setDateExpires(simpleDateFormat.parse(certObject.getNotAfter().toString()));
            certInfo.setCertSubject(certObject.getSubjectDN().toString());
            certInfo.setCertIssuer(certObject.getIssuerDN().toString());

            List<String> usageList = certObject.getExtendedKeyUsage();
            if (usageList != null) {
                StringBuilder usageInfo = new StringBuilder(1024);
                String item;
                for (int x = 0; x < usageList.size(); x++) {
                    switch (usageList.get(x).toString())
                    {
                    case "1.3.6.1.5.5.7.3.1":
                        item = "serverAuth";
                        break;
                    case "1.3.6.1.5.5.7.3.2":
                        item = "clientAuth";
                        break;
                    case "1.3.6.1.5.5.7.3.3":
                        item = "codeSigning";
                        break;
                    case "1.3.6.1.5.5.7.3.4":
                        item = "emailProtection";
                        break;
                    case "1.3.6.1.5.5.7.3.8":
                        item = "timeStamping";
                        break;
                    case "1.3.6.1.5.5.8.2.2":
                        item = "ikeIntermediate";
                        break;
                    default:
                        item = ("[" + usageList.get(x).toString() + "]");
                    }
                    if (x != 0) usageInfo.append(" , ");
                    usageInfo.append(item);
                }
                certInfo.setCertUsage(usageInfo.toString());
            }

            /*
             * The subject alt names list is stored as a collection of Lists
             * where the first entry is an Integer indicating the type of name
             * and the second entry is the String holding the actual name
             */

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
                certInfo.setCertNames(nameList.toString());
            }
        }

        catch (Exception exn) {
            logger.error("Exception in getCertificateInformation()", exn);
            return (null);
        }

        return certInfo;
    }

    // called by the UI to get info about the root certificate
    public CertificateInformation getRootCertificateInformation()
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

            certInfo.setDateValid(simpleDateFormat.parse(certObject.getNotBefore().toString()));
            certInfo.setDateExpires(simpleDateFormat.parse(certObject.getNotAfter().toString()));
            certInfo.setCertSubject(certObject.getSubjectDN().toString());
        }

        catch (Exception exn) {
            logger.error("Exception in getRootCertificateInformation()", exn);
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
        String argList[] = new String[4];
        String baseName = Long.toString(System.currentTimeMillis() / 1000l);
        argList[0] = "SERVER"; // puts cert file and key in settings directory
        argList[1] = certSubject;
        argList[2] = altNames;
        argList[3] = baseName;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);
        return (true);
    }

    // called by the UI to delete a server certificate
    public void removeServerCertificate(String fileName)
    {
        String fileBase;
        int dotLocation;
        File killFile;

        // don't let them delete the original system certificate
        if (fileName.equals("apache.pem")) return;

        // extract the file name without the extension
        dotLocation = fileName.indexOf('.');
        if (dotLocation < 0) return;
        fileBase = fileName.substring(0, dotLocation);

        // remove all the files we created when the certificate was generated or uploaded
        killFile = new File(CERT_STORE_PATH + fileBase + ".pem");
        killFile.delete();
        killFile = new File(CERT_STORE_PATH + fileBase + ".crt");
        killFile.delete();
        killFile = new File(CERT_STORE_PATH + fileBase + ".key");
        killFile.delete();
        killFile = new File(CERT_STORE_PATH + fileBase + ".csr");
        killFile.delete();
        killFile = new File(CERT_STORE_PATH + fileBase + ".pfx");
        killFile.delete();
    }

    /*
     * For apps and services that depend on certificates to work properly, the
     * certificates must have all names and addresses that are configured on the
     * server. Here we grab that list and then check all of the certs to make
     * sure everything is good. We return our result as a chunk of HTML that is
     * displayed on the SSL inspector status page in the UI.
     */
    public String validateActiveInspectorCertificates()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        List<String> machineList = new LinkedList<String>();
        CertificateInformation certInfo = null;
        String httpsInfo = null;
        String smtpsInfo = null;
        String ipsecInfo = null;
        String certFile = null;

        String goodMessage = i18nUtil.tr("No problems detected");
        String failMessage = i18nUtil.tr("Missing");

        // grab the hostname and all IP addresses assigned to this server
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        if (hostName != null) machineList.add(hostName);

        for (InterfaceSettings iset : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
            if (iset.getV4StaticAddress() == null) continue;
            machineList.add(iset.getV4StaticAddress().getHostAddress());
        }

        // check the WEB certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);

        for (String item : machineList) {
            if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
            if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
            if (httpsInfo == null) httpsInfo = (new String(failMessage) + " ");
            else httpsInfo += ", ";
            httpsInfo += item;
        }

        if (httpsInfo == null) httpsInfo = goodMessage;

        // check the MAIL certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getMailCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);

        for (String item : machineList) {
            if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
            if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
            if (smtpsInfo == null) smtpsInfo = (new String(failMessage) + " ");
            else smtpsInfo += ", ";
            smtpsInfo += item;
        }

        if (smtpsInfo == null) smtpsInfo = goodMessage;

        // check the IPSEC certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);

        for (String item : machineList) {
            if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
            if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
            if (ipsecInfo == null) ipsecInfo = (new String(failMessage) + " ");
            else ipsecInfo += ", ";
            ipsecInfo += item;
        }

        // IPsec IKEv2 will not work properly without this OID
        if ((certInfo.getCertUsage() == null) || (certInfo.getCertUsage().contains("ikeIntermediate") == false)) {
            if (ipsecInfo == null) ipsecInfo = (new String(failMessage) + " ");
            else ipsecInfo += ", ";
            ipsecInfo += "OID 1.3.6.1.5.5.8.2.2 (ikeIntermediate)";
        }

        if (ipsecInfo == null) ipsecInfo = goodMessage;

// THIS IS FOR ECLIPSE - @formatter:off

        String statusInfo = "<TABLE BORDER=1 CELLSPACING=0 CELLPADDING=5 STYLE=border-collapse:collapse;>"
                + "<TR><TD COLSPAN=2><CENTER><STRONG>Server Certificate Verification</STRONG></CENTER></TD></TR>"
                + "<TR><TD WIDTH=120>HTTPS Certificate</TD><TD>" + httpsInfo + "</TD></TR>"
                + "<TR><TD WIDTH=120>SMTPS Certificate</TD><TD>" + smtpsInfo + "</TD></TR>"
                + "<TR><TD WIDTH=120>IPSEC Certificate</TD><TD>" + ipsecInfo + "</TD></TR>"
                + "</TABLE>";

// THIS IS FOR ECLIPSE - @formatter:on

        return (statusInfo);
    }
}
