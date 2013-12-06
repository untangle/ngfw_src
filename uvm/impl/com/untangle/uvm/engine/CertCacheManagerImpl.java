/**
 * $Id: CertCacheManagerImpl.java 36469 2013-11-21 20:11:48Z dmorris $
 */

package com.untangle.uvm.engine;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.util.Hashtable;
import java.util.HashSet;
import java.net.SocketException;
import java.net.URL;
import org.apache.log4j.Logger;
import com.untangle.uvm.CertCacheManager;

public class CertCacheManagerImpl implements CertCacheManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final int prefetchTimeout = 1000;

    private static Hashtable<String, X509Certificate> certTable = new Hashtable<String, X509Certificate>();
    private static HashSet<String> certLocker = new HashSet<String>();

    public X509Certificate searchServerCertificate(String serverAddress)
    {
        logger.debug("CertCache Searching " + serverAddress);

        X509Certificate cert = certTable.get(serverAddress);

        if (cert != null) {
            logger.debug("CertCache Found " + serverAddress + " SubjectDN(" + cert.getSubjectDN().toString() + ") IssuerDN(" + cert.getIssuerDN().toString() + ")");
        }

        return (cert);
    }

    public X509Certificate fetchServerCertificate(String serverAddress)
    {
        X509Certificate serverCertificate = null;
        boolean certWaiter = false;
        int certTimer = 0;

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
        // but we give up once we've exceeded the prek timeout
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
            serverCertificate = certTable.get(serverAddress);
            logger.debug("CertLocker acquire " + serverAddress);
            return (serverCertificate);
        }

        // certWaiter was not true so we setup to fetch the certificate
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
            certTable.put(serverAddress, serverCertificate);
            logger.info("CertCache Fetch " + serverAddress + " SubjectDN(" + serverCertificate.getSubjectDN().toString() + ") IssuerDN(" + serverCertificate.getIssuerDN().toString() + ")");
        }

        // we log and ignore all socket exceptions
        catch (SocketException soc) {
            logger.warn("Socket exception fetching server certificate", soc);
        }

        // we log and ignore all other exceptions
        catch (Exception exn) {
            logger.warn("Exception fetching server certificate", exn);
        }

        // We were the first thread to prefetch this certificate so we have
        // to remember to remove the address from the certlocker.  Subsequent
        // threads will find the cert in the certTable so prefetch shouldn't
        // ever be called again for the server we just prefetched 
        synchronized (certLocker) {
            certLocker.remove(serverAddress);
            logger.debug("CertLocker cleanup " + serverAddress);
        }

        return (serverCertificate);
    }

    public void storeServerCertificate(String serverAddress, X509Certificate serverCertificate)
    {
        certTable.put(serverAddress, serverCertificate);
        logger.debug("CertCache Store " + serverAddress + " SubjectDN(" + serverCertificate.getSubjectDN().toString() + ") IssuerDN(" + serverCertificate.getIssuerDN().toString() + ")");
    }

    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    };

    private HostnameVerifier verify_all_hostnames = new HostnameVerifier()
    {
        public boolean verify(String hostname, SSLSession session)
        {
            return (true);
        }
    };
}
