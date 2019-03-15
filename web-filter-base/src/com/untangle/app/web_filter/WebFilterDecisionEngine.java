/**
 * $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Iterator;


import org.apache.log4j.Logger;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.License;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Does blacklist lookups from zvelo API.
 */
public class WebFilterDecisionEngine extends DecisionEngine
{
    private static final int[] CRC_TABLE;
    private static final long DIA_TIMEOUT = 604800000; // 1 week
    private static final long DIA_TRY_LIMIT = 300000; // 5 min
    private static final String QUIC_COOKIE_FIELD_NAME = "alt-svc";
    // private static final String QUIC_COOKIE_FIELD_START = "svc=quic=";
    private static final String QUIC_COOKIE_FIELD_START = "quic=";

    private static final String YOUTUBE_HEADER_FIELD_FIND_NAME = "youtube";
    private static final String YOUTUBE_RESTRICT_COOKIE_NAME = "PREF=";
    private static final String YOUTUBE_RESTIRCT_COOKIE_VALUE = "&f2=8000000";
    private static final int YOUTUBE_RESTRICT_COOKIE_TIMEOUT = 60 * 1000;
    // private static final int YOUTUBE_RESTRICT_COOKIE_TIMEOUT = 24 * 60 * 60 * 1000;
    private static final DateFormat COOKIE_DATE_FORMATTER = new SimpleDateFormat("E, MM-dd-yyyy HH:mm:ss z");
    private static final Pattern SEPARATORS_REGEX = Pattern.compile("\\.");

    private static Pattern trailingDotsPattern = Pattern.compile("\\.*$");
    private static Pattern trailingDotsSlashesPattern = Pattern.compile("[.\\/]+$");

    private static String serialNum;

    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase ourApp = null;

    private static String diaKey = null;
    private int failures = 0;
    private long lastDiaUpdate = 0;
    private long lastDiaTry = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The base application
     */
    public WebFilterDecisionEngine(WebFilterBase app)
    {
        super(app);
        ourApp = app;

        synchronized (WebFilterDecisionEngine.class) {
            if (null == serialNum) {
                String uid = UvmContextFactory.context().getServerUID();
                if (null == uid) {
                    logger.warn("No UID");
                } else {
                    String[] s = uid.split("-");
                    if (s.length < 4) {
                        logger.warn("bad UID: " + uid);
                    } else {
                        // dont put full UID in query
                        //serialNum = "*REMOVED*";
                        serialNum = "*REMOVED*";
                    }
                }
            }
        }
    }

    /**
     * Check the request
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        The client address
     * @param port
     *        The port
     * @param requestLine
     *        The request line
     * @param header
     *        The header
     * @return The result
     */
    @Override
    public String checkRequest(AppTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        if (!isLicenseValid()) {
            return null;
        } else {
            String result = super.checkRequest(sess, clientIp, port, requestLine, header);
            if (result == null) {
                String term = SearchEngine.getQueryTerm(clientIp, requestLine, header);
                if (term != null) {
                    WebFilterQueryEvent hbe = new WebFilterQueryEvent(requestLine.getRequestLine(), header.getValue("host"), term, getApp().getName());
                    getApp().logEvent(hbe);
                }

                if(ourApp.getSettings().getRestrictYoutube()){
                    addYoutubeRestrictCookie(sess, header);
                }
           }
            return result;
        }
    }

    /**
     * Check the response
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        The client address
     * @param requestLine
     *        The request line
     * @param header
     *        The header
     * @return The result
     */
    @Override
    public String checkResponse(AppTCPSession sess, InetAddress clientIp, RequestLineToken requestLine, HeaderToken header)
    {
        if (!isLicenseValid()) {
            return null;
        } else {
            String result = super.checkResponse(sess, clientIp, requestLine, header);
            if(result == null){
                if(ourApp.getSettings().getBlockQuic()){
                    removeQuicCookie(header);
                }
            }
            return result;
       }
    }

