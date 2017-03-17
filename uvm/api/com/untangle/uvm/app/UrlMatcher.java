/**
 * $HeadURL$
 */
package com.untangle.uvm.app;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

/**
 * This class manages the "matching" of URLs.
 *
 */
@SuppressWarnings("serial")
public class UrlMatcher implements java.io.Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String  LEFT_SIDE_ANCHOR = "^([a-zA-Z_0-9-]*\\.)*";
    private static final String  RIGHT_SIDE_ANCHOR = ".*$";
    
    /**
     * Original string value
     */
    private String value;

    /**
     * The original value converted to a regex
     */
    private String regexValue;

    /**
     * The pattern for regexValue
     */
    private Pattern regexPattern;

    
    public UrlMatcher( String value )
    {
        initialize( value );
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue( String value )
    {
        initialize(value);
    }

    public String getRegexValue()
    {
        return this.regexValue;
    }
    
    /**
     * Return true if this URL this matcher.
     */
    public boolean isMatch( String domain, String uri )
    {
        return isMatch( domain + uri );
    }

    /**
     * Return true if this URL this matcher.
     */
    public boolean isMatch( String url )
    {
        if ( this.regexPattern == null )
            return false;
        if ( url == null )
            return false;
        
        Matcher matcher = this.regexPattern.matcher( url );
        return matcher.matches();
    }

    /**
     * return string representation
     */
    public String toString()
    {
        return value;
    }

    /**
     * Initialize all the private variables
     */
    private void initialize( String value )
    {
        this.value = value;
        this.regexValue = globToRegex( value );

        try {
            this.regexPattern = Pattern.compile( this.regexValue );
        } catch ( Exception e ) {
            logger.warn( "Failed to compile UrlMatcher: \"" + value + "\" regex: \"" + regexValue + "\"", e );
            this.regexPattern = null;
        }
    }

    private static String globToRegex( String glob )
    {
        if (glob == null)
            return null;
        if ("".equals(glob))
            return "^$";

        String re = glob;

        /**
         * remove potential '\*\.?' or 'www.' at the beginning
         * for example "*.foo.com" becomes just "foo.com" because "foo.com" matches "*.foo.com" AND "foo.com"
         */
        re = re.replaceAll("^"+Pattern.quote("*."), "");
        re = re.replaceAll("^"+Pattern.quote("www."), "");

        /**
         * transform unescaped globbing operators into regex ones
         */
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("."), "\\.");
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("*"), ".*");
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("?"), ".");

        /**
         * transform escaped globbing operators into regex ones
         */
        re = re.replaceAll("\\\\" + Pattern.quote("."), ".");
        re = re.replaceAll("\\\\" + Pattern.quote("*"), "*");
        re = re.replaceAll("\\\\" + Pattern.quote("?"), "?");

        /**
         * Add the right side anchor (if not explicitly denied with $)
         * This is so google.com blocks google.com/whatever
         */
        if (re.charAt(re.length()-1) != '$')  {
            re = re + RIGHT_SIDE_ANCHOR;
        }

        /**
         * Add the left side anchor (if not explicitly denied with ^)
         * This is so google.com blocks (\\w*\\.)*google.com
         */
        if (re.charAt( 0 ) != '^')  {
            re = LEFT_SIDE_ANCHOR + re;
        }
        
        return re;

    }
    
}
