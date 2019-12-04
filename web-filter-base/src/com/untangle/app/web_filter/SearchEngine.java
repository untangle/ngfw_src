/**
 * $Id: UrlRewriter.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.util.UrlMatchingUtil;

/**
 * Map search engine to their query URIs
 * 
 */
public class SearchEngine
{
    private static final Logger logger = Logger.getLogger(SearchEngine.class);

    private static final List<Pattern> SearchEngines;
    public static final String KidFriendlySearchEngineIgnoreHost = "search.kidzsearch.com";
    public static final String KidFriendlySearchEngineRedirectUrl = "https://search.kidzsearch.com/kzsearchlmt.php";
    public static final HashMap<String,Object> KidFriendlyRedirectParameters;
    static {
        SearchEngines = new ArrayList<Pattern>();
        SearchEngines.add(Pattern.compile(".*youtube\\.[a-z]+(\\.[a-z]+)?/results\\?search_query=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*google\\.[a-z]+(\\.[a-z]+)?/(complete/|)search.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*ask\\.[a-z]+(\\.[a-z]+)?/web.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*bing\\.[a-z]+(\\.[a-z]+)?/search.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*yahoo\\.[a-z]+(\\.[a-z]+)?/search.*(\\?|&)p=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*duckduckgo\\.[a-z]+(\\.[a-z]+)?/.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*kidzsearch\\.[a-z]+(\\.[a-z]+)?/.*(\\?|&)q=([^&]+).*"));

        KidFriendlyRedirectParameters = new HashMap<>();
        KidFriendlyRedirectParameters.put("q", null);
    };

    /**
     * Get the query term
     *
     * @param clientIp
     *        The client address
     * @param requestLine
     *        The request line token
     * @param header
     *        The header token
     *
     * @return The query term
     */
    public static String getQueryTerm(InetAddress clientIp, RequestLineToken requestLine, HeaderToken header)
    {
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("(?<!:)/+", "/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
        }

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }
        host = UrlMatchingUtil.normalizeHostname(host);

        String url = host + uri.toString();

        for (Pattern p : SearchEngines) {
            Matcher m = p.matcher(url);
            if (m.matches()) {
                String term = "";
                try {
                    term = m.group(m.groupCount());
                    term = URLDecoder.decode(term, "UTF-8");
                } catch (Exception e) {

                }
                return term;
            }
        }
        return null;
    }
}
