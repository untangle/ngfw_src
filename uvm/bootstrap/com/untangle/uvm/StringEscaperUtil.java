/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * String Escape utility
 */
public class StringEscaperUtil {

    /**
     * CharSequence mapping for escape operations 
     */
    public static final Map<CharSequence, CharSequence> CHAR_SEQUENCE_ESCAPE_MAP;

    static {
        final Map<CharSequence, CharSequence> charMap = new HashMap<>();

        // ISO-8859-1 characters
        charMap.put("\u00A0", "&nbsp;");
        charMap.put("\u00A1", "&iexcl;");
        charMap.put("\u00A2", "&cent;");
        charMap.put("\u00A3", "&pound;");
        charMap.put("\u00A4", "&curren;");
        charMap.put("\u00A5", "&yen;");
        charMap.put("\u00A6", "&brvbar;");
        charMap.put("\u00A7", "&sect;");
        charMap.put("\u00A8", "&uml;");
        charMap.put("\u00A9", "&copy;");
        charMap.put("\u00AA", "&ordf;");
        charMap.put("\u00AB", "&laquo;");
        charMap.put("\u00AC", "&not;");
        charMap.put("\u00AD", "&shy;");
        charMap.put("\u00AE", "&reg;");
        charMap.put("\u00AF", "&macr;");
        charMap.put("\u00B0", "&deg;");
        charMap.put("\u00B1", "&plusmn;");
        charMap.put("\u00B2", "&sup2;");
        charMap.put("\u00B3", "&sup3;");
        charMap.put("\u00B4", "&acute;");
        charMap.put("\u00B5", "&micro;");
        charMap.put("\u00B6", "&para;");
        charMap.put("\u00B7", "&middot;");
        charMap.put("\u00B8", "&cedil;");
        charMap.put("\u00B9", "&sup1;");
        charMap.put("\u00BA", "&ordm;");
        charMap.put("\u00BB", "&raquo;");
        charMap.put("\u00BC", "&frac14;");
        charMap.put("\u00BD", "&frac12;");
        charMap.put("\u00BE", "&frac34;");
        charMap.put("\u00BF", "&iquest;");
        charMap.put("\u00C0", "&Agrave;");
        charMap.put("\u00C1", "&Aacute;");
        charMap.put("\u00C2", "&Acirc;");
        charMap.put("\u00C3", "&Atilde;");
        charMap.put("\u00C4", "&Auml;");
        charMap.put("\u00C5", "&Aring;");
        charMap.put("\u00C6", "&AElig;");
        charMap.put("\u00C7", "&Ccedil;");
        charMap.put("\u00C8", "&Egrave;");
        charMap.put("\u00C9", "&Eacute;");
        charMap.put("\u00CA", "&Ecirc;");
        charMap.put("\u00CB", "&Euml;");
        charMap.put("\u00CC", "&Igrave;");
        charMap.put("\u00CD", "&Iacute;");
        charMap.put("\u00CE", "&Icirc;");
        charMap.put("\u00CF", "&Iuml;");
        charMap.put("\u00D0", "&ETH;");
        charMap.put("\u00D1", "&Ntilde;");
        charMap.put("\u00D2", "&Ograve;");
        charMap.put("\u00D3", "&Oacute;");
        charMap.put("\u00D4", "&Ocirc;");
        charMap.put("\u00D5", "&Otilde;");
        charMap.put("\u00D6", "&Ouml;");
        charMap.put("\u00D7", "&times;");
        charMap.put("\u00D8", "&Oslash;");
        charMap.put("\u00D9", "&Ugrave;");
        charMap.put("\u00DA", "&Uacute;");
        charMap.put("\u00DB", "&Ucirc;");
        charMap.put("\u00DC", "&Uuml;");
        charMap.put("\u00DD", "&Yacute;");
        charMap.put("\u00DE", "&THORN;");
        charMap.put("\u00DF", "&szlig;");
        charMap.put("\u00E0", "&agrave;");
        charMap.put("\u00E1", "&aacute;");
        charMap.put("\u00E2", "&acirc;");
        charMap.put("\u00E3", "&atilde;");
        charMap.put("\u00E4", "&auml;");
        charMap.put("\u00E5", "&aring;");
        charMap.put("\u00E6", "&aelig;");
        charMap.put("\u00E7", "&ccedil;");
        charMap.put("\u00E8", "&egrave;");
        charMap.put("\u00E9", "&eacute;");
        charMap.put("\u00EA", "&ecirc;");
        charMap.put("\u00EB", "&euml;");
        charMap.put("\u00EC", "&igrave;");
        charMap.put("\u00ED", "&iacute;");
        charMap.put("\u00EE", "&icirc;");
        charMap.put("\u00EF", "&iuml;");
        charMap.put("\u00F0", "&eth;");
        charMap.put("\u00F1", "&ntilde;");
        charMap.put("\u00F2", "&ograve;");
        charMap.put("\u00F3", "&oacute;");
        charMap.put("\u00F4", "&ocirc;");
        charMap.put("\u00F5", "&otilde;");
        charMap.put("\u00F6", "&ouml;");
        charMap.put("\u00F7", "&divide;");
        charMap.put("\u00F8", "&oslash;");
        charMap.put("\u00F9", "&ugrave;");
        charMap.put("\u00FA", "&uacute;");
        charMap.put("\u00FB", "&ucirc;");
        charMap.put("\u00FC", "&uuml;");
        charMap.put("\u00FD", "&yacute;");
        charMap.put("\u00FE", "&thorn;");
        charMap.put("\u00FF", "&yuml;");

        // Additional Character entity references
        charMap.put("\u0192", "&fnof;");
        charMap.put("\u0391", "&Alpha;");
        charMap.put("\u0392", "&Beta;");
        charMap.put("\u0393", "&Gamma;");
        charMap.put("\u0394", "&Delta;");
        charMap.put("\u0395", "&Epsilon;");
        charMap.put("\u0396", "&Zeta;");
        charMap.put("\u0397", "&Eta;");
        charMap.put("\u0398", "&Theta;");
        charMap.put("\u0399", "&Iota;");
        charMap.put("\u039A", "&Kappa;");
        charMap.put("\u039B", "&Lambda;");
        charMap.put("\u039C", "&Mu;");
        charMap.put("\u039D", "&Nu;");
        charMap.put("\u039E", "&Xi;");
        charMap.put("\u039F", "&Omicron;");
        charMap.put("\u03A0", "&Pi;");
        charMap.put("\u03A1", "&Rho;");
        charMap.put("\u03A3", "&Sigma;");
        charMap.put("\u03A4", "&Tau;");
        charMap.put("\u03A5", "&Upsilon;");
        charMap.put("\u03A6", "&Phi;");
        charMap.put("\u03A7", "&Chi;");
        charMap.put("\u03A8", "&Psi;");
        charMap.put("\u03A9", "&Omega;");
        charMap.put("\u03B1", "&alpha;");
        charMap.put("\u03B2", "&beta;");
        charMap.put("\u03B3", "&gamma;");
        charMap.put("\u03B4", "&delta;");
        charMap.put("\u03B5", "&epsilon;");
        charMap.put("\u03B6", "&zeta;");
        charMap.put("\u03B7", "&eta;");
        charMap.put("\u03B8", "&theta;");
        charMap.put("\u03B9", "&iota;");
        charMap.put("\u03BA", "&kappa;");
        charMap.put("\u03BB", "&lambda;");
        charMap.put("\u03BC", "&mu;");
        charMap.put("\u03BD", "&nu;");
        charMap.put("\u03BE", "&xi;");
        charMap.put("\u03BF", "&omicron;");
        charMap.put("\u03C0", "&pi;");
        charMap.put("\u03C1", "&rho;");
        charMap.put("\u03C2", "&sigmaf;");
        charMap.put("\u03C3", "&sigma;");
        charMap.put("\u03C4", "&tau;");
        charMap.put("\u03C5", "&upsilon;");
        charMap.put("\u03C6", "&phi;");
        charMap.put("\u03C7", "&chi;");
        charMap.put("\u03C8", "&psi;");
        charMap.put("\u03C9", "&omega;");
        charMap.put("\u03D1", "&thetasym;");
        charMap.put("\u03D2", "&upsih;");
        charMap.put("\u03D6", "&piv;");
        charMap.put("\u2022", "&bull;");
        charMap.put("\u2026", "&hellip;");
        charMap.put("\u2032", "&prime;");
        charMap.put("\u2033", "&Prime;");
        charMap.put("\u203E", "&oline;");
        charMap.put("\u2044", "&frasl;");
        charMap.put("\u2118", "&weierp;");
        charMap.put("\u2111", "&image;");
        charMap.put("\u211C", "&real;");
        charMap.put("\u2122", "&trade;");
        charMap.put("\u2135", "&alefsym;");
        charMap.put("\u2190", "&larr;");
        charMap.put("\u2191", "&uarr;");
        charMap.put("\u2192", "&rarr;");
        charMap.put("\u2193", "&darr;");
        charMap.put("\u2194", "&harr;");
        charMap.put("\u21B5", "&crarr;");
        charMap.put("\u21D0", "&lArr;");
        charMap.put("\u21D1", "&uArr;");
        charMap.put("\u21D2", "&rArr;");
        charMap.put("\u21D3", "&dArr;");
        charMap.put("\u21D4", "&hArr;");
        charMap.put("\u2200", "&forall;");
        charMap.put("\u2202", "&part;");
        charMap.put("\u2203", "&exist;");
        charMap.put("\u2205", "&empty;");
        charMap.put("\u2207", "&nabla;");
        charMap.put("\u2208", "&isin;");
        charMap.put("\u2209", "&notin;");
        charMap.put("\u220B", "&ni;");
        charMap.put("\u220F", "&prod;");
        charMap.put("\u2211", "&sum;");
        charMap.put("\u2212", "&minus;");
        charMap.put("\u2217", "&lowast;");
        charMap.put("\u221A", "&radic;");
        charMap.put("\u221D", "&prop;");
        charMap.put("\u221E", "&infin;");
        charMap.put("\u2220", "&ang;");
        charMap.put("\u2227", "&and;");
        charMap.put("\u2228", "&or;");
        charMap.put("\u2229", "&cap;");
        charMap.put("\u222A", "&cup;");
        charMap.put("\u222B", "&int;");
        charMap.put("\u2234", "&there4;");
        charMap.put("\u223C", "&sim;");
        charMap.put("\u2245", "&cong;");
        charMap.put("\u2248", "&asymp;");
        charMap.put("\u2260", "&ne;");
        charMap.put("\u2261", "&equiv;");
        charMap.put("\u2264", "&le;");
        charMap.put("\u2265", "&ge;");
        charMap.put("\u2282", "&sub;");
        charMap.put("\u2283", "&sup;");
        charMap.put("\u2284", "&nsub;");
        charMap.put("\u2286", "&sube;");
        charMap.put("\u2287", "&supe;");
        charMap.put("\u2295", "&oplus;");
        charMap.put("\u2297", "&otimes;");
        charMap.put("\u22A5", "&perp;");
        charMap.put("\u22C5", "&sdot;");
        charMap.put("\u2308", "&lceil;");
        charMap.put("\u2309", "&rceil;");
        charMap.put("\u230A", "&lfloor;");
        charMap.put("\u230B", "&rfloor;");
        charMap.put("\u2329", "&lang;");
        charMap.put("\u232A", "&rang;");
        charMap.put("\u25CA", "&loz;");
        charMap.put("\u2660", "&spades;");
        charMap.put("\u2663", "&clubs;");
        charMap.put("\u2665", "&hearts;");
        charMap.put("\u2666", "&diams;");
        charMap.put("\u0152", "&OElig;");
        charMap.put("\u0153", "&oelig;");
        charMap.put("\u0160", "&Scaron;");
        charMap.put("\u0161", "&scaron;");
        charMap.put("\u0178", "&Yuml;");
        charMap.put("\u02C6", "&circ;");
        charMap.put("\u02DC", "&tilde;");
        charMap.put("\u2002", "&ensp;");
        charMap.put("\u2003", "&emsp;");
        charMap.put("\u2009", "&thinsp;");
        charMap.put("\u200C", "&zwnj;");
        charMap.put("\u200D", "&zwj;");
        charMap.put("\u200E", "&lrm;");
        charMap.put("\u200F", "&rlm;");
        charMap.put("\u2013", "&ndash;");
        charMap.put("\u2014", "&mdash;");
        charMap.put("\u2018", "&lsquo;");
        charMap.put("\u2019", "&rsquo;");
        charMap.put("\u201A", "&sbquo;");
        charMap.put("\u201C", "&ldquo;");
        charMap.put("\u201D", "&rdquo;");
        charMap.put("\u201E", "&bdquo;");
        charMap.put("\u2020", "&dagger;");
        charMap.put("\u2021", "&Dagger;");
        charMap.put("\u2030", "&permil;");
        charMap.put("\u2039", "&lsaquo;");
        charMap.put("\u203A", "&rsaquo;");
        charMap.put("\u20AC", "&euro;");

        // Basic XML and HTML character entities
        charMap.put("\"", "&quot;");
        charMap.put("&", "&amp;");
        charMap.put("<", "&lt;");
        charMap.put(">", "&gt;");

        CHAR_SEQUENCE_ESCAPE_MAP = Collections.unmodifiableMap(charMap);
    }

