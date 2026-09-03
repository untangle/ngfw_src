/**
 * $Id: $
 */
package com.untangle.app.http;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for Extract host name
 */
public class HttpUtility {

    // these are used while extracting the SNI from the SSL ClientHello packet
    private static int TLS_HANDSHAKE = 0x16;
    private static int CLIENT_HELLO = 0x01;
    private static int SERVER_NAME = 0x0000;
    private static int HOST_NAME = 0x00;
    private static int ENCRYPTED_CLIENT_HELLO = 0xfe0d;
    public static final String ECH_BLOCKED= "encrypted_client_hello";

    private static final HttpUtility INSTANCE = new HttpUtility();
    
    private static final Logger logger = LogManager.getLogger(HttpUtility.class);

    /**
     * private constructor
     */
    private HttpUtility(){}

    /**
     * Get the global HttpUtility singleton
     * @return the INSTANCE
     */
    public static HttpUtility getInstance()
    {
        return INSTANCE;
    }

    /**
     * Function for extracting the SNI hostname from the client request.
     * @param data
     * @param isEchBlocked
     * @return The SNI hostname extracted from the client request, or null
     * @throws Exception
     */
    public static String extractSniHostname(ByteBuffer data, boolean isEchBlocked) throws Exception{
        int counter = 0;
        int pos=0;

        logger.debug("Searching for SNI in " + data.toString());
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
            if (legacyType != CLIENT_HELLO) throw new TlsHandshakeException("Packet contains an invalid SSL handshake value "+ legacyType);
            // looks like a valid handshake message but the protocol does
            // not support SNI so we just return null
            logger.debug("No SNI available because SSLv2Hello was detected");
            return (null);
        }

        // not SSLv2Hello so proceed with TLS based on the table describe above
        if (recordType != TLS_HANDSHAKE) throw new TlsHandshakeException("Packet contains an invalid SSL handshake value " + recordType);
               
        int sslVersion = data.getShort();
        int recordLength = Math.abs(data.getShort());

        // make sure we have a ClientHello message
        int shakeType = Math.abs(data.get());
        if (shakeType != CLIENT_HELLO) throw new TlsHandshakeException("Packet does not contain TLS ClientHello value "+  shakeType);    
           
        // extract all the handshake data so we can get to the extensions
        int messageExtra = data.get();
        int messageLength = data.getShort();
        int clientVersion = data.getShort();
        int clientTime = data.getInt();
        
        setDataPositions(data, pos);

        // if position equals recordLength plus five we know this is the end
        // of the packet and thus there are no extensions - will normally
        // be equal but we include the greater than just to be safe
        if (data.position() >= (recordLength + 5)){
            logger.debug("No extensions found in TLS handshake message");
            return (null);
        }
        return extractSniHostNameFromExtensions(data, counter, isEchBlocked);
    }
    
    /**
    * Set data positions
    * @param data
    * @param pos
    * @throws Exception
    */
    public static void setDataPositions(ByteBuffer data, int pos) throws Exception{
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
        
    }
    
    /**
     * Process all extensions to find the SNI signature.
     * @param data
     * @param counter
     * @param isEchBlocked
     * @return The extracted SNI hostname, or null if not found.
     */
    public static String extractSniHostNameFromExtensions(ByteBuffer data, int counter, boolean isEchBlocked) {

        // get the total size of extension data block
        int extensionLength = Math.abs(data.getShort());
        boolean encryptedClientHelloFound = false;
        // if ECH check enbled check for ech extention 
        if(isEchBlocked){
            encryptedClientHelloFound = checkEchExtension(extensionLength, data.duplicate(),counter);
        }
         // walk through all of the extensions looking for SNI signature
         while (counter < extensionLength) {
            if (data.remaining() < 2) throw new BufferUnderflowException();
            int extType = Math.abs(data.getShort());
            int extSize = Math.abs(data.getShort());

            // if not server name extension adjust the offset to the next
            // extension record and continue
            if (extType != SERVER_NAME) {
                if (data.remaining() < extSize) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // we read the name list info by passing the offset location so we
            // don't modify the position which makes it easier to skip over the
            // whole extension if we bail out during name extraction
            if (data.remaining() < 6) throw new BufferUnderflowException();
            int listLength = Math.abs(data.getShort(data.position()));
            int nameType = Math.abs(data.get(data.position() + 2));
            int nameLength = Math.abs(data.getShort(data.position() + 3));

            // if we find a name type we don't understand we just abandon
            // processing the rest of the extension
            if (nameType != HOST_NAME) {
                if (data.remaining() < extSize) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }
            // found a valid host name so adjust the position to skip over
            // the list length and name type info we directly accessed above
            if (data.remaining() < 5) throw new BufferUnderflowException();
            String hostname = extractedSNIHostname(data, nameLength);
            //check for ech extention and encrypted hostname, if found return encrypted_client_hello
            if(encryptedClientHelloFound && StringUtils.isEmpty(hostname)){
                return ECH_BLOCKED;
            }
            return hostname;
        }
        return null;
    }

    /**
     * Check for ECH extention
     * @param extensionLength
     * @param data
     * @param counter
     * @return encryptedClientHelloFound
     */
    public static boolean checkEchExtension(int extensionLength, ByteBuffer data, int counter){
        while (counter < extensionLength) {
            if (data.remaining() < 2) throw new BufferUnderflowException();

            int extType = data.getShort() & 0xFFFF;
            int extSize = Math.abs(data.getShort());
            // Check for "encrypted client hello" extension first
            if (extType == ENCRYPTED_CLIENT_HELLO) {
                return true;
            }
    
            // If not "encrypted client hello", process other extensions
            if (data.remaining() < extSize) throw new BufferUnderflowException();
            data.position(data.position() + extSize);
            counter += (extSize + 4);
        }
        return false;
    }

    /**
     * Extracth hostName based on nameLength
     * @param data
     * @param nameLength
     * @return hostName
     */
    public static String extractedSNIHostname(ByteBuffer data, int nameLength){
            data.position(data.position() + 5);
            byte[] hostData = new byte[nameLength];
            data.get(hostData, 0, nameLength);
            String hostName = new String(hostData);
            logger.debug("Extracted SNI hostname =  {}", hostName);
            return hostName.toLowerCase();
    }
}