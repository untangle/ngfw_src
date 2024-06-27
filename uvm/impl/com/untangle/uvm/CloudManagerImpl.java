/**
 * $Id$
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.log4j.Logger;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONObject;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;

/**
 * The cloud manager
 */
public class CloudManagerImpl implements CloudManager
{
    private static final String UTF_8 = "UTF-8";
    private final Logger logger = Logger.getLogger(CloudManagerImpl.class);

    private static final String ZEROTOUCH_API = "/appliance/IsProvisioned?serialNumber=%serial%&uid=%uid%";
    private static final int ZEROTOUCH_SLEEP_TIME_MILLI = 30 * 1000;
    private static final int ZEROTOUCH_TIMEOUT_TIME_MILLI = 30 * 1000;

    private volatile Thread zerotouchThread;
    private zerotouchMonitor zerotouch = new zerotouchMonitor();

    /**
     * The singleton instance
     */
    private static final CloudManagerImpl INSTANCE = new CloudManagerImpl();

    /**
     * Constructor
     */
    private CloudManagerImpl()
    {
    }

    /**
     * Gets our singleton instance
     * 
     * @return Our singleton instance
     */
    public synchronized static CloudManagerImpl getInstance()
    {
        return INSTANCE;
    }

    /**
     * Called to login to a cloud account
     * 
     * @param email
     *        The email address
     * @param password
     *        The password
     * @return The login result
     * @throws Exception
     */
    public JSONObject accountLogin(String email, String password) throws Exception
    {
        return accountLogin(email, password, UvmContextImpl.getInstance().getServerUID(), "", "", "");
    }

