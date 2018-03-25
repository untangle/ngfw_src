/**
 * $Id: UrlMatcher.java,v 1.00 2017/12/22 18:04:34 dmorris Exp $
 */
package com.untangle.app.ad_blocker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.GenericRule;

/**
 * Optimized matcher for matching an URL against a list of rules
 */
public class UrlMatcher
{
    private final Logger logger = Logger.getLogger(getClass());

    private Map<String, List<GenericRule>> ruleByKeyword = new ConcurrentHashMap<>();
    private Map<String, String> keywordByRule = new ConcurrentHashMap<>();
    private static Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    /**
     * The keywords are chosen such that
     * they start with a non alphanumerical character, excepting % and *
     * they consist of at least 3 alphanumerical characters
     * they are followed by a non alphanumerical character, excepting % and *
     */
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[^a-z0-9%*][a-z0-9%]{3,}(?=[^a-z0-9%*])", Pattern.CASE_INSENSITIVE);
    private static final Pattern URI_CANDIDATE_PATTERN = Pattern.compile("[a-z0-9%]{3,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATCH_NOTHING = Pattern.compile("a^", Pattern.CASE_INSENSITIVE);

    /**
     * Create a UrlMatcher
     * Must be loaded with rules after creation
     */
    public UrlMatcher() {}

    /**
     * Removes all known rules
     */
    public void clear()
    {
        this.ruleByKeyword.clear();
        this.keywordByRule.clear();
    }

    /**
     * Adds a rule to the matcher
     * 
     * @param rule
     */
    public void addRule(GenericRule rule)
    {
        if (keywordByRule.containsKey(rule.getString())) {
            String keyword = keywordByRule.get(rule.getString());
            if (keyword.length() == 0)
                rule.setFlagged(true);
            return;
        }

        // Look for a suitable keyword
        String keyword = findKeyword(rule.getString());
        List<GenericRule> oldEntry = ruleByKeyword.get(keyword);
        if (oldEntry == null) {
            oldEntry = new ArrayList<>();
            ruleByKeyword.put(keyword, oldEntry);
        }
        oldEntry.add(rule);
        keywordByRule.put(rule.getString(), keyword);
        if (keyword.length() == 0)
            rule.setFlagged(true);
    }

    /**
     * Removes a rule from the matcher
     * 
     * @param rule
     */
    public void removeRule(GenericRule rule)
    {
        if (!keywordByRule.containsKey(rule.getString()))
            return;

        String keyword = keywordByRule.get(rule.getString());
        List<GenericRule> list = ruleByKeyword.get(keyword);

        if (list.size() <= 1) {
            ruleByKeyword.remove(keyword);
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getString().equals(rule.getString())) {
                    list.remove(i);
                    break;
                }
            }
        }

        keywordByRule.remove(rule.getString());
    }

    /**
     * Chooses a keyword to be associated with the rule
     * 
     * @param rule
     * @return keyword (might be empty string)
     */
    private String findKeyword(String rule)
    {
        String result = "";

        Matcher matcher = KEYWORD_PATTERN.matcher(rule);
        List<String> candidates = new LinkedList<>();
        while (matcher.find()) {
            candidates.add(matcher.group());
        }
        if (candidates.size() == 0)
            return result;

        int resultCount = Integer.MAX_VALUE;
        int resultLength = 0;
        for (String c : candidates) {
            String candidate = c.substring(1);
            int count = (ruleByKeyword.containsKey(candidate) ? ruleByKeyword.get(candidate).size() : 0);
            if (count < resultCount || (count == resultCount && candidate.length() > resultLength)) {
                result = candidate;
                resultCount = count;
                resultLength = candidate.length();
            }
        }
        return result;
    }

    /**
     * Checks whether the entries for a particular keyword match a URL
     * @param keyword The keyword
     * @param val The value
     * @return the matching rule if found, otherwise null
     */
    private GenericRule checkEntryMatch(String keyword, String val)
    {
        if (val == null)
            return null;
        List<GenericRule> list = ruleByKeyword.get(keyword);
        for (GenericRule rule : list) {
            Pattern pattern = getPattern(rule.getString());
            if (pattern != null && pattern.matcher(val).find()) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Format used by Adblock Plus lists
     * 
     * || stands for either one of "http://", "https://" or "www." | stands for
     * beginning or ending of url ^ stands for separator: anything but a letter,
     * a digit, or one of the following: _ - . %
     *
     * @param rule The rule
     * @return The regex pattern
     */
    private Pattern getPattern(String rule)
    {
        Pattern pattern = patterns.get(rule);
        if (pattern == null) {
            // transform rule in regex
            String regex = rule;
            // Matching at beginning/end of an address

            String prefix = "";
            if (regex.startsWith("||")) {
                prefix = "^(((http://)?(www\\.)?)|((https://)?(www\\.)?)?)";
                regex = regex.replaceFirst("\\|\\|", "");
            } else {
                if (regex.startsWith("|")) {
                    prefix = "\\^";
                    regex = regex.replaceFirst("\\|", "");
                }
            }
            if (regex.endsWith("|")) {
                regex = regex.substring(0, regex.length() - 1) + "$";
            }

            // Matching address with wildcards
            regex = regex.replaceAll("\\.", "\\\\.");
            regex = regex.replaceAll("\\|", "\\\\|");
            regex = regex.replaceAll("\\?", "\\\\?");
            regex = regex.replaceAll("\\*", "\\.\\*");
            regex = regex.replaceAll("\\]", "\\\\]");
            regex = regex.replaceAll("\\[", "\\\\]");
            regex = regex.replaceAll("\\^", "[^a-zA-Z0-9_\\\\-\\\\.%]");
            regex = prefix + regex;
            
            try {
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                logger.error("can not compile rule " + rule + " into a regex: " + e);
                pattern = MATCH_NOTHING;
            }
            patterns.put(rule, pattern);

        }
        return pattern;
    }

    /**
     * Tests whether the URL matches any of the known rules
     * 
     * @param uri
     * @return matching rule or null
     */
    public GenericRule findMatch(String uri)
    {
        Matcher matcher = URI_CANDIDATE_PATTERN.matcher(uri);
        LinkedList<String> candidates = new LinkedList<>();
        while (matcher.find()) {
            candidates.add(matcher.group());
        }
        candidates.push("");
        for (String substr : candidates) {
            if (ruleByKeyword.containsKey(substr)) {
                GenericRule result = checkEntryMatch(substr, uri);
                if (result != null)
                    return result;
            }
        }

        return null;
    }

    /**
     * Check if an adequate keyword can be computed for the given rule
     * 
     * @param ruleStr
     * @return
     */
    public static boolean isSlowRule(String ruleStr)
    {
        Matcher matcher = KEYWORD_PATTERN.matcher(ruleStr);

        return matcher.find();
    }

}
