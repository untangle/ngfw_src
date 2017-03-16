/**
 * $Id: SslInspectorManager.java,v 1.00 2017/03/03 19:29:24 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */

package com.untangle.app.ssl_inspector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.MessageDigest;
import java.security.KeyStore;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.io.File;

import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.GlobUtil;

import org.apache.log4j.Logger;

class SslInspectorManager
{
    private static String keyStorePath = "/var/cache/untangle-ssl";
    private static String keyStorePass = "password";

    /*
     * These HashMaps are used when creating a MitM certificate. They define the
     * components of the certificate DN that will be extracted from the real
     * server cert and used to create the fake certificate.
     */

    private static HashMap<String, String> validSubjectList = new HashMap<String, String>();
    private static HashMap<Integer, String> validAlternateList = new HashMap<Integer, String>();

    // This is the list of subject name tags that we know work with the
    // openssl utility. The key is the tag we retrieve from the server cert
    // by calling X509Certificate.getSubjectDN().getName() and the object
    // is the exact string that openssl expects to be passed in the subject.
    static {
        validSubjectList.put("C", "C");
        validSubjectList.put("CN", "CN");
        validSubjectList.put("EMAILADDRESS", "emailAddress");
        validSubjectList.put("L", "L");
        validSubjectList.put("O", "O");
        validSubjectList.put("OU", "OU");
        validSubjectList.put("ST", "ST");
    }

    // This is the list of subject alternative name types we extract from
    // the server certificate and use when generating our fake certificate
    static {
        validAlternateList.put(0x01, "email");
        validAlternateList.put(0x02, "DNS");
        validAlternateList.put(0x06, "URI");
        validAlternateList.put(0x07, "IP");
        validAlternateList.put(0x08, "RID");
    }

    public enum ProtocolList
    {
        CLIENT, SERVER
    }

    private final Logger logger = Logger.getLogger(getClass());
    private final AppTCPSession session;
    private final SslInspectorApp node;

    private X509Certificate peerCertificate;
    private ByteBuffer casingBuffer;
    private SSLContext sslContext;
    private SSLEngine sslEngine;
    private boolean clientSide;
    private boolean dataMode;

    public static final String CERTIFICATE_GENERATOR_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-certgen";
    public static final byte IPC_RELEASE_MESSAGE[] = "SSL_INSPECTOR_IPC_RELEASE".getBytes();
    public static final byte IPC_DESTROY_MESSAGE[] = "SSL_INSPECTOR_IPC_DESTROY".getBytes();
    public static final byte IPC_WAKEUP_MESSAGE[] = "SSL_INSPECTOR_IPC_WAKEUP".getBytes();

    // these are used while extracting the SNI from the SSL ClientHello packet
    private static int TLS_HANDSHAKE = 0x16;
    private static int CLIENT_HELLO = 0x01;
    private static int SERVER_NAME = 0x0000;
    private static int HOST_NAME = 0x00;

    public boolean tlsFlagClient;
    public boolean tlsFlagServer;

    public SslInspectorManager(AppTCPSession session, boolean clientSide, SslInspectorApp node)
    {
        this.session = session;
        this.node = node;
        this.tlsFlagClient = false;
        this.tlsFlagServer = false;

        this.clientSide = clientSide;
        this.dataMode = false;

        casingBuffer = ByteBuffer.allocate(8192);
    }

    // ---------- Functions to initialize the SSL manager SSLEngine

    public void initializeEngine()
    {
        try {
            if (clientSide) {
                // the server side casing should be finished with the
                // SSL handshake by now so we grab the peer certificate
                SslInspectorManager server = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
                if (server == null) throw new Exception("Server SslInspectorManager attachment is missing");

                X509Certificate cert = server.getPeerCertificate();
                if (cert == null) throw new Exception("Server SslInspectorManager certificate is missing");

                logger.debug("Initializing CLIENT SSLEngine");
                initializeClientEngine(cert);
            }

            else {
                // we need the SNI hostname from the client side casing
                SslInspectorManager client = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
                if (client == null) throw new Exception("Client SslInspectorManager attachment is missing");

                String sniHostname = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);

                logger.debug("Initializing SERVER SSLEngine");
                initializeServerEngine(sniHostname);
            }
        }

