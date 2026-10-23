/**
 * $Id: TrustCatalog.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import com.untangle.uvm.ExecManagerResult;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

    private static final Set<String> BLOCKED_FINGERPRINTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "42C4ACCD12C4F59597925E1A08A7A90102608543809308D50EC427E118BA1493",
        "45A883F349AEDFD5F3A649B5498FFB121712BA8B448F3F2A91DBC5A766FCDDBE",
        "98B896625C0C230ADEEC3EB2CF6AAD5001ECF9156A68644E82374CE5833BC25B",
        "5F1DED4DD590AF74631290CF073B122A2AAC6A1FA479778A0BD43579BD832699",
        "E7568FE6E12331F4FD7DE61DA4C7559D0F8EB192A218D4741A3BA75184333838",
        "579480AEA166290BD3C643589E795FC0D0E54E86505D180F1DFF99EFD3199512",
        "0164D7CEAE5BB909C272AF151FF44B1177AE9B47D9424B94F1D9558C30934D19",
        "62B7F9868263F72F6911FC78CEAF72C1979158B7368BF648683BB2AA39B02E72",
        "CB6E15165AC802E9B3F993F16C25D41F4E81C21EBDE71AE1B012AC00BF9C7C3F",
        "7485B51C0ED192878FA1D2F948FB529AE1842CC5086C4B67F7BE5B2039DE9489",
        "432EC25F0D28DE9006D97BF752F17BBB2CD41F669933FCB072D9A1148D0261F3",
        "FD9D5C2DBBD4995AB69B8D50C439D9527B00529199C2FD01B2174EB58F07021F",
        "9E39DE2881273ACC225A149C72BA3E0F51424963669AB633EE1DC7DF5DB067B4",
        "A496E23AC0CAB8155FC9B117B97EAE368EFCBB9041AEC8804D5A308D2D01C2D8",
        "4B2460A466F95FD40113BCDCCA2CEB29B3B093921DE45474DA7DF48874C630B1",
        "84112972B8E59D14560AE7E7119654474AAD119990CF3722BFC1E3B91615E1FD"
    )));

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
     * Creates trust managers loaded with the standard list of trusted CA's
     * and user-uploaded certs, wrapped with a blocklist that rejects chains
     * containing known-compromised intermediate certificates.
     *
     * @return Array of trust managers for SSLContext initialization
     * @throws Exception
     */
    public static TrustManager[] createTrustManagers() throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        FileInputStream keyStream = null;

        // first we load the standard list of trusted CA certificates
        keyStream = new FileInputStream(globalStoreFile);
        trustStore.load(keyStream, globalStorePass.toCharArray());
        keyStream.close();
        logger.info("Loaded " + trustStore.size() + " trusted certificates from " + globalStoreFile);

        // now we see if there is a local trustStore file
        File tester = new File(trustStoreFile);

        // found the file so load the certs and add each to trustStore
        if ((tester.exists() == true) && (tester.length() != 0)) {
            KeyStore localStore = KeyStore.getInstance("JKS");
            keyStream = new FileInputStream(trustStoreFile);
            localStore.load(keyStream, trustStorePass.toCharArray());
            keyStream.close();

            // walk through all the certs in the trustStore file and add
            // each one to the store we use to init the SSL context
            Enumeration<String> certlist = localStore.aliases();
            while (certlist.hasMoreElements()) {
                String alias = certlist.nextElement();
                logger.info("Adding trusted cert [" + alias + "] from " + trustStoreFile);
                trustStore.setCertificateEntry(alias, localStore.getCertificate(alias));
            }
        }

        TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
        factory.init(trustStore);

        X509TrustManager pkixManager = null;
        for (TrustManager tm : factory.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                pkixManager = (X509TrustManager) tm;
                break;
            }
        }

        if (pkixManager == null) {
            logger.error("No X509TrustManager found in PKIX factory, returning unwrapped");
            return factory.getTrustManagers();
        }

        logger.info("Wrapping trust manager with blocklist of " + BLOCKED_FINGERPRINTS.size() + " compromised intermediates");
        return new TrustManager[] { new BlocklistTrustManager(pkixManager) };
    }

    private static String sha256Fingerprint(X509Certificate cert) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02X", b & 0xff));
        }
        return sb.toString();
    }

    private static class BlocklistTrustManager implements X509TrustManager
    {
        private final X509TrustManager delegate;

        BlocklistTrustManager(X509TrustManager delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            for (X509Certificate cert : chain) {
                try {
                    String fingerprint = sha256Fingerprint(cert);
                    if (BLOCKED_FINGERPRINTS.contains(fingerprint)) {
                        logger.warn("Blocked compromised intermediate certificate: " + cert.getSubjectX500Principal() + " SHA-256: " + fingerprint);
                        throw new CertificateException("Certificate chain contains blocked compromised intermediate: " + cert.getSubjectX500Principal());
                    }
                } catch (CertificateException ce) {
                    throw ce;
                } catch (Exception exn) {
                    logger.error("Exception computing certificate fingerprint", exn);
                }
            }
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return delegate.getAcceptedIssuers();
        }
    }

    /**
     * Gets a list of all trusted certs that have been uploaded
     * 
     * @return The list of all trusted certs that have been uploaded
     * @throws Exception
     */
    public static LinkedList<TrustedCertificate> getTrustCatalog() throws Exception
    {
        LinkedList<TrustedCertificate> trustCatalog = new LinkedList<TrustedCertificate>();

        // make sure there is a local trustStore file
        File tester = new File(trustStoreFile);

        // if not found just return the empty catalog
        if ((tester.exists() == false) || (tester.length() == 0)) return (trustCatalog);

        KeyStore localStore = KeyStore.getInstance("JKS");
        FileInputStream keyStream = new FileInputStream(trustStoreFile);
        localStore.load(keyStream, trustStorePass.toCharArray());
        keyStream.close();

        // walk through all the certs in the trustStore file and add
        // each one to the list that will be returned to the caller
        Enumeration<String> certlist = localStore.aliases();
        while (certlist.hasMoreElements()) {
            String alias = certlist.nextElement();
            X509Certificate cert = (X509Certificate) localStore.getCertificate(alias);
            TrustedCertificate item = new TrustedCertificate();

            item.setCertAlias(alias);
            item.setIssuedTo(cert.getSubjectX500Principal().toString());
            item.setIssuedBy(cert.getIssuerX500Principal().toString());
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

        try {
            // convert the uploaded certificate file to an actual cert object
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream stream = new ByteArrayInputStream(certBytes);
            X509Certificate certObject = (X509Certificate) factory.generateCertificate(stream);
            stream.close();

            // get a new KeyStore instance
            KeyStore localStore = KeyStore.getInstance("JKS");

            // see if there is an existing local trustStore file
            File tester = new File(trustStoreFile);

            // found the file so load the existing certificates
            if ((tester.exists() == true) && (tester.length() != 0)) {
                FileInputStream iStream = new FileInputStream(trustStoreFile);
                localStore.load(iStream, trustStorePass.toCharArray());
                iStream.close();

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
            FileOutputStream oStream = new FileOutputStream(trustStoreFile);
            localStore.store(oStream, trustStorePass.toCharArray());
            oStream.close();

            // return success code and the cert subject
            return new ExecManagerResult(0, certObject.getSubjectX500Principal().toString());
        }

        catch (Exception exn) {
            return new ExecManagerResult(100, "Exception processing certificate: " + exn.getMessage());
        }
    }

    /**
     * Removes an uploaded certificate from the trust catalog
     * 
     * @param certAlias
     *        The alias of the certificate to remove
     * @return True if found and removed, otherwise false
     */
    public static boolean removeTrustedCertificate(String certAlias)
    {
        FileInputStream iStream = null;
        FileOutputStream oStream = null;
        KeyStore localStore = null;

        // if the trust store file doesn't exist we can't do anything
        File tester = new File(trustStoreFile);
        if ((tester.exists() == false) || (tester.length() == 0)) return (false);

        // get an empty keystore
        try {
            localStore = KeyStore.getInstance("JKS");
        } catch (Exception exn) {
            logger.warn("Exception calling KeyStore.getInstance", exn);
            return (false);
        }

        // load the keystore from the file
        try {
            iStream = new FileInputStream(trustStoreFile);
            localStore.load(iStream, trustStorePass.toCharArray());
            iStream.close();
        } catch (Exception exn) {
            logger.error("Exception loading keystore: " + trustStoreFile, exn);
            return (false);
        }

        // make sure the alias to be removed exists
        try {
            if (localStore.containsAlias(certAlias) == false) return (false);
        } catch (Exception exn) {
            logger.error("Exception checking alias: " + certAlias, exn);
            return (false);
        }

        // remove the alias from the keystore
        try {
            localStore.deleteEntry(certAlias);
        } catch (Exception exn) {
            logger.error("Exception removing alias: " + certAlias, exn);
            return (false);
        }

        // write the updated keystore to the file
        try {
            oStream = new FileOutputStream(trustStoreFile);
            localStore.store(oStream, trustStorePass.toCharArray());
            oStream.close();
        } catch (Exception exn) {
            logger.error("Exception saving keystore: " + trustStoreFile, exn);
            return (false);
        }

        // return true for success
        return (true);
    }
}
