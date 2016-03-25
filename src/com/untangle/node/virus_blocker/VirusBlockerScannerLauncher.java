/*
 * $Id: VirusBlockerScannerLauncher.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.virus_blocker;

import java.lang.StringBuilder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.virus_blocker.VirusScannerLauncher;
import com.untangle.node.virus_blocker.VirusScannerResult;
import com.untangle.uvm.vnet.NodeSession;

public class VirusBlockerScannerLauncher extends VirusScannerLauncher
{
    private static final String CLOUD_SCANNER_URL = "https://classify.totaldefense.com/v1/md5s";
    private static final String CLOUD_FEEDBACK_URL = "https://telemetry.totaldefense.com/ngfw/v1/infection";
    private static final String CLOUD_SCANNER_KEY = "B132C885-962B-4D63-8B2F-441B7A43CD93";
    private static final String EICAR_TEST_MD5 = "44d88612fea8a8f36de82e1278abb02f";
    private static final long CLOUD_SCAN_MAX_MILLISECONDS = 2000;

    private static final String BDAM_SCANNER_HOST = "127.0.0.1";
    private static final long SCANNER_MAXSIZE = 10485760;
    private static final long SCANNER_MINSIZE = 1;
    private static final int BDAM_SCANNER_PORT = 1344;

    // -----------------------------------------------------------
    // ---------- cloud scanning and telemetry feedback ----------
    // -----------------------------------------------------------

    @SuppressWarnings("serial")
    protected class CloudResult implements Serializable, JSONString
    { 
        String itemCategory = null;
        String itemClass = null;
        String itemConfidence = null;
        String itemHash = null;

        public String getItemCategory() { return itemCategory; }
        public void   setItemCategory(String newValue) { this.itemCategory = newValue; }
        public String getItemClass() { return itemClass; }
        public void   setItemClass(String newValue) { this.itemClass = newValue; }
        public String getItemConfidence() { return itemConfidence; }
        public void   setItemConfidence(String newValue) { this.itemConfidence = newValue; }
        public String getItemHash() { return itemHash; }
        public void   setItemHash(String newValue) { this.itemHash = newValue; }

        public String toJSONString()
        {
            JSONObject jO = new JSONObject(this);
            return jO.toString();
        }
    }

    protected class CloudScanner extends Thread
    {
        CloudResult cloudResult = null;
        VirusBlockerState virusState = null;

        public CloudScanner(VirusBlockerState virusState)
        {
            this.virusState = virusState;
        }

        protected synchronized CloudResult getCloudResult()
        {
            return cloudResult;
        }

        protected synchronized void setCloudResult(CloudResult argResult)
        {
            this.cloudResult = argResult;
        }

        public void run()
        {
            StringBuilder builder = new StringBuilder(256);
            CloudResult cloudResult = new CloudResult();

            // ----- uncomment this string to force cloud detection for testing -----
            // String body = "[\n\"" + EICAR_TEST_MD5 + "\"\n]\n";
            String body = "[\n\"" + virusState.fileHash + "\"\n]\n";

            logger.debug("CloudScanner thread has started for: " + body);

            try {
                URL myurl = new URL(CLOUD_SCANNER_URL);
                HttpsURLConnection mycon = (HttpsURLConnection) myurl.openConnection();
                mycon.setRequestMethod("POST");

                mycon.setRequestProperty("Content-length", String.valueOf(body.length()));
                mycon.setRequestProperty("Content-Type", "application/json");
                mycon.setRequestProperty("User-Agent", "Untangle NGFW Virus Blocker");
                mycon.setRequestProperty("UID", UvmContextFactory.context().getServerUID());
                mycon.setRequestProperty("AuthRequest", CLOUD_SCANNER_KEY);
                mycon.setDoOutput(true);
                mycon.setDoInput(true);

                DataOutputStream output = new DataOutputStream(mycon.getOutputStream());
                output.writeBytes(body);
                output.close();

                DataInputStream input = new DataInputStream(mycon.getInputStream());

                // build a string from the cloud response skipping brackets and parenthesis
                for (int c = input.read(); c != -1; c = input.read()) {
                    if ((char) c == '[') continue;
                    if ((char) c == ']') continue;
                    if ((char) c == '{') continue;
                    if ((char) c == '}') continue;
                    builder.append((char) c);
                }

                input.close();

                mycon.disconnect();

                logger.debug("CloudScanner CODE:" + mycon.getResponseCode() + " MSG:" + mycon.getResponseMessage() + " DATA:" + builder.toString());

// THIS IS FOR ECLIPSE - @formatter:off

                /*
                 * This is an example of the message we get back from the cloud server. The insane
                 * code below is my solution for parsing the response into a result we can use.
                 * This WILL break if there are : " , characters within any item or value field.
                 *   
                 * [{"Category":"The EICAR Test String!16","Class":"m","Confidence":100,"Item":"44d88612fea8a8f36de82e1278abb02f"}]
                 * 
                 * Also worth noting... for a negative result the cloud server seems to return this:
                 * []
                 * 
                 */