        catch (Exception exn) {
            logger.error("Unable to initialize " + (clientSide ? "client" : "server") + " encryption engine", exn);
        }
    }

    // ---------- Function to initialize the client side SSLEngine

    /*
     * Initialize the client side engine. Since we're doing man-in-the-middle
     * our client side acts like an SSL server, so no TrustStore needed here.
     * For the KeyStore we need to use a fake certificate with a CN that matches
     * the CN in the cert received on the server side of the casing. We load
     * these certs from a common shared directory so they can be re-used by all
     * instances once they have been created. If the cert file doesn't yet exist
     * we call the generateFakeCertificate() function to create.
     */

    public void initializeClientEngine(X509Certificate baseCert) throws Exception
    {
        String mailCertFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getMailCertificate().replaceAll("\\.pem", "\\.pfx");
        InterfaceSettings intfSettings = UvmContextFactory.context().networkManager().findInterfaceId(session.getClientIntf());

        boolean isInbound = false;
        if (intfSettings != null) isInbound = intfSettings.getIsWan();
        KeyStore keyStore = null;

        // for inbound SMTP we use the certificate assigned for scanning STARTTLS traffic
        if ((session.getServerPort() == 25) && isInbound) {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(mailCertFile), CertificateManager.CERT_FILE_PASSWORD.toCharArray());
        }

        // for everything else we generate a fake cert that mimics the server cert
        else {
            // certs are now stored in files named with the SHA-1 thumbprint
            // instead of the old method of using the host name contained in the
            // server certificate which could cause us to load the wrong one
            String certThumbprint = generateThumbPrint(baseCert);
            String certFileName = (certThumbprint + ".p12");
            String certPathFile = keyStorePath + "/" + certFileName;
            String certHostName = new String();

            // grab the subject distinguished name from the certificate
            LdapName ldapDN = new LdapName(baseCert.getSubjectDN().getName());

            // we only want the CN from the certificate
            for (Rdn rdn : ldapDN.getRdns()) {
                if (rdn.getType().equals("CN") == false) continue;
                certHostName += rdn.getValue().toString();
                break;
            }

            // by doing sync on the keyStorePath we prevent a certificate from
            // being generated multiple times when several threads all attempt
            // to create a certificate that doesn't yet exist.
            synchronized (keyStorePath) {
                // see if the cert file already exists
                File tester = new File(certPathFile);

                // file not found so call the external script to generate a new cert
                if ((tester.exists() == false) || (tester.length() == 0)) {
                    logger.info("Creating new certificate for " + certHostName + " in " + certFileName);
                    generateFakeCertificate(baseCert, certFileName);
                }

                else {
                    logger.debug("Loading existing certificate " + certPathFile);
                }
            }

            // our cert generator script creates certificates in PKCS12 format
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(certPathFile), keyStorePass.toCharArray());

            /*
             * Because Java 6 doesn't support SNI we see cases where the cert
             * returned by a server without SNI may be different than the cert
             * returned if SNI had been included in the request. I've seen this
             * with t0.gstatic.com (and also t1, t2, and t3) where the server
             * returns a generic google cert. The end result is the browser may
             * show a hostname/certname warning message, or may ignore the
             * content altogether. As a workaround, if the client included an
             * SNI hostname, we search the CN and ALT names in the standard
             * server cert to make sure it will validate. If not, we create a
             * special certificate that matches the SNI received from the
             * client.
             */

            // grab the SNI hostname so we can check against loaded cert
            String sniHostname = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            X509Certificate loadedCert = (X509Certificate) keyStore.getCertificate("default");
            if ((node.getSettings().getServerFakeHostname() == true) && (validateCertificate(loadedCert, sniHostname) == false)) {

                // we put special certs in files named with the SNI hostname 
                String hackFileName = (sniHostname + ".p12");
                String hackPathFile = keyStorePath + "/" + hackFileName;

                synchronized (keyStorePath) {
                    // see if the special file already exists
                    File special = new File(hackPathFile);

                    // file not found so call the external script to generate a new cert
                    if ((special.exists() == false) || (special.length() == 0)) {
                        logger.info("Creating new certificate for " + sniHostname + " in " + hackFileName);

                        // call the external script to generate the new certificate
                        String argList[] = new String[2];
                        argList[0] = hackFileName;
                        argList[1] = "/CN=" + sniHostname;
                        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
                        logger.debug("SCRIPT_ARGS = " + argString);
                        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);
                    }

                    else {
                        logger.debug("Loading existing certificate " + hackPathFile);
                    }
                }

                // now that we know the special cert file exists we initialize
                // the keystore again and load it with the special certificate
                keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new FileInputStream(hackPathFile), keyStorePass.toCharArray());
            }

        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keyStorePass.toCharArray());

        // pass trust_all_certificates as the trust manager for the client
        // side to prevent the SSLEngine from loading cacerts by default
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[] { trust_all_certificates }, null);
        sslEngine = sslContext.createSSLEngine();
        sslEngine.setEnabledProtocols(generateProtocolList(ProtocolList.CLIENT));

        // on the client side we act like an SSL server
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        sslEngine.setWantClientAuth(false);
    }

    // ---------- Function to initialize the server side SSLEngine

    /*
     * Initialize the server side engine. Since we're doing man-in-the-middle
     * our server side acts like an SSL client. We only need to init the
     * trustStore which configures the certs we will trust as a client. If blind
     * trust is enabled, we use our own empty trust manager which will simply
     * trust every server certificate we receive. When not enabled, we start
     * with a standard list of well know CA's which is then supplemented with
     * additional certs to trust installed by the customer. This will make it
     * easy for us to update the standard CA list without losing any custom
     * certs added by customers.
     */

    public void initializeServerEngine(String sniHostname) throws Exception
    {
        sslContext = SSLContext.getInstance("TLS");

        // TODO - Today we pass null as the key manager but some day we may
        // want to investigate the possibility of grabbing the cert and key
        // presented for authentication on the client side and put them
        // in a KeyStore that can be used to authenticate with the server.
        // No idea if this is even possible, but if so it would allow us
        // to support client to server authentication via certificate.

        // if blind trust is enabled or we are doing SMTP then we simply trust everything
        if ((node.getSettings().getServerBlindTrust() == true) || (session.getServerPort() == 25)) {
            sslContext.init(null, new TrustManager[] { trust_all_certificates }, null);
        }

        // blind trust not enabled and not SMTP so use the shared list of trusted certs
        else {
            sslContext.init(null, node.getTrustFactory().getTrustManagers(), null);
        }

        String target = (session.getServerAddr().getHostAddress().toString() + " | " + sniHostname);

        // if broken server don't include any args to disable SNI in the outbound ClientHello
        if (node.checkBrokenServer(target) == true) {
            sslEngine = sslContext.createSSLEngine();
        }

        // not broken so include args which Java seems to use for adding SNI to ClientHello
        else {
            sslEngine = sslContext.createSSLEngine(sniHostname, 443);
        }

        sslEngine.setEnabledProtocols(generateProtocolList(ProtocolList.SERVER));

        // on the server side we act like an SSL client
        sslEngine.setUseClientMode(true);
        sslEngine.beginHandshake();
    }

    // ---------- Other private functions

    // call the openssl utility to generate a fake server certificate
    private void generateFakeCertificate(X509Certificate baseCert, String certFileName) throws Exception
    {
        StringBuilder certSubject = new StringBuilder(1024);
        StringBuilder certSANlist = new StringBuilder(1024);

        // grab the subject distinguished name from the certificate
        LdapName ldapDN = new LdapName(baseCert.getSubjectDN().getName());

        // use the valid items in the SubjectDN received from the external
        // server to generate the subject field for our fake certificate
        for (Rdn rdn : ldapDN.getRdns()) {
            // force the type name to uppercase for searching the hashmap
            String strType = rdn.getType().toString().toUpperCase();
            String strValue = rdn.getValue().toString();

            // ignore any subject fields that we don't understand
            if (validSubjectList.containsKey(strType) == false) continue;

            // use the type string stored in the hashmap since openssl
            // is very picky about the case of the field names
            strType = validSubjectList.get(strType);

            // escape forward slash so openssl doesn't get confused
            // since it uses that character between subject fields
            strValue = strValue.replace("/", "\\/");

            // append the type and value to the subject string
            certSubject.append("/" + strType + "=" + strValue);
        }

        // The SAN list is stored as a collection of List's where the
        // first entry is an Integer indicating the type of name and the
        // second entry is the String holding the actual name
        Collection<List<?>> altNames = baseCert.getSubjectAlternativeNames();

        if (altNames != null) {
            Iterator<List<?>> iterator = altNames.iterator();

            while (iterator.hasNext()) {
                List<?> entry = iterator.next();
                int value = ((Integer) entry.get(0)).intValue();

                // check the entry type against the list we understand
                if (validAlternateList.containsKey(value) == false) continue;

                // use the name string from our hashmap along with the
                // value from the certificate to build our SAN list
                if (certSANlist.length() != 0) certSANlist.append(",");
                certSANlist.append(validAlternateList.get(value) + ":" + entry.get(1).toString());
            }
        }

        // call the external script to generate the new certificate
        String argList[] = new String[3];
        argList[0] = certFileName;
        argList[1] = certSubject.toString();
        argList[2] = certSANlist.toString();
        String argString = UvmContextFactory.context().execManager().argBuilder(argList);
        logger.debug("SCRIPT_ARGS = " + argString);
        UvmContextFactory.context().execManager().exec(CERTIFICATE_GENERATOR_SCRIPT + argString);
    }

    // ---------- Functions to access the SSL manager SSLEngine

    public boolean getClientSide()
    {
        return (clientSide);
    }

    public SSLEngine getSSLEngine()
    {
        return (sslEngine);
    }

    public AppTCPSession getSession()
    {
        return (session);
    }

    public ByteBuffer getCasingBuffer()
    {
        return (casingBuffer);
    }

    public X509Certificate getPeerCertificate()
    {
        return (peerCertificate);
    }

    public void setPeerCertificate(String peerAddress, X509Certificate peerCertificate)
    {
        this.peerCertificate = peerCertificate;
    }

    public boolean getDataMode()
    {
        return (dataMode);
    }

    public void setDataMode(boolean argMode)
    {
        this.dataMode = argMode;
    }

