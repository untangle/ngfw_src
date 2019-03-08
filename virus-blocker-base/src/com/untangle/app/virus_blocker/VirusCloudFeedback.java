/**
 * $Id: VirusCloudFeedback.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.DataOutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppSession;

/**
 * Class to send virus telemetry to the cloud
 */
public class VirusCloudFeedback extends Thread
{
    private final Logger logger = Logger.getLogger(VirusBlockerBaseApp.class);

    private static final String CLOUD_FEEDBACK_URL = "https://telemetry.untangle.com/ngfw/v1/infection";
    private static final String CLOUD_SCANNER_KEY = "B132C885-962B-4D63-8B2F-441B7A43CD93";

    VirusBlockerState virusState = null;
    long fileLength = 0;
    AppSession session = null;
    String vendorName = null;
    String threatName = null;
    String threatType = null;
    VirusCloudResult cloudResult = null;

    /**
     * Constructor
     * 
     * @param virusState
     *        The virus state
     * @param vendorName
     *        Virus engine vendor name
     * @param threatName
     *        Threat name
     * @param threatType
     *        Threat type
     * @param fileLength
     *        File length
     * @param session
     *        The session
     * @param cloudResult
     *        The cloud result
     */
    public VirusCloudFeedback(VirusBlockerState virusState, String vendorName, String threatName, String threatType, long fileLength, AppSession session, VirusCloudResult cloudResult)
    {
        this.virusState = virusState;
        this.vendorName = vendorName;
        this.threatName = threatName;
        this.threatType = threatType;
        this.fileLength = fileLength;
        this.session = session;
        this.cloudResult = cloudResult;
    }

    /**
     * The main thread function
     */
    public void run()
    {
        StringBuilder feedback = new StringBuilder(256);
        JSONObject json = new JSONObject();

        try {
            json.put("hash", virusState.fileHash);
            json.put("length", fileLength);
            json.put("vendorName", vendorName);
            json.put("threatName", threatName);
            json.put("threatType", threatType);
            if (cloudResult != null) json.put("cloudResult", cloudResult);
            if (session != null) {
                if (session.globalAttachment(AppSession.KEY_HTTP_HOSTNAME) != null) json.put(AppSession.KEY_HTTP_HOSTNAME, session.globalAttachment(AppSession.KEY_HTTP_HOSTNAME));
                if (session.globalAttachment(AppSession.KEY_HTTP_URI) != null) json.put(AppSession.KEY_HTTP_URI, session.globalAttachment(AppSession.KEY_HTTP_URI));
                if (session.globalAttachment(AppSession.KEY_HTTP_URL) != null) json.put(AppSession.KEY_HTTP_URL, session.globalAttachment(AppSession.KEY_HTTP_URL));
                if (session.globalAttachment(AppSession.KEY_HTTP_REFERER) != null) json.put(AppSession.KEY_HTTP_REFERER, session.globalAttachment(AppSession.KEY_HTTP_REFERER));
                if (session.globalAttachment(AppSession.KEY_FTP_FILE_NAME) != null) json.put(AppSession.KEY_FTP_FILE_NAME, session.globalAttachment(AppSession.KEY_FTP_FILE_NAME));
                if (session.getOrigClientAddr() != null) json.put("clientAddr", session.getOrigClientAddr().getHostAddress());
                if (session.getNewServerAddr() != null) json.put("serverAddr", session.getNewServerAddr().getHostAddress());
                json.put("clientPort", session.getOrigClientPort());
                json.put("serverPort", session.getNewServerPort());
                if (session.getAttachments() != null) json.put("attachments", session.getAttachments());
            }

        } catch (Exception exn) {
            logger.warn("Exception building CloudFeedback JSON object.", exn);
        }

        feedback.append(json.toString());

        logger.debug("CloudFeedback thread has started for: " + feedback.toString());

        try {
            String target = (CLOUD_FEEDBACK_URL + "?hash=" + virusState.fileHash + "&det=" + threatName + "&type=" + threatType + "&detProvider=" + vendorName + "&metaProvider=NGFW");
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
            logger.warn("CloudFeedback thread exception: " + exn.toString());
        }
    }
}
