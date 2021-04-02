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

import org.apache.http.client.utils.URIBuilder;
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

    private static final List<String> SearchEngineHosts;
    private static final List<Pattern> SearchEngines;
    private static final Pattern YouTubeQuery = Pattern.compile(".*=oq=([^&]+).*");
    public static final URIBuilder KidFriendlySearchEngineRedirectUri;
    public static final HashMap<String,Object> KidFriendlyRedirectParameters;
    static {
        // Substring of hosts we care about; be sure to align with SearchEngineS!
        SearchEngineHosts = new ArrayList<String>();
        SearchEngineHosts.add("google");
        SearchEngineHosts.add("youtube");
        SearchEngineHosts.add("ask");
        SearchEngineHosts.add("bing");
        SearchEngineHosts.add("yahoo");
        SearchEngineHosts.add("duckduckgo");
        SearchEngineHosts.add("kidzsearch");

        SearchEngines = new ArrayList<Pattern>();
        SearchEngines.add(Pattern.compile(".*youtube\\.[a-z]+(\\.[a-z]+)?/results\\?search_query=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*(youtube|google)\\.[a-z]+(\\.[a-z]+)?/(complete/|)search.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*(youtube|google)\\.[a-z]+(\\.[a-z]+)?/gen_204(\\?|&)oq=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*ask\\.[a-z]+(\\.[a-z]+)?/web.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*bing\\.[a-z]+(\\.[a-z]+)?/search.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*yahoo\\.[a-z]+(\\.[a-z]+)?/search.*(\\?|&)p=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*duckduckgo\\.[a-z]+(\\.[a-z]+)?/.*(\\?|&)q=([^&]+).*"));
        SearchEngines.add(Pattern.compile(".*kidzsearch\\.[a-z]+(\\.[a-z]+)?/.*(\\?|&)q=([^&]+).*"));

        URIBuilder k = null;
        try{
            k = new URIBuilder();
            k = new URIBuilder("https://search.kidzsearch.com/kzsearchlmt.php");
        }catch(Exception e){
            logger.warn("Unable to create kid friendly search uri:", e);
        }
        KidFriendlySearchEngineRedirectUri = k;

        KidFriendlyRedirectParameters = new HashMap<>();
        KidFriendlyRedirectParameters.put("q", null);
    };

    /**
     * Get the query term
     *
     * @param clientIp
     *        The client address
     * @param host
     *        URL host.
     * @param uri
     *        URL URI.
     * @param header
     *        The header token
     *
     * @return The query term
     */
    public static String getQueryTerm(InetAddress clientIp, String host, String uri, HeaderToken header)
    {
        boolean hostFound = false;
        for(String hostPiece : SearchEngineHosts){
            if(host.contains(hostPiece)){
                hostFound = true;
            }
        }
        if(hostFound == false){
            return null;
        }

        String url = host + uri.toString();

        String term = null;
        for (Pattern p : SearchEngines) {
            Matcher m = p.matcher(url);
            if (m.matches()) {
                try {
                    term = m.group(m.groupCount());
                    term = URLDecoder.decode(term, "UTF-8");
                } catch (Exception e) {

                }
                return term;
            }
        }

        if(host.contains(WebFilterDecisionEngine.YOUTUBE_HEADER_FIELD_FIND_NAME) ) {
            List<String> cookies = header.getValues("cookie");
            if (cookies == null) {
                return term;
            }
            String cookie = null;
            for(int i = 0; i < cookies.size(); i++){
                cookie = cookies.get(i);
                Matcher m = YouTubeQuery.matcher(cookie);
                if (m.matches()) {
                    if(term == null){
                        try {
                            term = m.group(m.groupCount());
                            term = URLDecoder.decode(term, "UTF-8");
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        return term;
    }

    /**
     * Build a new hash of parameters for kid friendly redirect using the specified search term.
     * @param  term Search term to use as kid friendly key search value.
     * @return      New hash with populated term.
     */
    public static Map<String,Object> getKidFriendlyRedirectParameters(String term)
    {
        Map<String,Object> parameters = new HashMap<String,Object>(SearchEngine.KidFriendlyRedirectParameters);
        parameters.put("q", term);
        return parameters;
    }
}
