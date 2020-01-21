/**
 * $Id: VirusCloudScanner.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;

/**
 * Implements a cloud based virus scanner
 */
public class VirusCloudScanner extends Thread
{
    private final Logger logger = Logger.getLogger(VirusBlockerBaseApp.class);

    private static final String CLOUD_SCANNER_URL = "https://classify.untangle.com/v1/md5s";
    private static final String CLOUD_SCANNER_KEY = "B132C885-962B-4D63-8B2F-441B7A43CD93";
    private static final String EICAR_TEST_MD5 = "44d88612fea8a8f36de82e1278abb02f";

    VirusCloudResult cloudResult = null;
    VirusBlockerState virusState = null;

    /**
     * Constructor
     * 
     * @param virusState
     *        The virus state
     */
    public VirusCloudScanner(VirusBlockerState virusState)
    {
        this.virusState = virusState;
    }

    /**
     * Gets the cloud result
     * 
     * @return The result
     */
    protected synchronized VirusCloudResult getCloudResult()
    {
        return cloudResult;
    }

    /**
     * Sets the cloud result
     * 
     * @param argResult
     *        The result
     */
    protected synchronized void setCloudResult(VirusCloudResult argResult)
    {
        this.cloudResult = argResult;
    }

    /**
     * The main thread function
     */
    public void run()
    {
        StringBuilder builder = new StringBuilder(256);
        VirusCloudResult cloudResult = new VirusCloudResult();

        // ----- uncomment this string to force cloud detection for testing -----
        // String body = "[\n\"" + EICAR_TEST_MD5 + "\"\n]\n";
        String body = "[\n\"" + virusState.fileHash + "\"\n]\n";

        logger.debug("CloudScanner thread has started for: " + body);

        try {
            URL myurl = new URL(UvmContextFactory.context().uriManager().getUri(CLOUD_SCANNER_URL));
            HttpURLConnection mycon;
            if(myurl.getProtocol().equals("https")){
                mycon = (HttpsURLConnection) myurl.openConnection();
            }else{
                mycon = (HttpURLConnection) myurl.openConnection();
            }
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

            // build a string from the cloud response ignoring the brackets 
            for (int c = input.read(); c != -1; c = input.read()) {
                if ((char) c == '[') continue;
                if ((char) c == ']') continue;
                builder.append((char) c);
            }

            input.close();
            mycon.disconnect();

// THIS IS FOR ECLIPSE - @formatter:off

                /*
                 * This is an example of the message we get back from the cloud server.
                 *   
                 * [{"Category":"The EICAR Test String!16","Class":"m","Confidence":100,"Item":"44d88612fea8a8f36de82e1278abb02f"}]
                 * 
                 * Also worth noting... for a negative result the cloud server returns just the empty brackets:
                 * []
                 * 
                 */

// THIS IS FOR ECLIPSE - @formatter:on

            String cloudString = builder.toString();
            logger.debug("CloudScanner CODE:" + mycon.getResponseCode() + " MSG:" + mycon.getResponseMessage() + " DATA:" + cloudString);

            // if no json object in response create empty object to prevent exception 
            if ((cloudString.indexOf('{') < 0) || (cloudString.indexOf('}') < 0)) cloudString = "{}";

            JSONObject cloudObject = new JSONObject(cloudString);
            if (cloudObject.has("Confidence")) cloudResult.setItemConfidence(cloudObject.getInt("Confidence"));
            if (cloudObject.has("Category")) cloudResult.setItemCategory(cloudObject.getString("Category"));
            if (cloudObject.has("Class")) cloudResult.setItemClass(cloudObject.getString("Class"));
            if (cloudObject.has("Item")) cloudResult.setItemHash(cloudObject.getString("Item"));
        }

        catch (Exception exn) {
            logger.warn("CloudScanner thread exception: " + exn.toString());
        }

        setCloudResult(cloudResult);

        synchronized (this) {
            this.notify();
        }
    }
}
