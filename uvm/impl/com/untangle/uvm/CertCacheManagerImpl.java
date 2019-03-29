/**
 * $Id$
 */

package com.untangle.uvm;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.URL;
import org.apache.log4j.Logger;
import com.untangle.uvm.CertCacheManager;

/**
 * The Certificate Cache Manager is used to fetch and cache SSL certificates
 * from external servers. This allows us to do rule processing and make other
 * decisions based on the cert contents. It was orignally written for use in the
 * SSL Inspector to allow cert based ignore rules, but later moved into the uvm
 * for use by web filter and possibly other apps. We need this because an SSL
 * connection doesn't see the server certificate until after the handshake has
 * started, at which point it is too late to cleanly release the session. To
 * solve the problem we open a separate connection to the server to fetch the
 * certificate. The certs are then cached by server IP address so the extra
 * connection and fetch overhead only happens the first time we connect to the
 * server. Since many browsers do the pipelining thing, it's possible that
 * multiple connections will be initiated at nearly the same time, and they
 * could all race to do the initial prefetch. To work around this, the fetch
 * logic synchronizes on certLocker which tracks active prefetch operations.
 * This ensures that only the first thread does the actual fetch, allowing
 * others to simply wait until the fetch is complete. We also do negative
 * caching so we don't repeatedly try to fetch the certificate from servers that
 * are not responding, or update the certificate too frequently.
 */
public class CertCacheManagerImpl implements CertCacheManager
{
    private static ConcurrentHashMap<String, CertificateHolder> certTable = new ConcurrentHashMap<>();
    private static HashSet<String> certLocker = new HashSet<>();

    private final Logger logger = Logger.getLogger(getClass());
    private final long cacheTimeout = 60000;
    private final int prefetchTimeout = 1000;

    /**
     * Holder for certificates that are cached in our hash map
     */
    class CertificateHolder
    {
        /**
         * Constructor
         * 
         * @param argCert
         *        The certificate
         */
        CertificateHolder(X509Certificate argCert)
        {
            creationTime = System.currentTimeMillis();
            ourCert = argCert;
        }

        /**
         * Constructor
         */
        CertificateHolder()
        {
            creationTime = System.currentTimeMillis();
            ourCert = null;
        }

        /**
         * Gets the certificate
         * 
         * @return The certificate
         */
        X509Certificate getCertificate()
        {
            return (ourCert);
        }

        /**
         * Checks to see if a certificate should be refreshed
         * 
         * @return True if refresh needed, otherwise false
         */
        boolean checkUpdateTimer()
        {
            long currentTime = System.currentTimeMillis();
            if ((creationTime + cacheTimeout) > currentTime) return (false);
            return (true);
        }

        private X509Certificate ourCert;
        private long creationTime;
    }