// THIS IS FOR ECLIPSE - @formatter:off

/*

This table describes the structure of the TLS ClientHello message:

Size   Description
----------------------------------------------------------------------
1      Record Content Type
2      SSL Version
2      Record Length 
1      Handshake Type
3      Message Length
2      Client Preferred Version
4      Client Epoch GMT
28     28 Random Bytes
1      Session ID Length
0+     Session ID Data
2      Cipher Suites Length
0+     Cipher Suites Data
1      Compression Methods Length
0+     Compression Methods Data
2      Extensions Length
0+     Extensions Data

This is the format of an SSLv2 client hello:

struct {
    uint16 msg_length;
    uint8 msg_type;
    Version version;
    uint16 cipher_spec_length;
    uint16 session_id_length;
    uint16 challenge_length;
    V2CipherSpec cipher_specs[V2ClientHello.cipher_spec_length];
    opaque session_id[V2ClientHello.session_id_length];
    opaque challenge[V2ClientHello.challenge_length;
} V2ClientHello;


We don't bother checking the buffer position or length here since the
caller uses the buffer underflow exception to know when it needs to wait
for more data when a full packet has not yet been received.

*/

// THIS IS FOR ECLIPSE - @formatter:on

    public String extractSNIhostname(ByteBuffer data) throws Exception
    {
        int counter = 0;
        int pos;

        // we use the first byte of the message to determine the protocol
        int recordType = Math.abs(data.get());

        // First check for an SSLv2 hello which Appendix E.2 of RFC 5246
        // says must always set the high bit of the length field
        if ((recordType & 0x80) == 0x80) {
            // skip over the next byte of the length word
            data.position(data.position() + 1);

            // get the message type
            int legacyType = Math.abs(data.get());

            // if not a valid ClientHello we throw an exception since
            // they may be blocking just this kind of invalid traffic
            if (legacyType != CLIENT_HELLO) throw new Exception("Packet contains an invalid SSL handshake");

            // looks like a valid handshake message but the protocol does
            // not support SNI so we just return null
            logger.debug("No SNI available because SSLv2Hello was detected");
            return (null);
        }

        // not SSLv2Hello so proceed with TLS based on the table describe above
        if (recordType != TLS_HANDSHAKE) throw new Exception("Packet does not contain TLS Handshake");

        int sslVersion = data.getShort();
        int recordLength = Math.abs(data.getShort());

        // make sure we have a ClientHello message
        int shakeType = Math.abs(data.get());
        if (shakeType != CLIENT_HELLO) throw new Exception("Packet does not contain TLS ClientHello");

        // extract all the handshake data so we can get to the extensions
        int messageExtra = data.get();
        int messageLength = data.getShort();
        int clientVersion = data.getShort();
        int clientTime = data.getInt();

        // skip over the fixed size client random data 
        if (data.remaining() < 28) throw new BufferUnderflowException();
        pos = data.position();
        data.position(pos + 28);

        // skip over the variable size session id data
        int sessionLength = Math.abs(data.get());
        if (sessionLength > 0) {
            if (data.remaining() < sessionLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + sessionLength);
        }

        // skip over the variable size cipher suites data
        int cipherLength = Math.abs(data.getShort());
        if (cipherLength > 0) {
            if (data.remaining() < cipherLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + cipherLength);
        }

        // skip over the variable size compression methods data
        int compLength = Math.abs(data.get());
        if (compLength > 0) {
            if (data.remaining() < compLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + compLength);
        }

        // if position equals recordLength plus five we know this is the end
        // of the packet and thus there are no extensions - will normally
        // be equal but we include the greater than just to be safe
        if (data.position() >= (recordLength + 5)) return (null);

        // get the total size of extension data block
        int extensionLength = Math.abs(data.getShort());

        // walk through all of the extensions looking for SNI signature
        while (counter < extensionLength) {
            int extType = Math.abs(data.getShort());
            int extSize = Math.abs(data.getShort());

            // if not server name extension adjust the offset to the next
            // extension record and continue
            if (extType != SERVER_NAME) {
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // we read the name list info by passing the offset location so we
            // don't modify the position which makes it easier to skip over the
            // whole extension if we bail out during name extraction
            int listLength = Math.abs(data.getShort(data.position()));
            int nameType = Math.abs(data.get(data.position() + 2));
            int nameLength = Math.abs(data.getShort(data.position() + 3));

            // if we find a name type we don't understand we just abandon
            // processing the rest of the extension
            if (nameType != HOST_NAME) {
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // found a valid host name so adjust the position to skip over
            // the list length and name type info we directly accessed above
            data.position(data.position() + 5);
            byte[] hostData = new byte[nameLength];
            data.get(hostData, 0, nameLength);
            return new String(hostData);
        }

        return (null);
    }

    public String generateThumbPrint(X509Certificate cert)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return (hexify(digest));
        }

        catch (Exception exn) {
            logger.error("Exception generating certificate thumbprint", exn);
            return (null);
        }
    }

    public static String hexify(byte bytes[])
    {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }

    public String[] generateProtocolList(ProtocolList listType)
    {
        ArrayList<String> protoList = new ArrayList<String>();

        switch (listType)
        {
        case CLIENT:
            if (node.getSettings().getClient_SSLv2Hello()) protoList.add("SSLv2Hello");
            if (node.getSettings().getClient_SSLv3()) protoList.add("SSLv3");
            if (node.getSettings().getClient_TLSv10()) protoList.add("TLSv1");
            if (node.getSettings().getClient_TLSv11()) protoList.add("TLSv1.1");
            if (node.getSettings().getClient_TLSv12()) protoList.add("TLSv1.2");
            break;
        case SERVER:
            if (node.getSettings().getServer_SSLv2Hello()) protoList.add("SSLv2Hello");
            if (node.getSettings().getServer_SSLv3()) protoList.add("SSLv3");
            if (node.getSettings().getServer_TLSv10()) protoList.add("TLSv1");
            if (node.getSettings().getServer_TLSv11()) protoList.add("TLSv1.1");
            if (node.getSettings().getServer_TLSv12()) protoList.add("TLSv1.2");
            break;
        }

        String protoArray[] = new String[protoList.size()];
        return (protoList.toArray(protoArray));
    }

    public boolean checkIPCMessage(byte[] haystack, byte[] needle)
    {
        // first make sure the haystack is large enough to contain the needle
        if (needle.length > haystack.length) return (false);

        // now check each byte returning false if we find a missmatch
        for (int x = 0; x < needle.length; x++)
            if (haystack[x] != needle[x]) return (false);

        // everything matches so return true
        return (true);
    }

    /*
     * We use the checkTls functions here to look for TLS handshake. The TLS
     * extension for SMTP defined by RFC-3207 only requires 220 in a successful
     * server reply to STARTTLS received from a client. This makes it slightly
     * more complicated to detect when the server is switching to TLS mode. Our
     * approach is to only look for the 220 response in the first server reply
     * after setting the TLS flag on the client side. If found we continue with
     * TLS handling. If not, we assume crazy things are happening and release
     * the session.
     */

    public boolean checkTlsClient(ByteBuffer data)
    {
        byte[] rawdata = data.array();
        int rawlen = data.limit();

        // make sure we have the minumum number of bytes
        if (rawlen < 9) return (false);

        if ((rawdata[0] != 'S') && (rawdata[0] != 's')) return (false);
        if ((rawdata[1] != 'T') && (rawdata[0] != 't')) return (false);
        if ((rawdata[2] != 'A') && (rawdata[0] != 'a')) return (false);
        if ((rawdata[3] != 'R') && (rawdata[0] != 'r')) return (false);
        if ((rawdata[4] != 'T') && (rawdata[0] != 't')) return (false);
        if ((rawdata[5] != 'T') && (rawdata[0] != 't')) return (false);
        if ((rawdata[6] != 'L') && (rawdata[0] != 'l')) return (false);
        if ((rawdata[7] != 'S') && (rawdata[0] != 's')) return (false);

        // the last character should be LF be we allow CR as well
        if ((rawdata[rawlen - 1] != '\n') && (rawdata[rawlen - 1] != '\r')) return (false);

        return (true);
    }

    public boolean checkTlsServer(ByteBuffer data)
    {
        byte[] rawdata = data.array();
        int rawlen = data.limit();

        if (rawdata.length < 4) return (false);

        if (rawdata[0] != '2') return (false);
        if (rawdata[1] != '2') return (false);
        if (rawdata[2] != '0') return (false);

        // the last character should be LF but we allow CR as well
        if ((rawdata[rawlen - 1] != '\n') && (rawdata[rawlen - 1] != '\r')) return (false);

        return (true);
    }

    // this function validates a certificate for a specific hostname
    // by making sure the argumented hostname matches either the CN
    // or one of the subject alt names included in the certificate
    private boolean validateCertificate(X509Certificate argCert, String argName) throws Exception
    {
        // if the argumented name is null we just return true
        if (argName == null) return (true);

        // for starters if we find wildcard characters in the arg name then
        // who knows what the hell is going on so just blindly return true
        if (argName.contains("*") == true) return (true);
        if (argName.contains("?") == true) return (true);

        String certName = new String();

        // grab the subject distinguished name from the certificate
        LdapName ldapDN = new LdapName(argCert.getSubjectDN().getName());

        // we only want the CN from the certificate
        for (Rdn rdn : ldapDN.getRdns()) {
            if (rdn.getType().equals("CN") == false) continue;
            certName += rdn.getValue().toString();
            break;
        }

        // see if the name matches the certificate common name
        logger.debug("CHECKING CN " + certName + " FOR " + argName);
        if (Pattern.matches(GlobUtil.globToRegex(certName), argName) == true) return (true);

        // The SAN list is stored as a collection of List's where the
        // first entry is an Integer indicating the type of name and the
        // second entry is the String holding the actual name
        Collection<List<?>> altNames = argCert.getSubjectAlternativeNames();

        // if there aren't any alt names then nothing else to check
        if (altNames == null) return (false);

        Iterator<List<?>> iterator = altNames.iterator();

        while (iterator.hasNext()) {
            List<?> entry = iterator.next();
            int value = ((Integer) entry.get(0)).intValue();

            // check the entry type against the list we understand
            if (validAlternateList.containsKey(value) == false) continue;

            String string = entry.get(1).toString();

            // if this alt name matches the arg name return true
            logger.debug("CHECKING ALT " + string + " FOR " + argName);
            if (Pattern.matches(GlobUtil.globToRegex(string), argName) == true) return (true);
        }

        // nothing matched above so return false
        return (false);
    }

    // this is a dumb trust manager that will blindly trust all server certs
    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            logger.debug("TrustManager.checkClientTrusted()");
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            logger.debug("TrustManager.checkServerTrusted()");
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            logger.debug("Returning NULL from TrustManager.getAcceptedIssuers()");
            return null;
        }
    };
}
