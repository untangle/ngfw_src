/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import org.apache.log4j.Logger;

/**
 * A utility class to Pattern objects from regex strings
 */
public class PatternFactory
{
    private static Map<String,Pattern> _cachedPatterns = new HashMap<>();
    private static final Logger logger = Logger.getLogger(PatternFactory.class);

    /**
     * There is a bug in the java regex compiler that makes it so the pattern
     * '[0xFF]' just doesn't work.  This is strange because '0xFF' does work.
     * To fix this, all sequences of the form [abd\xFF] are converted to ([abd]|0xFF)
     * which is identical, but more verbose.  Since [abd-\xFF] works, and is not logically
     * equivalent to '([abd-]|0xFF)', it -\xFF is treated as a special case.  This function
     * assumes that its input is 8 bit characters, so it uses 16 bit characters as
     * placeholders.  \uFFEE = '\xFF, \uFEFE = '-xFF'.  This array should be applied
     * to the input in order.
     *
     * XXX This will NOT work for anything with  [\xFF] inside of []
     */
    private static final StringReplacer[] FIX_XFF  = new StringReplacer[] {
        new StringReplacer( "\\\\xFF", "\uFFEE" ),
        new StringReplacer( "\\\\xff", "\uFFEE" ),
        new StringReplacer( "-\uFFEE", "\uFEFE" ),
        /* Move out any patterns with embedded xFF to \uFFEF */
        new StringReplacer( "\\[([^\uFFEE\\[]*)\uFFEE([^\uFFEE\\[]*)\\]", "([$1$2]|\uFFEF)" ),
        new StringReplacer( "\uFEFE", "-\\\\xFF" ),
        new StringReplacer( "\uFFEF", "\\\\xFF" ),
        /* Remove all unchanged \xFF's, this fixes patterns whee \xFF is specified twice
         * without a - in front */
        new StringReplacer( "\uFFEE", "" )
    };

    /**
     * Create a pattern from the regex
     * @param inputRegEx - the source regex string
     * @return the pattern
     */
    public static Pattern createRegExPattern (String inputRegEx)
    {

        Pattern result = null;

        synchronized(_cachedPatterns) {
            result = _cachedPatterns.get(inputRegEx);
            if (_cachedPatterns.containsKey(inputRegEx))
                return result;
        }

        String regEx = _perlRegexTranslate(inputRegEx);
        logger.info( "Factory modified regex to " + regEx );
        try {
            result = Pattern.compile(regEx, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exn) {
            logger.warn("bad pattern: " + exn.getMessage());
        }

        synchronized(_cachedPatterns) {
            _cachedPatterns.put(inputRegEx, result);
        }

        return result;
    }

    /**
     * Translate a regex from perl to java
     * @param regex - the source perl regex string
     * @return the java regex string equivalent
     */
    private static String _perlRegexTranslate (String regex)
    {
        /* Rules
         * -----
         */
        /* 1) When you use an escape sequence in a String literal,
           you have to double escape it, that's why \\. is written.
           But if you happened to read from a file, you must use normal
           escaping (\.), this double escaping only stands for literals.
           2) yString.matches("regex") returns true or false depending whether
           the string can be matched entirely by the regular expression.
           It is important to remember that String.matches() only returns true
           if the entire string can be matched.
           In other words: "regex" is applied as if you had written "^regex$" with
           start and end of string anchors. This is different from most other
           regex libraries, where the "quick match test" method returns true if
           the regex can be matched anywhere in the string. If myString is abc
           then myString.matches("bc") returns false. bc matches abc,
           but ^bc$ (which is really being used here) does not.
        */
        /* logger.info("Input String is "+ regex); */
        return fixFFPattern( regex );
    }

    /**
     * Fix the 0xFF java issue
     * @param regex The original regex string
     * @return the fixed regex string
     */
    private static String fixFFPattern( String regex )
    {
        for ( int c = 0; c < FIX_XFF.length ; c++ ) {
            regex = FIX_XFF[c].replaceAll( regex );
        }

        return regex;
    }

}

/**
 * A class to store the pattern and its replacement for regex manipulations
 */
class StringReplacer
{
    final Pattern pattern;
    final String replacement;

    /**
     * Create a StringReplacer
     * @param search The search string
     * @param replacement The replacement string
     */
    protected StringReplacer( String search, String replacement )
    {
        pattern = Pattern.compile( search );
        this.replacement = replacement;
    }

    /**
     * Perform the search/replace on the provided string
     * and return the new string
     * @param input - The input string
     * @return the new string
     */
    protected String replaceAll( String input )
    {
        return pattern.matcher( input ).replaceAll( replacement );
    }
}