    /**
     * Gets and returns a server certificate to the caller. If we have the
     * certificate in our cache, we return it directly. If not, we fetch it from
     * the server, store it in our cache, and return it to the caller.
     * 
     * @param serverAddress
     *        The server address
     * @return The certificate, or null if not available
     */
    public X509Certificate fetchServerCertificate(String serverAddress)
    {
        X509Certificate serverCertificate = null;
        CertificateHolder certHolder = null;
        boolean certWaiter = false;
        int certTimer = 0;

        logger.debug("CertCache Search " + serverAddress);

        // first lets see if the certificate holder already exists
        certHolder = certTable.get(serverAddress);

        // if we found the holder and the certificate is good return it here
        if (certHolder != null) {
            serverCertificate = certHolder.getCertificate();
            if (serverCertificate != null) {
                logger.debug("CertCache Found " + serverAddress + " SubjectDN(" + serverCertificate.getSubjectDN().toString() + ") IssuerDN(" + serverCertificate.getIssuerDN().toString() + ")");
                return (serverCertificate);
            }
        }

        // we do a synchronized check on the certLocker to see if some other
        // thread is already prefetching the cert for the argumented server
        synchronized (certLocker) {
            if ((certLocker.contains(serverAddress)) == false) {
                certLocker.add(serverAddress);
                logger.debug("CertLocker fetching " + serverAddress);
            } else {
                logger.debug("CertLocker waiting " + serverAddress);
                certWaiter = true;
            }
        }

        // if certWaiter is true some other thread is prefetching the
        // certificate so we just sit here and wait for it to show up
        // but we give up once we've exceeded the prefetch timeout
        while (certWaiter == true) {
            if (certTable.containsKey(serverAddress) == false) {

                try {
                    Thread.sleep(10);
                }

                catch (Exception exn) {
                }

                certTimer += 10;

                if (certTimer > prefetchTimeout) {
                    logger.debug("CertLocker timeout " + serverAddress);
                    return (null);
                }
                continue;
            }

            // if we make it here the other thread has added the
            // cert so we grab it and return it to the caller
            certHolder = certTable.get(serverAddress);
            serverCertificate = certHolder.getCertificate();

            if (serverCertificate == null) logger.debug("CertLocker empty " + serverAddress);
            else logger.debug("CertLocker acquire " + serverAddress);

            return (serverCertificate);
        }

        // if we have the holder but didn't return the cert above it must be null
        // so we check the update timer and return null if not time try again
        if ((certHolder != null) && (certHolder.checkUpdateTimer() == false)) {
            return (null);
        }

        // certWaiter was not true and we haven't returned the cert
        // yet so we setup to fetch the certificate
        try {
            // setup the fetch URL using the server ip address
            URL url = new URL("https://" + serverAddress);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            // we only cache certs for rule matching purposes so we set the
            // connection object to use our special trust all manager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { trust_all_certificates }, null);
            con.setSSLSocketFactory(context.getSocketFactory());

            // use our special hostname verifier to effectively disable
            // hostname checking since we connect using the ip address
            con.setHostnameVerifier(verify_all_hostnames);

            // set the connect timeout and establish the connection
            con.setConnectTimeout(prefetchTimeout);
            con.connect();

            // grab the server certificates and close the connection
            Certificate[] certList = con.getServerCertificates();
            con.disconnect();

            // the first certificate should be the actual server cert so we
            // grab it and push it into the certTable and return it to caller
            serverCertificate = (X509Certificate) certList[0];
            certTable.remove(serverAddress);
            certTable.put(serverAddress, new CertificateHolder(serverCertificate));
            logger.info("CertCache Fetch " + serverAddress + " SubjectDN(" + serverCertificate.getSubjectDN().toString() + ") IssuerDN(" + serverCertificate.getIssuerDN().toString() + ")");
        }

        // we log and ignore all socket timeout exceptions 
        catch (SocketTimeoutException tex) {
            logger.debug("Socket timeout fetching server certificate from " + serverAddress);
            certTable.remove(serverAddress);
            certTable.put(serverAddress, new CertificateHolder());
        }

        // we log and ignore all other socket exceptions
        catch (SocketException soc) {
            logger.debug("Socket exception fetching server certificate from " + serverAddress);
            certTable.remove(serverAddress);
            certTable.put(serverAddress, new CertificateHolder());
        }

        // we log and ignore all other exceptions
        catch (Exception exn) {
            String exmess = exn.getMessage();
            if (exmess == null) exmess = "unknown";
            logger.warn("Exception(" + exmess + ") fetching server certificate from " + serverAddress);
            certTable.remove(serverAddress);
            certTable.put(serverAddress, new CertificateHolder());
        }

        // We were the first thread to prefetch this certificate so we have
        // to remember to remove the address from the certlocker.
        synchronized (certLocker) {
            certLocker.remove(serverAddress);
            logger.debug("CertLocker cleanup " + serverAddress);
        }

        return (serverCertificate);
    }

    /**
     * When apps get server certificates in the normal course of handling
     * traffic, they call this function to uppdate the cache. For maximum
     * efficiency, we only replace the cached certificate when it has exceeded
     * the cache timeout. TODO - A future enhancement would be to compare the
     * serial numbers of the two certificates, and update the cache if we detect
     * the cert has changed.
     * 
     * @param serverAddress
     *        The server address
     * @param serverCertificate
     *        The certificate
     */
    public void updateServerCertificate(String serverAddress, X509Certificate serverCertificate)
    {
        CertificateHolder certHolder = certTable.get(serverAddress);

        // check if we found the certificate holder 
        if (certHolder != null) {
            // check if the certificate is good
            if (certHolder.getCertificate() != null) {
                // check if we are ready for an update
                if (certHolder.checkUpdateTimer() == false) {
                    // no update needed so just return
                    return;
                }
            }
        }

        // not found, null or expired certificate so replace
        certTable.remove(serverAddress);
        certTable.put(serverAddress, new CertificateHolder(serverCertificate));
        logger.debug("CertCache Update " + serverAddress + " SubjectDN(" + serverCertificate.getSubjectDN().toString() + ") IssuerDN(" + serverCertificate.getIssuerDN().toString() + ")");
    }

    /**
     * We currently only use cached certs for rule matching, not for encryption
     * or authentication, so it's safe to skip all validation.
     */
    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        /**
         * Throw nothing, trust everything
         * 
         * @param chain
         * @param authType
         * @throws CertificateException
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * Throw nothing, trust everything
         * 
         * @param chain
         * @param authType
         * @throws CertificateException
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * @return null to accept all issuers
         */
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    };

    /**
     * Since our certs are only used for rule matching, we also don't bother
     * validating the hostname, so we use this trust all class to allow
     * everything.
     */
    private HostnameVerifier verify_all_hostnames = new HostnameVerifier()
    {
        /**
         * @param hostname
         *        The hostname
         * @param session
         *        The SSL session
         * @return True to verify/allow everything
         */
        public boolean verify(String hostname, SSLSession session)
        {
            return (true);
        }
    };
}
