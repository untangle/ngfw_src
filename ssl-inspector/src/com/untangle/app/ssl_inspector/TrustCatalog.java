/**
 * $Id: TrustCatalog.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import javax.net.ssl.TrustManagerFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.LinkedList;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import com.untangle.uvm.ExecManagerResult;
import org.apache.log4j.Logger;

/**
 * This class is used to manage the main trust factory that is used to validate
 * certificates from the servers we connect to on behalf of the client. Since
 * the client will only see the MitM certificates we sign, we have the
 * responsibility of doing the server cert verification the client would
 * normally do if we weren't sitting in the middle.
 */
class TrustCatalog
{
    private static String globalStoreFile = System.getProperty("uvm.lib.dir") + "/ssl-inspector/trusted-ca-list.jks";
    private static String globalStorePass = "password";
    private static String trustStoreFile = System.getProperty("uvm.settings.dir") + "/untangle-certificates/trustStore.jks";
    private static String trustStorePass = "password";

    private static Logger logger;

    /**
     * Static function for initialization
     * 
     * @param argLogger
     *        The logger to use for loggin
     */
    public static void staticInitialization(Logger argLogger)
    {
        logger = argLogger;
    }

    /**
     * Creates our trust factory and loads it with the standard list of trusted
     * CA's and then adds other CA's and certs that have been uploaded by the
     * user.
     * 
     * @return Our trust manager factory
     * @throws Exception
     */
    public static TrustManagerFactory createTrustFactory() throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance("JKS");

        // first we load the standard list of trusted CA certificates
        trustStore.load(new FileInputStream(globalStoreFile), globalStorePass.toCharArray());
        logger.info("Loaded " + trustStore.size() + " trusted certificates from " + globalStoreFile);

        // now we see if there is a local trustStore file
        File tester = new File(trustStoreFile);

        // found the file so load the certs and add each to trustStore
        if (tester.exists() == true) {
            KeyStore localStore = KeyStore.getInstance("JKS");
            localStore.load(new FileInputStream(trustStoreFile), trustStorePass.toCharArray());
            Enumeration<String> certlist = localStore.aliases();

            // walk through all the certs in the trustStore file and add
            // each one to the store we use to init the SSL context
            while (certlist.hasMoreElements()) {
                String alias = certlist.nextElement();
                logger.info("Adding trusted cert [" + alias + "] from " + trustStoreFile);
                trustStore.setCertificateEntry(alias, localStore.getCertificate(alias));
            }
        }

