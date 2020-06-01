/**
 * $Id$
 */

package com.untangle.uvm;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;

/**
 * The Certificate Manager handles the internal certificate authority that is
 * used to sign the man-in-the-middle certificates generated when using the SSL
 * Inspector application. It also manages the server certificates that are used
 * by the internal web server, IPsec server, and for handling inbound secure
 * SMTP traffic.
 */
public class CertificateManagerImpl implements CertificateManager
{
    private static final String CERTIFICATE_PARSER_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-cert-parser";
    private static final String CERTIFICATE_PARSER_FILE = "/tmp/certificate.upload";

    private static final String CERTIFICATE_GENERATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-certgen";
    private static final String ROOT_CA_CREATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen";

    private static final String CERTIFICATE_UPLOAD_FILE = CERT_STORE_PATH + "upload.crt";
    private static final String KEY_UPLOAD_FILE = CERT_STORE_PATH + "upload.key";
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
    private static HashMap<Integer, String> validAlternateList = new HashMap<>();

    static {
        validAlternateList.put(0x01, "email");
        validAlternateList.put(0x02, "DNS");
        validAlternateList.put(0x06, "URI");
        validAlternateList.put(0x07, "IP");
        validAlternateList.put(0x08, "RID");
    }

    // CertContent enum is used for key/cert validation functions
    private enum CertContent {CERT, KEY, EXTRA}