    /**
     * Called to login to a cloud account
     * 
     * @param email
     *        The email address
     * @param password
     *        The password
     * @param uid
     *        The system UID
     * @param applianceModel
     *        The appliance model
     * @param majorVersion
     *        The major version
     * @param installType
     *        The installation type
     * @return The login result
     * @throws Exception
     */
    public JSONObject accountLogin(String email, String password, String uid, String applianceModel, String majorVersion, String installType) throws Exception
    {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpClientContext context = HttpClientContext.create();

            URIBuilder builder = new URIBuilder(UvmContextImpl.getInstance().getStoreUrl() + "/account/login");
            builder.addParameter("email", email);
            builder.addParameter("uid", uid);
            builder.addParameter("applianceModel", applianceModel);
            builder.addParameter("majorVersion", majorVersion);
            builder.addParameter("installType", installType);
            String url = builder.build().toString();

            LinkedList<NameValuePair> bodyParams = new LinkedList<>();
            bodyParams.add(new BasicNameValuePair("password", password));
            UrlEncodedFormEntity body = new UrlEncodedFormEntity(bodyParams);

            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(body);

            String responseBody = httpClient.execute(post, context, response -> EntityUtils.toString(response.getEntity(), UTF_8));

            responseBody = responseBody.trim();

            logger.info("accountLogin( " + email + " ) = " + responseBody);

            JSONObject json = new JSONObject(responseBody);
            return json;
        } catch (Exception e) {
            logger.warn("accountLogin Exception: ", e);
            throw e;
        }
    }

    /**
     * Called to create a cloud account
     * 
     * @param email
     *        The email address
     * @param password
     *        The password
     * @param firstName
     *        First name
     * @param lastName
     *        Last name
     * @param companyName
     *        Company name
     * @param uid
     *        The system UID
     * @param applianceModel
     *        The appliance model
     * @param majorVersion
     *        The major version
     * @param installType
     *        The installation type
     * @return The create account result
     * @throws Exception
     */
    public JSONObject accountCreate(String email, String password, String firstName, String lastName, String companyName, String uid, String applianceModel, String majorVersion, String installType) throws Exception
    {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpClientContext context = HttpClientContext.create();

            URIBuilder builder = new URIBuilder(UvmContextImpl.getInstance().getStoreUrl() + "/account/create");
            builder.addParameter("email", email);
            builder.addParameter("fname", firstName);
            builder.addParameter("lname", lastName);
            builder.addParameter("cname", companyName);
            builder.addParameter("uid", uid);
            builder.addParameter("applianceModel", applianceModel);
            builder.addParameter("majorVersion", majorVersion);
            builder.addParameter("installType", installType);

            String url = builder.build().toString();

            LinkedList<NameValuePair> bodyParams = new LinkedList<>();
            bodyParams.add(new BasicNameValuePair("password", password));
            UrlEncodedFormEntity body = new UrlEncodedFormEntity(bodyParams);

            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(body);

            String responseBody = httpClient.execute(post, context, response -> EntityUtils.toString(response.getEntity(), UTF_8));
            responseBody = responseBody.trim();

            logger.info("accountCreate( " + email + " ) = " + responseBody);

            JSONObject json = new JSONObject(responseBody);
            return json;
        } catch (Exception e) {
            logger.warn("accountCreate Exception: ", e);
            throw e;
        }
    }

    /**
     * Launch the zero touch monitor.
     */
    public void startZeroTouchMonitor(){
        UvmContextFactory.context().newThread(this.zerotouch).start();        
    }

    /**
     * On a newly installed system, launch zero Touch provisioning.
     */
    private class zerotouchMonitor implements Runnable
    {
        /**
         * Launch zerotouch API query if system qualifies.
         */
        public void run()
        {
            /**
             * Determine if we need to run.  Don't run if:
             * -    Setup Wizard has already complated.
             * -    Setup Wizard has been started already.
             * -    Serial number or uid is null.
             */
            WizardSettings wizardSettings = UvmContextFactory.context().getWizardSettings();
            String serialNumber = UvmContextFactory.context().getServerSerialNumber();
            String uid = UvmContextFactory.context().getServerUID();

            if( wizardSettings.getWizardComplete() == true ||
                wizardSettings.getCompletedStep() != null ||
                serialNumber == null ||
                uid == null){
                return;
            }

            /**
             * Build API call, replacing serial and uid parameters.
             */
            String zerotouchApiCall = UvmContextImpl.getInstance().getStoreUrl() + ZEROTOUCH_API;
            try{
                zerotouchApiCall = zerotouchApiCall
                .replace("%serial%",URLEncoder.encode(serialNumber, UTF_8))
                .replace("%uid%",URLEncoder.encode(uid, UTF_8));
            } catch(Exception exn){
                logger.error("zerotouchMonitor: Unable to encode: " + exn);
                return;
            }
            zerotouchThread = Thread.currentThread();

            logger.info("zerotouchMonitor: starting");

            /**
             * Create client with short timeout.
             */
            RequestConfig.Builder config = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(ZEROTOUCH_TIMEOUT_TIME_MILLI));

            PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
            poolingConnManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(ZEROTOUCH_TIMEOUT_TIME_MILLI))
                    .setSocketTimeout(Timeout.ofMilliseconds(ZEROTOUCH_TIMEOUT_TIME_MILLI))
                    .build());
            CloseableHttpClient httpClient = HttpClientBuilder
                    .create()
                    .setConnectionManager(poolingConnManager)
                    .setDefaultRequestConfig(config.build())
                    .build();
            HttpGet get;
            URL url;

            while (true) {
                wizardSettings = UvmContextFactory.context().getWizardSettings();
                if( wizardSettings.getWizardComplete() == true ||
                    wizardSettings.getCompletedStep() != null ){
                    /*
                     * While waiting, someone has initiated the setup wizard, so let them go for it.
                     */
                    break;
                }
                /**
                 * Verify we have a WAN address.
                 */
                InetAddress firstWan = UvmContextFactory.context().networkManager().getFirstWanAddress();
                if(firstWan != null){
                    boolean receivedResponse = false;

                    /**
                     * Peform query.
                     */
                    String responseBody = null;
                    try {
                        logger.info("zerotouchMonitor: Requesting: " + zerotouchApiCall);
                        url = new URL(zerotouchApiCall);
                        get = new HttpGet(url.toString());
                        responseBody = httpClient.execute(get, response -> EntityUtils.toString(response.getEntity()));
                        if ( responseBody != null ) {
                            /**
                             * It doesn't matter what the response is.
                             * If false, nothing will be done.
                             * If true, command center will contact unit to launch ut-restore.sh.
                             */
                            receivedResponse = true;
                        }
                    } catch ( java.net.UnknownHostException e ) {
                        logger.warn("zerotouchMonitor: Exception requesting (unknown host):" + e.toString());
                    } catch ( java.net.ConnectException e ) {
                        logger.warn("zerotouchMonitor: Exception requesting (connect exception):" + e.toString());
                    } catch ( Exception e ) {
                        logger.warn("zerotouchMonitor: Exception requesting(other exception):" + e.toString());
                    } finally {
                        try {
                            httpClient.close();
                        } catch (Exception e) {
                            logger.warn("zerotouchMonitor: close",e);
                        }
                    }

                    if(receivedResponse){
                        logger.info("zerotouchMonitor: stopping after receiving response: " + responseBody);
                        break;
                    }
                }

                try {
                    Thread.sleep(ZEROTOUCH_SLEEP_TIME_MILLI);
                } catch (Exception e) {}

                wizardSettings = UvmContextFactory.context().getWizardSettings();
                if( wizardSettings.getWizardComplete() == true ||
                    wizardSettings.getCompletedStep() != null ){
                        logger.info("zerotouchMonitor: stopping after wizard started or completed");
                    break;
                }
            }
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.warn("zerotouchMonitor: close",e);
            }
        }
    }
}