        // initialize the trust manager with all the certs we loaded
        TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
        factory.init(trustStore);
        return (factory);
    }

    /**
     * Gets a list of all trusted certs that have been uploaded
     * 
     * @return The list of all trusted certs that have been uploaded
     * @throws Exception
     */
    public static LinkedList<TrustedCertificate> getTrustCatalog() throws Exception
    {
        LinkedList<TrustedCertificate> trustCatalog = new LinkedList<>();

        // make sure there is a local trustStore file
        File tester = new File(trustStoreFile);

        // if not found just return the empty catalog
        if (tester.exists() == false) return (trustCatalog);

        KeyStore localStore = KeyStore.getInstance("JKS");
        localStore.load(new FileInputStream(trustStoreFile), trustStorePass.toCharArray());
        Enumeration<String> certlist = localStore.aliases();

        // walk through all the certs in the trustStore file and add
        // each one to the store we use to init the SSL context
        while (certlist.hasMoreElements()) {
            String alias = certlist.nextElement();
            X509Certificate cert = (X509Certificate) localStore.getCertificate(alias);
            TrustedCertificate item = new TrustedCertificate();

            item.setCertAlias(alias);
            item.setIssuedTo(cert.getSubjectDN().toString());
            item.setIssuedBy(cert.getIssuerDN().toString());
            item.setDateValid(cert.getNotBefore().toString());
            item.setDateExpire(cert.getNotAfter().toString());

            trustCatalog.add(item);
        }

        return (trustCatalog);
    }

    /**
     * Adds an uploaded certificate to the trust catalog
     * 
     * @param certAlias
     *        The alias for the certificate
     * @param certBytes
     *        The raw certificate data
     * @return The result
     */
    public static ExecManagerResult addTrustedCertificate(String certAlias, byte[] certBytes)
    {
        // make sure the certificate is a reasonable size
        if (certBytes.length > 10240) return new ExecManagerResult(1, "Uploaded file seems too large to be a valid certificate");

        // look for the head and tail markers
        String certString = new String(certBytes);
        if ((certString.contains("BEGIN CERTIFICATE") == false) || (certString.contains("END CERTIFICATE") == false)) return new ExecManagerResult(2, "Certificates should be DER-encoded text files in Base64 format and must start with -----BEGIN CERTIFICATE----- and end with -----END CERTIFICATE-----");

        FileInputStream trustStoreFileInputStream = null;
        FileOutputStream trustStoreFileOutputStream = null;
        try {
            // convert the uploaded certificate file to an actual cert object
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream stream = new ByteArrayInputStream(certBytes);
            X509Certificate certObject = (X509Certificate) factory.generateCertificate(stream);

            // get a new KeyStore instance
            KeyStore localStore = KeyStore.getInstance("JKS");

            // see if there is an existing local trustStore file
            File tester = new File(trustStoreFile);

            // found the file so load the existing certificates
            if (tester.exists() == true) {
                trustStoreFileInputStream = new FileInputStream(trustStoreFile);
                localStore.load(trustStoreFileInputStream, trustStorePass.toCharArray());

                // make sure they don't attempt to overwrite an existing cert
                if (localStore.containsAlias(certAlias) == true) return new ExecManagerResult(3, "The alias specified is already assigned to an existing certificate.");
            }

            // trust store file doesn't exist so initialize new empty store
            else {
                localStore.load(null, trustStorePass.toCharArray());
            }

            // add the new certificate to the KeyStore
            localStore.setCertificateEntry(certAlias, certObject);

            // write the updated KeyStore to the trustStore file
            trustStoreFileOutputStream = new FileOutputStream(trustStoreFile);
            localStore.store(trustStoreFileOutputStream, trustStorePass.toCharArray());

            // return success code and the cert subject
            return new ExecManagerResult(0, certObject.getSubjectDN().toString());
        }

        catch (Exception exn) {
            return new ExecManagerResult(100, "Exception processing certificate: " + exn.getMessage());
        }finally{
            if(trustStoreFileOutputStream != null){
                try{
                    trustStoreFileOutputStream.close();
                }catch(Exception e){
                    logger.error(e);
                }
            }
            if(trustStoreFileInputStream != null){
                try{
                    trustStoreFileInputStream.close();
                }catch(Exception e){
                    logger.error(e);
                }
            }
        }
    }

    /**
     * Removes an uploaded certificate from the trust catalog
     * 
     * @param certAlias
     *        The alias of the certificate to remove
     * @return True if found and removed, owtherwise false
     */
    public static boolean removeTrustedCertificate(String certAlias)
    {
        boolean result = false;
        // if the trust store file doesn't exist we can't do anything
        File tester = new File(trustStoreFile);
        if (tester.exists() == false) return (false);

        try(
            FileInputStream trustStoreFileInputStream = new FileInputStream(trustStoreFile);
            FileOutputStream trustStoreFileOutputStream = new FileOutputStream(trustStoreFile);
        ) {
            // get a new KeyStore instance
            KeyStore localStore = KeyStore.getInstance("JKS");

            // load the trust store file
            localStore.load(trustStoreFileInputStream, trustStorePass.toCharArray());

            // make sure the alias to delete exists
            if (localStore.containsAlias(certAlias) == true){
                // add the new certificate to the KeyStore
                localStore.deleteEntry(certAlias);

                // write the updated KeyStore to the trustStore file
                localStore.store(trustStoreFileOutputStream, trustStorePass.toCharArray());

                // return success code and the cert subject
                result = true;
            }

        }catch (Exception exn) {
            logger.debug("Exception removing certificate: " + exn);
        }

        return result;
    }
}