// THIS IS FOR ECLIPSE - @formatter:on

                // split the string on commas to find all of the item and value pairs
                String[] tokens = builder.toString().split(",");

                for (int x = 0; x < tokens.length; x++) {
                    // split the pair on the colon to get the item and value
                    String[] list = tokens[x].split(":");
                    if (list.length != 2) continue;

                    // seems like a good time to remove quotation marks
                    String item = list[0].replace("\"", "");
                    String value = list[1].replace("\"", "");

                    // look for the stuff we know about and store in result object when found
                    if (item.compareTo("Category") == 0) cloudResult.itemCategory = value;
                    if (item.compareTo("Class") == 0) cloudResult.itemClass = value;
                    if (item.compareTo("Confidence") == 0) cloudResult.itemConfidence = value;
                    if (item.compareTo("Item") == 0) cloudResult.itemHash = value;
                }
            }

            catch (Exception exn) {
                logger.debug("CloudScanner thread exception: " + exn.toString());
            }

            setCloudResult(cloudResult);

            synchronized (this) {
                this.notify();
            }
        }
    }

    protected class CloudFeedback extends Thread
    {
        VirusBlockerState virusState = null;
        long fileLength = 0;
        NodeSession session = null;
        String bitdefenderResult = null;
        CloudResult cloudResult = null;
        
        public CloudFeedback(VirusBlockerState virusState, String bitdefenderResult, long fileLength, NodeSession session, CloudResult cloudResult)
        {
            this.virusState = virusState;
            this.bitdefenderResult = bitdefenderResult;
            this.fileLength = fileLength;
            this.session = session;
            this.cloudResult = cloudResult;
        }

        public void run()
        {
            StringBuilder feedback = new StringBuilder(256);
            JSONObject json = new JSONObject();

            try {
                json.put("hash", virusState.fileHash);
                json.put("length", fileLength);
                json.put("bitdefenderResult", bitdefenderResult);
                json.put("cloudResult", cloudResult);
                if ( session != null ) {
                    if ( session.globalAttachment( NodeSession.KEY_HTTP_HOSTNAME ) != null )
                        json.put(NodeSession.KEY_HTTP_HOSTNAME, session.globalAttachment( NodeSession.KEY_HTTP_HOSTNAME ));
                    if ( session.globalAttachment( NodeSession.KEY_HTTP_URI ) != null )
                        json.put(NodeSession.KEY_HTTP_URI, session.globalAttachment( NodeSession.KEY_HTTP_URI ));
                    if ( session.globalAttachment( NodeSession.KEY_HTTP_URL ) != null )
                        json.put(NodeSession.KEY_HTTP_URL, session.globalAttachment( NodeSession.KEY_HTTP_URL ));
                    if ( session.globalAttachment( NodeSession.KEY_HTTP_REFERER ) != null )
                        json.put(NodeSession.KEY_HTTP_REFERER, session.globalAttachment( NodeSession.KEY_HTTP_REFERER ));
                    if ( session.globalAttachment( NodeSession.KEY_FTP_FILE_NAME ) != null )
                        json.put(NodeSession.KEY_FTP_FILE_NAME, session.globalAttachment( NodeSession.KEY_FTP_FILE_NAME ));
                    if ( session.getOrigClientAddr() != null )
                        json.put("clientAddr", session.getOrigClientAddr().getHostAddress() );
                    if ( session.getNewServerAddr() != null )
                        json.put("serverAddr", session.getNewServerAddr().getHostAddress() );
                    json.put("clientPort", session.getOrigClientPort() );
                    json.put("serverPort", session.getNewServerPort() );
                    if ( session.getAttachments() != null )
                        json.put("attachments", session.getAttachments() );
                }
                
            } catch (Exception exn) {
                logger.warn("Exception building CloudFeedback JSON object.", exn);
            }

            logger.warn("XXXX " + json.toString());
            feedback.append(json.toString());

            logger.debug("CloudFeedback thread has started for: " + feedback.toString());

            try {
                String target = (CLOUD_FEEDBACK_URL + "?hash=" + virusState.fileHash + "&det=" + bitdefenderResult + "&detProvider=BD&metaProvider=NGFW");
                URL myurl = new URL(target);
                HttpsURLConnection mycon = (HttpsURLConnection) myurl.openConnection();
                mycon.setRequestMethod("POST");

                mycon.setRequestProperty("Content-length", String.valueOf(feedback.length()));
                mycon.setRequestProperty("User-Agent", "Untangle NGFW Virus Blocker");
                mycon.setRequestProperty("UID", UvmContextFactory.context().getServerUID());
                mycon.setRequestProperty("AuthRequest", CLOUD_SCANNER_KEY);
                mycon.setDoOutput(true);
                mycon.setDoInput(true);

                DataOutputStream output = new DataOutputStream(mycon.getOutputStream());
                output.writeBytes(feedback.toString());
                output.close();

                mycon.disconnect();

                logger.debug("CloudFeedback CODE:" + mycon.getResponseCode() + " MSG:" + mycon.getResponseMessage());
            }

            catch (Exception exn) {
                logger.debug("CloudFeedback thread exception: " + exn.toString());
            }
        }
    }

    // ------------------------------------------------------------
    // ---------- the scanner launcher stuff starts here ----------
    // ------------------------------------------------------------

    /**
     * Create a Launcher for the give file
     */
    public VirusBlockerScannerLauncher(File scanfile, NodeSession session)
    {
        super(scanfile, session);
    }

    /**
     * This runs the virus scan, and stores the result for retrieval. Any
     * threads in waitFor() are awoken so they can retrieve the result
     */
    public void run()
    {
        File scanFile = new File(scanfilePath);
        long scanFileLength = scanFile.length();
        CloudScanner cloudScanner = null;
        CloudResult cloudResult = null;
        String virusName = null;

        VirusBlockerState virusState = (VirusBlockerState) nodeSession.attachment();

        logger.debug("Scanning file: " + scanfilePath + " MD5: " + virusState.fileHash);

        try {
            // Bug #9796 - to avoid large memory usage don't scan large files // XXX
            if (scanFileLength > SCANNER_MAXSIZE) {
                logger.debug("Passing large file: " + (scanFile.length() / 1024) + "K");
                setResult(VirusScannerResult.CLEAN);
                return;
            }
            // ignore small or empty files
            if (scanFileLength < SCANNER_MINSIZE) {
                logger.debug("Passing small file: " + scanFile.length() + " bytes");
                setResult(VirusScannerResult.CLEAN);
                return;
            }

        } catch (Exception exn) {
            logger.warn("Exception checking file length: ", exn);
            setResult(VirusScannerResult.ERROR);
            return;
        }

        // if we have a good MD5 hash then spin up the cloud checker
        if (virusState.fileHash != null) {
            cloudScanner = new CloudScanner(virusState);
            cloudScanner.start();
        }

        DataOutputStream txstream = null;
        DataInputStream rxstream = null;
        Socket socket = null;
        byte buffer[] = new byte[256];
        long timeSeconds = 0;
        int txcount = 0;
        int rxcount = 0;

        // Transmit the scan request to the daemon and grab the response
        // Syntax = SCANFILE options filename - available options bits: (see docs for details)
        // 1 = BDAM_SCANOPT_ARCHIVES
        // 2 = BDAM_SCANOPT_PACKED
        // 4 = BDAM_SCANOPT_EMAILS
        // 8 = enable virus heuristics scanner
        // 16 = BDAM_SCANOPT_DISINFECT
        // 32 = return in-progress information
        // 64 = BDAM_SCANOPT_SPAMCHECK
        try {
            InetSocketAddress address = new InetSocketAddress(BDAM_SCANNER_HOST, BDAM_SCANNER_PORT);
            socket = new Socket();
            socket.connect(address, 10000);
            socket.setSoTimeout(10000);
            txstream = new DataOutputStream(socket.getOutputStream());
            rxstream = new DataInputStream(socket.getInputStream());
            txstream.writeBytes("SCANFILE 15 " + scanfilePath + "\r\n");
            txcount = txstream.size();
            rxcount = rxstream.read(buffer);
        } catch (Exception exn) {
            logger.warn("Exception scanning file: ", exn);
            setResult(VirusScannerResult.ERROR);
            return;
        }

        // close the streams and socket ignoring exceptions
        try {
            if (txstream != null) txstream.close();
            if (rxstream != null) rxstream.close();
            if (socket != null) socket.close();
        } catch (Exception exn) {
        }

        // REPLY EXAMPLE: 222 V Trojan.GenericKD.1359402
        // REPLY FORMAT: ccc ttt nnn
        // ccc = result code
        // ttt = malware type (Virus, Spyware, adWare, Dialer, App)
        // nnn = malware name

        String message = new String(buffer, 0, rxcount).trim();
        logger.debug("Scan result: " + message);

        // split the string on the spaces so we can find all the fields
        String[] tokens = message.split(" ");
        int retcode = 0;
        String bdResult = null;
        
        try {
            retcode = Integer.valueOf(tokens[0]);
            bdResult = tokens[2];
        } catch (Exception exn) {
            logger.warn("Exception parsing result code: " + message, exn);
        }

        if (cloudScanner != null) {
            try {
                synchronized (cloudScanner) {
                    cloudScanner.wait(CLOUD_SCAN_MAX_MILLISECONDS);
                }
            } catch (Exception exn) {
                logger.debug("Exception waiting for CloudScanner: ", exn);
            }
            cloudResult = cloudScanner.getCloudResult();
        }

        CloudFeedback feedback = null;

        /**
         * If BD returned positive result - send feedback
         * If cloud return positive result - send feedback
         */
        if (retcode == 222 || retcode == 223) {
            feedback = new CloudFeedback(virusState, bdResult, scanFileLength, nodeSession, cloudResult);
        }
        if (feedback == null && (cloudResult != null) && (cloudResult.itemCategory != null) && (cloudResult.itemConfidence != null) && (cloudResult.itemConfidence.equals("100"))) {
            feedback = new CloudFeedback(virusState, bdResult, scanFileLength, nodeSession, cloudResult);
        }
        
        // if we have a feedback object start it up now
        if (feedback != null)
            feedback.run();
        
        // if the cloud says it is infected we set the result and return now
        if ((cloudResult != null) && (cloudResult.itemCategory != null) && (cloudResult.itemConfidence != null) && (cloudResult.itemConfidence.equals("100"))) {
            setResult(new VirusScannerResult(false, cloudResult.itemCategory));
            return;
        }

        switch (retcode)
        {
        case 227: // clean
            setResult(VirusScannerResult.CLEAN);
            break;
        case 222: // known infection
            setResult(new VirusScannerResult(false, tokens[2]));
            break;
        case 223: // likely infection
            setResult(new VirusScannerResult(false, tokens[2]));
            break;
        case 225: // password protected file
            setResult(VirusScannerResult.CLEAN);
            break;
        case 221: // scan aborted or failed
            setResult(VirusScannerResult.ERROR);
            break;
        case 224: // corrupted file
            setResult(VirusScannerResult.ERROR);
            break;
        default:
            setResult(VirusScannerResult.ERROR);
            break;
        }
        return;
    }

    private void setResult(VirusScannerResult value)
    {
        this.result = value;

        synchronized (this) {
            this.notifyAll();
        }
    }
}