    /**
     * The main certificate manager implementation where we register our upload
     * and download handlers, and do other initialization.
     */
    protected CertificateManagerImpl()
    {
        File rootCertFile = new File(ROOT_CERT_FILE);
        File rootKeyFile = new File(ROOT_KEY_FILE);

        UvmContextFactory.context().servletFileManager().registerUploadHandler(new ServerCertificateUploadHandler());

        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new CertificateDownloadHandler());

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
            UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + "UntangleRootCA DEFAULT");

            // Symlink them
            symlinkRootCerts(CERT_STORE_PATH, CERT_STORE_PATH + "UntangleRootCA/", false);

            // in the development enviroment save these to a global location
            // so they will survive a rake clean
            if (UvmContextFactory.context().isDevel()) {
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
                UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CERT_FILE + " /etc/untangle/untangle.crt");
            }
        }

        // if the root CA files are not symlinks, then we need to move them (to a timestamped directory) and symlink them
        if(!Files.isSymbolicLink(rootCertFile.toPath()) || !Files.isSymbolicLink(rootKeyFile.toPath())) {
            String baseName = Long.toString(System.currentTimeMillis() / 1000l);

            symlinkRootCerts(CERT_STORE_PATH, CERT_STORE_PATH + baseName + "/", true);
        }

        // we should have a root CA at this point so we check the local apache
        // cert files and create them now if either is missing
        File localCertFile = new File(LOCAL_CRT_FILE);
        File localKeyFile = new File(LOCAL_KEY_FILE);
        if ((localCertFile.exists() == false) || (localKeyFile.exists() == false)) {
            String fqdn = UvmContextFactory.context().networkManager().getFullyQualifiedHostname();
            logger.info("Creating default locally signed apache certificate for " + fqdn);
            UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + fqdn);
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

        // activate the RADIUS server certificate
        UvmContextFactory.context().systemManager().activateRadiusCertificate();
    }

    /**
     * Called by the UI to upload server certificates
     */
    private class ServerCertificateUploadHandler implements UploadHandler
    {
        /**
         * Get the name of our upload handler
         * 
         * @return The name of our upload handler
         */
        @Override
        public String getName()
        {
            return "server_cert";
        }

        /**
         * Called to handle a certificate uploaded from the Admin web page
         * 
         * @param fileItem
         *        The uploaded file
         * @param argument
         *        Arguments passed to the upload handler
         * @return The upload result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            // save the uploaded file
            FileOutputStream fileStream = new FileOutputStream(CERTIFICATE_PARSER_FILE);
            fileStream.write(fileItem.get());
            fileStream.close();

            // call the external utility to parse the uploaded file
            String certData = UvmContextFactory.context().execManager().execOutput(CERTIFICATE_PARSER_SCRIPT + " " + CERTIFICATE_PARSER_FILE);

            // returned the results 
            ExecManagerResult result = new ExecManagerResult(0, certData);
            return (result);
        }
    }

    /**
     * Called by requests to download the root CA certificate file
     */
    private class CertificateDownloadHandler implements DownloadHandler
    {
        /**
         * Get the name of our download handler
         * 
         * @return The name of our download handler
         */
        @Override
        public String getName()
        {
            return "certificate_download";
        }

        /**
         * Called to handle download requests
         * 
         * @param req
         *        The web request
         * @param resp
         *        The web repsonse
         */
        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            FileInputStream certStream = null;
            String downloadType = req.getParameter("arg1");

            if (downloadType.equalsIgnoreCase("root")) {
                try {
                    File certFile = new File(ROOT_CERT_FILE);
                    certStream = new FileInputStream(certFile);
                    byte[] certData = new byte[(int) certFile.length()];
                    certStream.read(certData);

                    // set the headers.
                    resp.setContentType("application/x-download");
                    resp.setHeader("Content-Disposition", "attachment; filename=root_authority.crt");

                    OutputStream webStream = resp.getOutputStream();
                    webStream.write(certData);
                } catch (Exception exn) {
                    logger.warn("Exception during certificate download", exn);
                } finally {
                    try {
                        if (certStream != null) {
                            certStream.close();
                        }
                    } catch (IOException ex) {
                        logger.error("Unable to close file", ex);
                    }
                }
            }
            else if (downloadType.equalsIgnoreCase("csr")) {
                String argList[] = new String[3];
                argList[0] = "REQUEST"; // create CSR for server
                argList[1] = req.getParameter("arg2"); // cert subject
                argList[2] = req.getParameter("arg3"); // alt names
                String argString = UvmContextFactory.context().execManager().argBuilder(argList);
                UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);

                try {
                    File certFile = new File(EXTERNAL_REQUEST_FILE);
                    certStream = new FileInputStream(certFile);
                    byte[] certData = new byte[(int) certFile.length()];
                    certStream.read(certData);

                    // set the headers.
                    resp.setContentType("application/x-download");
                    resp.setHeader("Content-Disposition", "attachment; filename=server_certificate.csr");

                    OutputStream webStream = resp.getOutputStream();
                    webStream.write(certData);
                } catch (Exception exn) {
                    logger.warn("Exception during certificate download", exn);
                } finally {
                    try {
                        if (certStream != null) {
                            certStream.close();
                        }
                    } catch (IOException ex) {
                        logger.error("Unable to close file", ex);
                    }
                }
            }
        }
    }

    /**
     * Called by the UI to get a list of all available server certificates,
     * which we generate by searching for .pem files in the certificate store
     * path. We compare each cert to those assigned for web, mail, and ipsec
     * services, and set the corresponding flag so the user interface can
     * properly display the active certificate for each service.
     * 
     * @return A list of all known server certificate files
     */
    public LinkedList<CertificateInformation> getServerCertificateList()
    {
        LinkedList<CertificateInformation> certList = new LinkedList<>();
        File filePath = new File(CERT_STORE_PATH);

        File[] fileList = filePath.listFiles(new FilenameFilter()
        {
            /**
             * Matcher for listFiles that looks for .pem files
             * 
             * @param dir
             *        The file directory
             * @param name
             *        The file name
             * @return True to accept the file, false to reject
             */
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
                if (UvmContextFactory.context().systemManager().getSettings().getRadiusCertificate().equals(file.getName())) {
                    certInfo.setRadiusServer(true);
                }

                certList.add(certInfo);
            }
        }

        return (certList);
    }

    /**
     * Called to retrieve the details about a certificate
     * 
     * @param fileName
     *        The certificate file
     * @return The certificate details
     */
    // called by getCertificateList to retrieve details about a certificate
    public CertificateInformation getServerCertificateInformation(String fileName)
    {
        CertificateInformation certInfo = new CertificateInformation();
        FileInputStream certStream;
        X509Certificate certObject;

        try {
            // find the certificate inside the file
            File certFile = new File(fileName);
            certStream = new FileInputStream(certFile);
            byte[] certData = new byte[(int) certFile.length()];
            certStream.read(certData);
            certStream.close();

            // look for the header and trailer strings
            String pemString = new String(certData);
            int certTop = pemString.indexOf(MARKER_CERT_HEAD);
            int certEnd = pemString.indexOf(MARKER_CERT_TAIL);
            int certLen = (certEnd - certTop + MARKER_CERT_TAIL.length());

            // if either certTop or certEnd returned an error something is
            // wrong so just return what we have so far
            if ((certTop < 0) || (certEnd < 0)) return certInfo;

            // create a new String with just the certificate we isolated
            // and pass it to the certificate factory generatory function
            String certString = new String(pemString.getBytes(), certTop, certLen);
            certObject = get509CertFromString(certString);

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

    /**
     * getRootCertificateList will return any root CAs that are currently in the untangle-certificates directory
     * 
     * 
     * @return - A list of CertificateInformation of Root Certificates
     */
    public LinkedList<CertificateInformation> getRootCertificateList()
    {
        LinkedList<CertificateInformation> certList = new LinkedList<>();

        // Use readlink to find where the current untangle.crt is linked to
        // We do this instead of loading the File because it seems the JVM
        // is caching the symlink path for a few seconds after we update
        // the symlinks with symlinkRootCerts
        String symlinkPath = UvmContextFactory.context().execManager().exec("readlink " + ROOT_CERT_FILE).getOutput().trim();
        
        File filePath = new File(CERT_STORE_PATH);

        //Iterate directories in the CERT_STORE_PATH, if they contain files that end with CRT then add to the file arraylist
        for(File certs : filePath.listFiles()) {
            if(certs.isDirectory()) {
                for(File crt : certs.listFiles()) {
                    if(crt.getName().endsWith(".crt")){
                        // Call getRootCertificateInformation and add the results into the certificateinformation list
                        var certInfo = getRootCertificateInformation(crt.getAbsolutePath());

                        // If this is the root CA, then set the property
                        if(symlinkPath.equalsIgnoreCase(certInfo.getFileName())) {
                            certInfo.setActiveRootCA(true);
                        }

                        certList.add(certInfo);
                    }
                }
            }
        }
        logger.info("Root CA File List:" + certList);

       return (certList);
   }

   /**
    * getRootCertificateInformation is used to get the details of the current ROOT_CERT_FILE
    *
    * @return The CA root certificate details
    */
   public CertificateInformation getRootCertificateInformation()
   {
       return getRootCertificateInformation(ROOT_CERT_FILE);
   }

    /**
     * Called to get the details from our CA root certificate
     * 
     * @param fileName 
     *          The filename to get details of
     * 
     * @return The CA root certificate details
     */
    public CertificateInformation getRootCertificateInformation(String fileName)
    {
        CertificateInformation certInfo = new CertificateInformation();
        X509Certificate certObject;

        try {
            certObject = get509CertFromFile(fileName);

            certInfo.setFileName(fileName);

            if(certObject != null) {
                certInfo.setDateValid(simpleDateFormat.parse(certObject.getNotBefore().toString()));
                certInfo.setDateExpires(simpleDateFormat.parse(certObject.getNotAfter().toString()));
                certInfo.setCertSubject(certObject.getSubjectDN().toString());
            }
        }

        catch (Exception exn) {
            logger.error("Exception in getRootCertificateInformation()", exn);
        }

        return certInfo;
    }

    /**
     * Called by the UI to generate a new root certificate authority. The dummy
     * argument is not used and makes the JavaScript a little simpler.
     * 
     * @param commonName
     *        The cert common name, used in creating the CA directory
     * @param certSubject
     *        The subject for the new CA root certificate
     * @return True
     */
    public boolean generateCertificateAuthority(String commonName, String certSubject)
    {
        String baseName = commonName +"-"+ Long.toString(System.currentTimeMillis() / 1000l);

        logger.info("Creating new root certificate authority: " + certSubject);
        String argList[] = new String[2];
        argList[0] = baseName;
        argList[1] = certSubject;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        UvmContextFactory.context().execManager().exec(ROOT_CA_CREATOR_SCRIPT + argString);

        // Symlink generated certs to the CERT_STORE_PATH (ROOT_KEY_FILE and ROOT_CERT_FILE)
        symlinkRootCerts(CERT_STORE_PATH, CERT_STORE_PATH + baseName + "/", false);

        // in the development enviroment save these to a global location
        // so they will survive a rake clean
        if (UvmContextFactory.context().isDevel()) {
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_KEY_FILE + " /etc/untangle/untangle.key");
            UvmContextFactory.context().execManager().exec("cp -fa " + ROOT_CERT_FILE + " /etc/untangle/untangle.crt");
        }

        return (true);
    }

    /**
     * Called to generate a new server certificate that is signed by our
     * internal certificate authority.
     * 
     * @param certSubject
     *        The certificate subject for the new certificate
     * @param altNames
     *        The subject alternative names for the new certificate
     * @return True
     */
    public boolean generateServerCertificate(String certSubject, String altNames)
    {
        logger.info("Creating new locally signed apache certificate: " + certSubject);
        String argList[] = new String[4];
        String baseName = Long.toString(System.currentTimeMillis() / 1000l);
        argList[0] = "SERVER"; // puts cert file and key in settings directory
        argList[1] = certSubject.replaceAll("\"", "'");
        argList[2] = altNames;
        argList[3] = baseName;
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);
        if (result.getResult() != 0) return (false);
        return (true);
    }

    /**
     * setActiveRootCertificate will set a specific root CA to the active root certificate
     * @param fileName
     */
    public void setActiveRootCertificate(String fileName) {
        // Use filename to get the parent dir
        File rootCert = new File(fileName);
        var certParent = rootCert.getParent();

        // Use symlink function to replace CERT_STORE_PATH root certs
        symlinkRootCerts(CERT_STORE_PATH, certParent + "/", false);
    }


    /**
     * Called to create a new server certificate using the data provided.
     * 
     * @param certMode
     *   The certMode upload type
     * @param certData
     *        The certificate data
     * @param keyData
     *        The key data
     * @param extraData
     *        Optional intermediate certificates
     * @return The result of the operation
     */
    public ExecManagerResult uploadCertificate(String certMode, String certData, String keyData, String extraData)
    {
        String baseName = Long.toString(System.currentTimeMillis() / 1000l);
        int certLen = 0;
        int keyLen = 0;
        int extraLen = 0;

        // make sure all of the strings passed have a trailing newline character
        certLen = validateData(certData, CertContent.CERT);
        if (certLen == 0) return new ExecManagerResult(1, "The certificate is not valid");

        keyLen = validateData(keyData, CertContent.KEY);
        if (keyLen == 0) return new ExecManagerResult(1, "The key is not valid");

        extraLen = validateData(extraData, CertContent.EXTRA);

        // we have a valid cert and key so save the uploaded certificate to a temp upload file
        storeData(certData, CERTIFICATE_UPLOAD_FILE);

        // next we save the uploaded key to a temp upload file
        storeData(keyData, KEY_UPLOAD_FILE);

        // make sure the uploaded cert matches the uploaded key
        if (!validateCertKeyPair(CERTIFICATE_UPLOAD_FILE, KEY_UPLOAD_FILE)) {
            return new ExecManagerResult(1, "The Server Certificate does not match the Certificate Key.");
        }

        // store them in permanent locations
        if(certMode.equalsIgnoreCase("SERVER")) {
            // create the key file
            storeData(keyData, CERT_STORE_PATH + baseName + ".key");

            // create the crt file with the certData and extraData
            if(extraLen > 0) {
                // append the cert data with extra
                certData += extraData;
            }

            storeData(certData, CERT_STORE_PATH + baseName + ".crt");

            // next create the certificate PEM file from the certificate KEY and CRT files
            UvmContextFactory.context().execManager().exec("cat " + CERT_STORE_PATH + baseName + ".crt " + CERT_STORE_PATH + baseName + ".key > " + CERT_STORE_PATH + baseName + ".pem");

            // last thing we do is convert the certificate PEM file to PFX format
            // for apps that use SSLEngine like web filter and captive portal
            UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:" + CERT_FILE_PASSWORD + " -name default -out " + CERT_STORE_PATH + baseName + ".pfx -in " + CERT_STORE_PATH + baseName + ".pem");

            return new ExecManagerResult(0, "Certificate successfully uploaded");
        } else if (certMode.equalsIgnoreCase("ROOT")) {

            var newRootPath = CERT_STORE_PATH + baseName + "/";
            var newRootKey = newRootPath + "untangle.key";
            var newRootCrt = newRootPath + "untangle.crt";

            // create the root cert dir using the basename
            UvmContextFactory.context().execManager().exec("mkdir -p " + newRootPath);

            // store the key and cert there
            storeData(keyData, newRootKey);
            storeData(certData, newRootCrt);

            // we use this certInfo to get the serial and add it to serial.txt
            var certInfo = get509CertFromString(certData);

            // Root certs also need an index and serial
            UvmContextFactory.context().execManager().exec("echo " + certInfo.getSerialNumber().toString(16) + ">"+newRootPath+"serial.txt");
            UvmContextFactory.context().execManager().exec("touch " + newRootPath + "index.txt");

            // symlinks key, cert, index, serial to the untangle-certificates directory
            symlinkRootCerts(CERT_STORE_PATH, newRootPath, false);

            return new ExecManagerResult(0, "Root Certificate successfully uploaded");
        } else {
            return new ExecManagerResult(1, "Invalid certMode in uploadCeritificate call.");
        }

    }

    /**
     * Called to import a signed certificate that originated with a certificate
     * signing request that we generated.
     * 
     * @param certData
     *        The certificate data
     * @param extraData
     *        Optional intermediate certificates
     * @return The result of the operation
     */
    public ExecManagerResult importSignedRequest(String certData, String extraData)
    {
        String baseName = Long.toString(System.currentTimeMillis() / 1000l);
        FileOutputStream fileStream = null;
        int certLen = 0;
        int keyLen = 0;

        // Validate as a SERVER_CERT
        certLen = validateData(certData, CertContent.CERT);
        if (certLen == 0) return new ExecManagerResult(1, "The certificate provided is not valid");

        validateData(extraData, CertContent.EXTRA);

        // start by writing the uploaded cert to a temporary file
        storeData(certData, CERTIFICATE_UPLOAD_FILE);

        if(!validateCertKeyPair(CERTIFICATE_UPLOAD_FILE, LOCAL_KEY_FILE)) {
            return new ExecManagerResult(1, "The uploaded certificate does not match the server private key used to create CSR's (certificate signing requests) on this server.");  
        }

        // Store the CSR base name for subsequent file usage
        var csrBaseCrt = CERT_STORE_PATH + baseName + ".crt";
        var csrBaseKey = CERT_STORE_PATH + baseName + ".key";
        var csrBasePem = CERT_STORE_PATH + baseName + ".pem";
        var csrBasePfx = CERT_STORE_PATH + baseName + ".pfx";

        // the cert and key match so save the certificate to a file
        if(extraData.length() > 0) {
            storeData(certData + extraData, csrBaseCrt);
        } else {
            storeData(certData, csrBaseCrt);
        }

        // make a copy of the server key file in the certificate key file
        UvmContextFactory.context().execManager().exec("cp " + LOCAL_KEY_FILE + " " + csrBaseKey);

        // next create the certificate PEM file from the certificate KEY and CRT files
        UvmContextFactory.context().execManager().exec("cat " + csrBaseCrt + " " + csrBaseKey + " > " + csrBasePem);

        // last thing we do is convert the certificate PEM file to PFX format
        // for apps that use SSLEngine like web filter and captive portal
        UvmContextFactory.context().execManager().exec("openssl pkcs12 -export -passout pass:" + CERT_FILE_PASSWORD + " -name default -out " + csrBasePfx + " -in " + csrBasePem);

        return new ExecManagerResult(0, "Certificate successfully uploaded");
    }

    /**
     * Called by the UI to delete a server certificate. To make sure we always
     * have a certificate we can use for the Web server, we protect the
     * certificate with the base file name "apache" from ever being removed,
     * since that's the certificate that is generated during installation.
     * 
     * @param type
     *        The type of certificate to delete
     * @param fileName
     *        The certificate file to delete
     */
    public void removeCertificate(String type, String fileName)
    {
        String fileBase;
        int dotLocation;
        File killFile;

        // don't let them delete the original system certificate
        if (fileName.equals("apache.pem")) return;

        // don't let them delete any certificate that is assigned to a service
        if (fileName.equals(UvmContextFactory.context().systemManager().getSettings().getWebCertificate())) return;
        if (fileName.equals(UvmContextFactory.context().systemManager().getSettings().getMailCertificate())) return;
        if (fileName.equals(UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate())) return;
        if (fileName.equals(UvmContextFactory.context().systemManager().getSettings().getRadiusCertificate())) return;

        if(type.equalsIgnoreCase("SERVER")){
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
        } else if(type.equalsIgnoreCase("ROOT")) {
            // Use filename to get the parent dir
            File rootCert = new File(fileName);
            var certParent = rootCert.getParent();

            // verify dotLocation is not top level
            if(certParent != CERT_STORE_PATH) {
                File parentFile = new File(certParent);

                // rm the index, crt, key, serial files in here
                for(File child : parentFile.listFiles()) {
                    child.delete();
                }
                
                // rm the directory
                parentFile.delete();
            }
        }
    }

    /**
     * For apps and services that depend on certificates to work properly, the
     * certificates must have all names and addresses that are configured on the
     * server. Here we grab that list and then check all of the certs to make
     * sure everything is good. We return our result as a chunk of HTML that is
     * displayed on the SSL inspector status page in the UI.
     * 
     * @return The certificate status
     */
    public String validateActiveInspectorCertificates()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        List<String> machineList = new LinkedList<>();
        CertificateInformation certInfo = null;
        String httpsInfo = null;
        String smtpsInfo = null;
        String ipsecInfo = null;
        String radiusInfo = null;
        String certFile = null;

        String missMessage = i18nUtil.tr("Certificate not found");
        String goodMessage = i18nUtil.tr("No problems detected");
        String failMessage = i18nUtil.tr("Missing");

        // grab the hostname and all IP addresses assigned to this server
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        if (hostName != null) machineList.add(hostName);

        for (InterfaceSettings iset : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
            if (iset.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC) continue;
            if (iset.getV4StaticAddress() == null) continue;
            if (iset.igetDisabled()) continue;
            if (iset.igetBridged()) continue;
            machineList.add(iset.getV4StaticAddress().getHostAddress());
        }

        // check the WEB certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);
        if (certInfo == null) {
            httpsInfo = missMessage;
        } else {
            for (String item : machineList) {
                if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
                if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
                if (httpsInfo == null) httpsInfo = (new String(failMessage) + " ");
                else httpsInfo += ", ";
                httpsInfo += item;
            }
            if (httpsInfo == null) httpsInfo = goodMessage;
        }

        // check the MAIL certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getMailCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);
        if (certInfo == null) {
            smtpsInfo = missMessage;
        } else {
            for (String item : machineList) {
                if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
                if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
                if (smtpsInfo == null) smtpsInfo = (new String(failMessage) + " ");
                else smtpsInfo += ", ";
                smtpsInfo += item;
            }
            if (smtpsInfo == null) smtpsInfo = goodMessage;
        }

        // check the IPSEC certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);
        if (certInfo == null) {
            ipsecInfo = missMessage;
        } else {
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
        }

        // check the RADIUS certificate
        certFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getRadiusCertificate().replaceAll("\\.pem", "\\.crt");
        certInfo = getServerCertificateInformation(certFile);
        if (certInfo == null) {
            radiusInfo = missMessage;
        } else {
            for (String item : machineList) {
                if ((certInfo.getCertSubject() != null) && (certInfo.getCertSubject().toLowerCase().contains(item.toLowerCase()))) continue;
                if ((certInfo.getCertNames() != null) && (certInfo.getCertNames().toLowerCase().contains(item.toLowerCase()))) continue;
                if (radiusInfo == null) radiusInfo = (new String(failMessage) + " ");
                else radiusInfo += ", ";
                radiusInfo += item;
            }
            if (radiusInfo == null) radiusInfo = goodMessage;
        }

        StringBuilder statusInfo = new StringBuilder(1024);
        statusInfo.append("<TABLE BORDER=1 CELLSPACING=0 CELLPADDING=5 STYLE=border-collapse:collapse;>");
        statusInfo.append("<TR><TD COLSPAN=2><CENTER><STRONG>Server Certificate Verification</STRONG></CENTER></TD></TR>");
        statusInfo.append("<TR><TD WIDTH=120>HTTPS Certificate</TD><TD>" + httpsInfo + "</TD></TR>");
        statusInfo.append("<TR><TD WIDTH=120>SMTPS Certificate</TD><TD>" + smtpsInfo + "</TD></TR>");
        statusInfo.append("<TR><TD WIDTH=120>IPSEC Certificate</TD><TD>" + ipsecInfo + "</TD></TR>");
        if (UvmContextFactory.context().isExpertMode()) {
            statusInfo.append("<TR><TD WIDTH=120>RADIUS Certificate</TD><TD>" + radiusInfo + "</TD></TR>");
        }
        statusInfo.append("</TABLE>");
        return (statusInfo.toString());
    }

    
    /**
     * validateCertKeyPair will get the modulus of a certFile and keyFile, compare them, and return whether or not the pair match
     * 
     * @param certFileLocation - Location of the cert file
     * @param keyFileLocation - Location of the key file
     * @return boolean - True if key/cert pair is valid, false otherwise
     */
    private boolean validateCertKeyPair(String certFileLocation, String keyFileLocation) {
        String certMod = UvmContextFactory.context().execManager().execOutput("openssl x509 -noout -modulus -in " + certFileLocation);
        logger.info("CRT MODULUS " + certFileLocation + " = " + certMod);
        String keyMod = UvmContextFactory.context().execManager().execOutput("openssl rsa -noout -modulus -in " + keyFileLocation);
        logger.info("KEY MODULUS " + keyFileLocation + " = " + keyMod);

        // if the cert and key modulus do not match then it's garbage 
        if (certMod.compareTo(keyMod) != 0) {
            return false;
        }

        return true;
    }

    /**
     * validateData is used to validate a key or certificate using the CertContent enumerator,
     * and returns the length of the data after validation.
     * 
     * @param data - the key or cert data
     * @param type - the type of validation we should handle
     * @return int - Length of the data
     */
    private int validateData(String data, CertContent type) {
        int dataLen = 0;

        // ensure trailing newline
        if ((data.length() > 0) && (!data.endsWith("\n"))) data = data.concat("\n");

        String headMarker = "";
        String tailMarker = "";

        // determine markers by type
        // KEY == MARKER_RKEY_HEAD/MARKER_RKEY_TAIL or MARKER_GKEY_HEAD/MARKER_GKEY_TAIL
        // CERT == MARKER_CERT_HEAD/MARKER_CERT_TAIL
        // EXTRA == return full data length (including header/tail)
        if (type == CertContent.KEY) {
            headMarker = MARKER_RKEY_HEAD;
            tailMarker = MARKER_RKEY_TAIL;
        } else if (type == CertContent.CERT) {
            headMarker = MARKER_CERT_HEAD;
            tailMarker = MARKER_CERT_TAIL;
        } else if (type == CertContent.EXTRA) {
            // If there's any extra data, just return the full length
            return data.length();
        } 
        // return 0 for any other types passed in
        else return 0;
        
        int dataTop = data.indexOf(headMarker);
        int dataEnd = data.indexOf(tailMarker);

        // if both key markers found then calculate the length
        if ((dataTop >= 0) && (dataEnd >= 0)) {
            dataLen = (dataEnd - dataTop + tailMarker.length());
        } else if (type == CertContent.KEY) {
            // if we didn't find the RSA style during server_cert check, we check for generic format
            dataTop = data.indexOf(MARKER_GKEY_HEAD);
            dataEnd = data.indexOf(MARKER_GKEY_TAIL);
            if ((dataTop >= 0) && (dataEnd >= 0)) {
                dataLen = (dataEnd - dataTop + MARKER_GKEY_TAIL.length());
            }
        }

        return dataLen;
    }

    /**
     * storeData uses the FileOutputStream to save data into a fileLocation
     * 
     * @param data - the data to save
     * @param fileLocation - the location to save the data
     */
    private void storeData(String data, String fileLocation) {
        FileOutputStream fileStream = null;

        try {
            fileStream = new FileOutputStream(fileLocation);
            fileStream.write(data.getBytes());
            fileStream.close();
        } catch (Exception exn) {
            logger.warn("Exception saving file", exn);
        } finally {
            try {
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
            }
            fileStream = null;
        }
    }

    /**
     * get509CertFromFile will read the fileinputstream and return the get509CertFromString
     * 
     * @param filePath
     * @return
     */
    private X509Certificate get509CertFromFile(String filePath) {
        X509Certificate retCert;
        try
        {
            var certStream = new FileInputStream(filePath);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            retCert = (X509Certificate) factory.generateCertificate(certStream);
            certStream.close();

        } catch (Exception exn) {
            logger.error("Exception in get509CertFromFile(): ", exn);
            return null;
        }

        return retCert;
    }

    /**
     * get509CertInfo will access the cert factory and return cert info for a String of certData
     * 
     * @param certData
     * @return
     */
    private X509Certificate get509CertFromString(String certData) {
        X509Certificate retCert;
        try
        {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream byteStream = new ByteArrayInputStream(certData.getBytes());
            retCert = (X509Certificate) factory.generateCertificate(byteStream);
        } catch (Exception exn) {
            logger.error("Exception in get509CertFromString(): ", exn);
            return null;
        }
        
        return retCert;
    }

    /**
     * symlinkRootCert is used to link root certificates and associated files to specific directories
     * 
     * @param targetDir - The old directory the files were in, the target of the symlinks
     * @param sourceDir - The new directory the files should be in, the source of the actual files
     * @param moveCerts - Move the files from the target to the source also
     */
    private void symlinkRootCerts(String targetDir, String sourceDir, boolean moveCerts) {
        if(moveCerts) {
            // create a sourcedir if we need to
            UvmContextFactory.context().execManager().exec("mkdir -p " + sourceDir);

            // move cert, key, index, serial from old to new if move is specified
            UvmContextFactory.context().execManager().exec("mv " + targetDir + "untangle.crt " + sourceDir);
            UvmContextFactory.context().execManager().exec("mv " + targetDir + "untangle.key " + sourceDir);
            UvmContextFactory.context().execManager().exec("mv " + targetDir + "index* " + sourceDir);
            UvmContextFactory.context().execManager().exec("mv " + targetDir + "serial* " + sourceDir);
        }

        // symlink cert, key, index, serial from new location to old
        UvmContextFactory.context().execManager().exec("ln -sf " + sourceDir + "untangle.crt " + targetDir + "untangle.crt");
        UvmContextFactory.context().execManager().exec("ln -sf " + sourceDir + "untangle.key " + targetDir + "untangle.key");
        UvmContextFactory.context().execManager().exec("ln -sf " + sourceDir + "index.txt " + targetDir + "index.txt");
        UvmContextFactory.context().execManager().exec("ln -sf " + sourceDir + "serial.txt " + targetDir + "serial.txt");

    }

}
