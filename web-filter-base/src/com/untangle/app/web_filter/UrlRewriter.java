/**
 * $Id: UrlRewriter.java 41284 2015-09-18 07:03:39Z dmorris $
 */

package com.untangle.app.web_filter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Holds mappings between popular search engines and their safe search HTTP
 * requests.
 * 
 * Created: Tue Sep 22 10:39:28 2009
 * 
 */
public class UrlRewriter
{
    private static final Logger logger = Logger.getLogger(UrlRewriter.class);

    private static final Map<Pattern, String> safeSearchRewrites;
    static {
        safeSearchRewrites = new HashMap<Pattern, String>();
        //safeSearchRewrites.put(Pattern.compile(".*google\\.[a-z]+(\\.[a-z]+)?/.+q=.*"), "safe=active"); // active (moderate)
        safeSearchRewrites.put(Pattern.compile(".*google\\.[a-z]+(\\.[a-z]+)?/.+q=.*"), "safe=strict"); // strict
        safeSearchRewrites.put(Pattern.compile(".*ask\\.[a-z]+(\\.[a-z]+)?/.+q=.*"), "adt=0");
        safeSearchRewrites.put(Pattern.compile(".*bing\\.[a-z]+(\\.[a-z]+)?/.+q=.*"), "adlt=strict");
        safeSearchRewrites.put(Pattern.compile(".*yahoo\\.[a-z]+(\\.[a-z]+)?/.+p=.*"), "vm=r");
        safeSearchRewrites.put(Pattern.compile(".*duckduckgo\\.[a-z]+(\\.[a-z]+)?/.+q=.*"), "kp=1"); // strict // https://duck.co/help/features/safe-search
    };

    private static final Map<Pattern, String> youtubeForSchoolsRewrites;
    static {
        youtubeForSchoolsRewrites = new HashMap<Pattern, String>();
        youtubeForSchoolsRewrites.put(Pattern.compile(".*youtube\\.[a-z]+(\\.[a-z]+)?/.+"), "edufilter=");
    };

    private static List<Pattern> excludes;
    static {
        excludes = new ArrayList<Pattern>();
        excludes.add(Pattern.compile(".*bing\\.[a-z]+(\\.[a-z]+)?/maps/.*"));
    };

    //http://support.google.com/youtube/bin/answer.py?hl=en&answer=1686318
    private static Pattern youtubeIgnorePattern = Pattern.compile("\\.(png|gif|js|xml|css)$");

    /**
     * Get the safe search URI
     * 
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @return The safe search URI
     */
    public static URI getSafeSearchUri(String host, URI uri)
    {
        String uriParam = getParam(safeSearchRewrites, host, uri);
        if (uriParam != null) {
            URI safeUri = URI.create(uri.toString() + "&" + uriParam);
            logger.debug("getUrlRewriterUri: '" + safeUri + "'");
            return safeUri;
        } else return null;
    }

    /**
     * Get the YouTube for Schools URI
     * 
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @param youtubeIdentifier
     *        The YouTube identifier
     * @return The YouTube for Schools URI
     */
    public static URI getYoutubeForSchoolsUri(String host, URI uri, String youtubeIdentifier)
    {
        String uriParam = getParam(youtubeForSchoolsRewrites, host, uri);
        if (uriParam != null) {
            if (youtubeIgnorePattern.matcher(uri.toString()).matches()) {
                return null;
            }

            String newUri;

            /**
             * If it already contains arguments, append to them. Otherwise add
             * them
             */
            if (uri.toString().contains("?")) newUri = uri.toString() + "&" + uriParam + youtubeIdentifier;
            else newUri = uri.toString() + "?" + uriParam + youtubeIdentifier;

            logger.debug("Original  URI: \"" + uri + "\"");
            logger.debug("Using new URI: \"" + newUri + "\"");

            URI youtubeUri = URI.create(newUri);
            logger.debug("getYoutubeForSchoolsUri: '" + youtubeUri + "'");
            return youtubeUri;
        } else return null;
    }

    /**
     * Get parameter
     * 
     * @param patterns
     *        The patterns
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @return The paramter
     */
    private static String getParam(Map<Pattern, String> patterns, String host, URI uri)
    {
        String url = host + uri.toString();
        logger.debug("getUrlRewriterParam: trying to match string '" + url + "'");
        for (Pattern p : patterns.keySet()) {
            logger.debug("getUrlRewriterParam: ... with pattern '" + p.pattern() + "'");
            if (p.matcher(url).matches()) {
                logger.debug("getUrlRewriterParam: ...... match !");
                for (Pattern q : excludes) {
                    if (q.matcher(url).matches()) {
                        logger.debug("getUrlRewriterParam: ...... but it also matches an exclude !");
                        return null;
                    }
                }
                return patterns.get(p);
            }
        }
        return null;
    }
}