    /**
     * CharSequence mapping for unescape operations 
     */
    public static final Map<CharSequence, CharSequence> CHAR_SEQUENCE_UNESCAE_MAP;

    static {
        CHAR_SEQUENCE_UNESCAE_MAP = Collections.unmodifiableMap(invert(CHAR_SEQUENCE_ESCAPE_MAP));
    }

    public static final CharSeqTransalator ESCAPE_HTML4_TRANSLATOR = new CharSeqTransalator(CHAR_SEQUENCE_ESCAPE_MAP);
    public static final CharSeqTransalator UNESCAPE_HTML4_TRANSLATOR = new CharSeqTransalator(CHAR_SEQUENCE_UNESCAE_MAP);

    /**
     * Escapes the characters in a {@code String} using HTML entities.
     *
     * @param input  the {@code String} to escape, may be null
     * @return a new escaped {@code String}, {@code null} if null string input
     */
    public static final String escapeHtml4(final String input) {
        return ESCAPE_HTML4_TRANSLATOR.translate(input, true);
    }

    /**
     * Unescapes a string containing escapes to a string containing 
     * the actual Unicode characters corresponding to the escapes.
     *
     * @param input  the {@code String} to unescape, may be null
     * @return a new unescaped {@code String}, {@code null} if null string input
     */
    public static final String unescapeHtml4(final String input) {
        return UNESCAPE_HTML4_TRANSLATOR.translate(input, false);
    }

    /**
     * Inverts an escape Map into an unescape Map.
     *
     * @param map to be inverted
     * @return inverted map
     */
    public static Map<CharSequence, CharSequence> invert(final Map<CharSequence, CharSequence> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
    }
}