    /**
     * Encode a DNS query
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The URI
     * @param command
     *        The command
     * @return The encoded query
     */
    public String encodeDnsQuery(String domain, String uri, String command)
    {
        String key = getDiaKey();

        String encodedUrl = encodeUrl(domain, uri);
        String data = domain + key + serialNum;
        String crc = crcccitt(data);

        // If the length of e_url + serial + authkey + host + all the
        // dots exceeds 255 then trim it until it will fit.  Trim by
        // removing path components, then domain components.
        int attempts = 30;
        while ((--attempts > 0) && (encodedUrl.length() + serialNum.length() + 4 + domain.length() + 5 >= 255)) {
            int i = encodedUrl.lastIndexOf("_-.");
            if (i >= 0) {
                encodedUrl = encodedUrl.substring(0, i);
            } else {
                int j = encodedUrl.indexOf('.');
                if (j >= 0) {
                    encodedUrl = encodedUrl.substring(j + 1);
                }
            }
        }

        // remove trailing dots
        encodedUrl.replaceFirst("\\.+$", "");

        String question = "*REMOVED*";
        return question;
    }

    /**
     * Default to the lookup command.
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The uri
     * @return The result
     */
    public String encodeDnsQuery(String domain, String uri)
    {
        return encodeDnsQuery(domain, uri, "0");
    }

    /**
     * Categorize a site
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The URI
     * @return The category list
     */
    @Override
    protected List<String> categorizeSite(String domain, String uri)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("start-checkDecisionEngineDatabase");
        }
        if (uri == "") {
            uri = "/";
        }

        // remove the :80 port info off the end of the domain if present
        int i = domain.indexOf(':');
        if (i > 0) {
            domain = domain.substring(0, i);
        }

        // lookup categorization in cache
        String catStr = HostCache.getCachedCategories(domain, uri);

        if (catStr == null) {
            String question = encodeDnsQuery(domain, uri);
            String[] answers = lookupDns(question);
            if (answers.length > 0) {
                for (int a = 0; a < answers.length; a++) {
                    if (answers[a] == null) continue;
                    String[] split = answers[a].split("\\s+");
                    String cs = null;
                    if (split.length >= 1) {
                        cs = split[0];
                    }

                    String cu = null;
                    if (split.length >= 2) {
                        cu = split[1];
                    }

                    if (null != cs && null != cu) {
                        HostCache.cacheCategories(cu, cs, domain, uri);
                    }
                }
            }

            catStr = HostCache.getCachedCategories(domain, uri);

            if (logger.isDebugEnabled()) {
                logger.debug("post-lookup");
            }

            if (catStr == null) {
                logger.info("zvelo null cat for : \"" + domain + uri + "\" using Miscellaneous");
                logger.info("zvelo question     : \"" + question + "\"");
                //logger.info("zvelo resultStr    : \"" + resultStr + "\"");

                // catStr = "54"; /* misc category name/string */
            }
        }

        if (catStr == null) {
            logger.info("null categorization for \"" + domain + uri + "\" using Miscellaneous");
            catStr = "54"; /* misc category name/string */
        }

        List<String> categories = new ArrayList<String>(catStr.length());
        for (String cat : catStr.split("_")) {
            categories.add(cat);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("end-checkDecisionEngineDatabase");
        }

        return categories;
    }

    /**
     * Clear the host cache
     * 
     * @param expireAll
     *        Flag to clear all or expired only
     */
    public void clearCache(boolean expireAll)
    {
        HostCache.cleanCache(expireAll);
    }

    /**
     * Lookup a site
     * 
     * @param url
     *        The URL
     * @return The category list
     */
    public List<String> lookupSite(String url)
    {
        String[] urlSplit = splitUrl(url);
        return categorizeSite(urlSplit[0], urlSplit[2]);
    }

    /**
     * Re-categorize a site
     * 
     * @param url
     *        The URL
     * @param category
     *        The category
     * @return The category
     */
    public int recategorizeSite(String url, int category)
    {
        String[] urlSplit = splitUrl(url);
        String question = encodeDnsQuery(urlSplit[0], urlSplit[2], Integer.toString(category) + "_a");
        String[] answers = lookupDns(question);
        int answerCategory = -1;
        if (answers.length > 0) {
            /**
             * The only way this can fail is if zvelo removes the submit
             * functionality.
             */
            String[] split = answers[0].split("\\s+");
            if (-1 != url.indexOf(split[1])) {
                answerCategory = category;
            }
        }
        return (category == answerCategory ? category : -1);
    }

    /**
     * Get dia key
     * 
     * @return The dia key
     */
    public String getDiaKey()
    {
        if (WebFilterDecisionEngine.diaKey != null) return WebFilterDecisionEngine.diaKey;

        /* otherwise synchronize and fetch a new one */

        if (logger.isDebugEnabled()) {
            logger.debug("start-getDiaKey");
        }
        synchronized (this) {
            long t = System.currentTimeMillis();

            if ((null == diaKey) || ((t - DIA_TIMEOUT) > lastDiaUpdate)) {
                if ((t - DIA_TRY_LIMIT) < lastDiaTry) {
                    logger.warn("DIA_TRY_LIMIT has not expired, not getting key");
                    return diaKey;
                }

                lastDiaTry = t;

                int timeout = 60*1000;
                RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).setConnectionRequestTimeout(timeout).build();
                CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
                CloseableHttpResponse response = null;
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials("untangle", "wu+glev6"));
                HttpClientContext context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);

                try {
                    String url = "*REMOVED*";
                    logger.debug("Fetch URL: \"" + url + "\"");
                    HttpGet get = new HttpGet(url);
                    response = httpClient.execute(get, context);
                    logger.debug("Fetched URL: \"" + url + "\"");

                    if (response != null && response.getStatusLine().getStatusCode() == 200) {
                        String s = EntityUtils.toString(response.getEntity(), "UTF-8");
                        s = s.trim();
                        logger.debug("DIA key response: " + s);

                        if (s.toUpperCase().startsWith("ERROR")) {
                            logger.warn("Could not get a dia key: " + s.substring(0, 10));
                        } else {
                            diaKey = s;
                            lastDiaUpdate = System.currentTimeMillis();
                        }
                    } else {
                        logger.warn("Failed to get dia key: " + response);
                    }
                } catch (Exception exn) {
                    logger.warn("Could not get dia key", exn);
                } finally {
                    try {
                        if (response != null) response.close();
                    } catch (Exception e) {
                        logger.warn("close", e);
                    }
                    try {
                        httpClient.close();
                    } catch (Exception e) {
                        logger.warn("close", e);
                    }
                }

            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("end-getDiaKey");
        }

        return diaKey;
    }

    /**
     * Encode a URL
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The URI
     * @return The encoded URL
     */
    private static String encodeUrl(String domain, String uri)
    {
        // Remote trailing dots from domain
        Matcher matcher = trailingDotsPattern.matcher(domain);
        domain = matcher.replaceAll("");

        String url = domain + uri;

        // Remote trailing dots or slashes from URL
        matcher = trailingDotsSlashesPattern.matcher(url);
        url = matcher.replaceAll("");

        StringBuilder qBuilder = new StringBuilder(url);

        int i;
        int lastDot = 0;

        for (i = 0; i < qBuilder.length(); i++) {

            int numCharsAfterThisOne = (qBuilder.length() - i) - 1;

            // Must insert a null escape to divide long spans
            if (i > lastDot + 59) {
                qBuilder.insert(i, "_-0.");
                lastDot = i + 3;
                i += 4;
            }

            if (qBuilder.charAt(i) == '.') {
                lastDot = i;
            }
            // Take care of the rare, but possible case of _- being in the string
            else if (qBuilder.charAt(i) == '_' && numCharsAfterThisOne >= 1 && qBuilder.charAt(i + 1) == '-') {
                qBuilder.replace(i, i + 2, "_-_-");
                i += 4;
            }
            // Convert / to rfc compliant characters _-.
            else if (qBuilder.charAt(i) == '/') {
                qBuilder.replace(i, i + 1, "_-.");
                lastDot = i + 2;
                i += 3;
            }
            // Convert any dots next to each other
            else if (qBuilder.charAt(i) == '.' && numCharsAfterThisOne >= 1 && qBuilder.charAt(i + 1) == '.') {
                qBuilder.replace(i, i + 2, "._-2e");
                i += 5;
            }
            // Convert any dots at the end. (these should have already been stripped but the zvelo implementation has this here)
            else if (qBuilder.charAt(i) == '.' && numCharsAfterThisOne == 0) {
                qBuilder.replace(i, i + 1, "_-2e");
                i += 4;
            }
            // Convert : to _--
            else if (qBuilder.charAt(i) == ':') {
                qBuilder.replace(i, i + 1, "_--");
                i += 3;
            }
            // Drop everything after ? or #
            else if (qBuilder.charAt(i) == '?' || qBuilder.charAt(i) == '#') {
                qBuilder.delete(i, qBuilder.length());
                break;
            }
            // Convert %HEXHEX to encoded form
            else if (qBuilder.charAt(i) == '%' && numCharsAfterThisOne >= 2 && _isHex(qBuilder.charAt(i + 1)) && _isHex(qBuilder.charAt(i + 2))) {
                //String hexString = new String(new char[] {qBuilder.charAt(i+1), qBuilder.charAt(i+2)});
                //char c = (char)( Integer.parseInt( hexString , 16) );
                //System.out.println("HEX: \"" + hexString +"\" -> \"" + Character.toString(c) + "\"");
                qBuilder.replace(i, i + 1, "_-");
                i += 4; // 2 for length of replacement + 2 for the hex characters
            }
            // Convert % charaters to encoded form
            else if (qBuilder.charAt(i) == '%') {
                qBuilder.replace(i, i + 1, "_-25");
                i += 4;
            }
            // Convert any remaining non-RFC characters.
            else if (!_isRfc(qBuilder.charAt(i))) {
                String replaceStr = String.format("_-%02X", ((byte) qBuilder.charAt(i)));
                qBuilder.replace(i, i + 1, replaceStr);
                i += 4;
            }
        }

        return qBuilder.toString();
    }

    /**
     * Calculate the CCITT-CRC checksum for a string
     * 
     * @param value
     *        The string
     * @return The checksum
     */
    private String crcccitt(String value)
    {
        int crc = 0xffff;

        for (byte b : value.getBytes()) {
            int i = ((b ^ (crc >> 8)) & 0xff);
            crc = ((crc << 8) ^ CRC_TABLE[i]) & 0xffff;
        }

        return Long.toHexString(crc);
    }

    /**
     * Get the DNS response for a question
     * 
     * @param question
     *        The question
     * @return The response
     */
    private String[] lookupDns(String question)
    {
        String[] answers = null;
        String resultStr = null;

        try {
            long t0 = System.currentTimeMillis();
            Lookup lookup = new Lookup(question, Type.TXT);
            Record[] records = lookup.run();
            long t1 = System.currentTimeMillis();
            long elapsedTime = t1 - t0;

            if (elapsedTime > 300) {
                logger.warn("DNS responded slowly. This may poorly effect web traffic latency. (" + elapsedTime + " ms).");
            }

            if (null == records) {
                logger.debug("No records for: " + question);
                records = new Record[0];
            }

            answers = new String[records.length];

            for (Record r : records) {
                if (r instanceof TXTRecord) {
                    TXTRecord txt = (TXTRecord) r;
                    for (Object o : txt.getStringsAsByteArrays()) {
                        resultStr = new String((byte[]) o);

                        if (resultStr.startsWith("FAILURE:") || resultStr.startsWith("REFUSED:")) {
                            synchronized (this) {
                                failures++;
                                if (failures > 1000) {
                                    logger.warn("1000 fails with key: " + diaKey);
                                    diaKey = null;
                                }
                            }
                            logger.warn("could not look up: " + question + "response: " + resultStr);
                        } else {
                            synchronized (this) {
                                failures = 0;
                            }
                            answers[answers.length - 1] = resultStr;
                        }
                    }
                }
            }
        } catch (TextParseException exn) {
            String message = exn.getMessage();

            /**
             * Don't litter the logs with common exceptions, Otherwise print the
             * full message
             */
            if (message.contains("invalid empty label")) {
                logger.info("Could not lookup (invalid empty label): \"" + question + "\"");
            } else if (message.contains("Name too long")) {
                logger.info("Could not lookup (Name too long): \"" + question + "\"");
            } else if (message.contains("label too long")) {
                logger.info("Could not lookup (label too long): \"" + question + "\"");
            } else {
                logger.warn("Could not lookup: \"" + question + "\": " + exn.getMessage());
            }
        }
        if (answers != null) {
            return answers;
        }
        return new String[0];
    }

    /**
     * Split a URL into parts
     * 
     * @param url
     *        The URL
     * @return The parts
     */
    private String[] splitUrl(String url)
    {
        String[] urlFields = new String[3];
        String domain = url;
        String uri = "/";
        String port = "80";
        int pos;

        /*
         * Strip protocol
         */
        pos = domain.indexOf("://");
        if (pos > 0) {
            domain = domain.substring(pos + 3);
        }

        /*
         * Split domain and URI
         */
        pos = domain.indexOf('/');
        if (pos > 0) {
            uri = domain.substring(pos);
            domain = domain.substring(0, pos);
        }

        /*
         * Split domain and port
         */
        pos = domain.indexOf(':');
        if (pos > 0) {
            port = domain.substring(pos + 1);
            domain = domain.substring(0, pos);
        }
        urlFields[0] = domain;
        urlFields[1] = port;
        urlFields[2] = uri;

        return urlFields;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    static {
        CRC_TABLE = new int[]
            { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
              0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
              0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
              0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
              0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
              0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
              0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
              0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
              0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
              0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
              0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
              0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
              0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
              0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
              0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
              0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
              0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
              0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
              0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
              0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
              0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
              0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
              0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
              0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
              0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
              0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
              0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
              0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
              0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
              0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
              0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
              0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0 };
    }

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Check the license status
     * 
     * @return True if valid, otherwise false
     */
    private boolean isLicenseValid()
    {
        if (ourApp.getAppName().equals("web-monitor")) return true;

        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_FILTER)) return true;
        return false;
    }

    /**
     * Determine if a character is a valid hex character
     * 
     * @param c
     *        The character
     * @return True if hex, otherwise false
     */
    private static boolean _isHex(char c)
    {
        if ('0' <= c && c <= '9') return true;
        else if ('a' <= c && c <= 'f') return true;
        else if ('A' <= c && c <= 'F') return true;
        return false;
    }

    /**
     * Checks to see if a character is DNS RFC compliant
     * 
     * @param c
     *        The character
     * @return True if compliant, otherwise false
     */
    private static boolean _isRfc(char c)
    {
        if ('a' <= c && c <= 'z') return true;
        else if ('A' <= c && c <= 'Z') return true;
        else if ('0' <= c && c <= '9') return true;
        else if (c == '.') return true;
        else if (c == '_') return true;
        else if (c == '-') return true;
        return false;
    }

    /**
     * Look for presence of quic cookie and if found remove.
     *
     * @param  header HeadetToken to parse.
     */
    private void removeQuicCookie(HeaderToken header){
        List<String> altSvcs = header.getValues(QUIC_COOKIE_FIELD_NAME);
        if(altSvcs != null){
            List<String> newAltSvcs = new ArrayList<>();
            for(String altSvc : altSvcs){
                if(!altSvc.toLowerCase().startsWith(QUIC_COOKIE_FIELD_START)){
                    logger.warn("ADD: " + altSvc);
                    newAltSvcs.add(altSvc);
                // }else{
                //     logger.warn("REMOVE: " + altSvc);
                }
            }
            header.removeField(QUIC_COOKIE_FIELD_NAME);
            if(newAltSvcs.size() > 0){
                header.setValues(QUIC_COOKIE_FIELD_NAME, newAltSvcs);
            }
        }
    }


    /**
     * If host is youtube, replace the inline PREF (if found) or append the PREF cookie that
     * enables restricted mode.
     * @param sess
     *        AppTCPSession for this session.
     * @param header HeaterToken to parse.
     */
    private void addYoutubeRestrictCookie(AppTCPSession sess, HeaderToken header){
        String host = header.getValue("host");

        if(host != null && SEPARATORS_REGEX.matcher(host.toLowerCase()).replaceAll("").contains(YOUTUBE_HEADER_FIELD_FIND_NAME) ) {

            List<String> cookies = header.getValues("cookie");
            long cookieExpiration = System.currentTimeMillis() + YOUTUBE_RESTRICT_COOKIE_TIMEOUT;

            if (cookies == null) {
                cookies = new ArrayList<>();
            }
            boolean found = false;
            for(int i = 0; i < cookies.size(); i++){
                String setCookie = cookies.get(i);
                if(setCookie.toUpperCase().startsWith(YOUTUBE_RESTRICT_COOKIE_NAME)){
                    found = true;
                    int firstSemiColon=setCookie.indexOf(';');
                    cookies.set(i, setCookie.substring(0, firstSemiColon) + YOUTUBE_RESTIRCT_COOKIE_VALUE + setCookie.substring(firstSemiColon));
                }
            }
            if(!found){
                cookies.add(YOUTUBE_RESTRICT_COOKIE_NAME + YOUTUBE_RESTIRCT_COOKIE_VALUE + "; expires="+COOKIE_DATE_FORMATTER.format(cookieExpiration)+"; path=/; domain=.youtube.com");
            }
        }
    }

}
