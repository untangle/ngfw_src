/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * CharSeqTransalator class for string escape and unescape util
 */
public class CharSeqTransalator {
    
    private final Map<String, String> lookupMap;
    private final BitSet prefixSet;
    private final int shortest;
    private final int longest;

    /**
     * Constructs the lookup table to be used in translation
     * 
     * @param lookupMap table of translator mappings
     */
    public CharSeqTransalator(final Map<CharSequence, CharSequence> lookupMap) {
        if (lookupMap == null) {
            throw new InvalidParameterException("lookupMap cannot be null");
        }
        this.lookupMap = new HashMap<>();
        this.prefixSet = new BitSet();
        int currentShortest = Integer.MAX_VALUE;
        int currentLongest = 0;

        for (final Map.Entry<CharSequence, CharSequence> pair : lookupMap.entrySet()) {
            this.lookupMap.put(pair.getKey().toString(), pair.getValue().toString());
            this.prefixSet.set(pair.getKey().charAt(0));
            final int sz = pair.getKey().length();
            if (sz < currentShortest) {
                currentShortest = sz;
            }
            if (sz > currentLongest) {
                currentLongest = sz;
            }
        }
        this.shortest = currentShortest;
        this.longest = currentLongest;
    }

    /**
     * Wrapper method for transalation task
     * 
     * @param input CharSequence to be translated
     * @param isEscape boolean to distinguish escape and unescape calls
     * @return String output of translation
     */
    public final String translate(final CharSequence input, final boolean isEscape) {
        if (input == null) {
            return null;
        }
        try {
            final StringWriter writer = new StringWriter(input.length() * 2);
            translate(input, writer, !isEscape);
            return writer.toString();
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Translate an input onto a Writer.
     * 
     * @param input CharSequence that is being translated
     * @param writer Writer to translate the text to
     * @param callNumeric boolean to make numericTranslate method call for unescape task
     * @throws IOException if and only if the Writer produces an IOException
     */
    public final void translate(final CharSequence input, final Writer writer, final boolean callNumeric) throws IOException {
        Validate.isTrue(writer != null, "The Writer must not be null");
        if (input == null) {
            return;
        }
        int pos = 0;
        final int len = input.length();
        while (pos < len) {
            final int consumed = translate(input, pos, writer, callNumeric);
            if (consumed == 0) {
                final char c1 = input.charAt(pos);
                writer.write(c1);
                pos++;
                if (Character.isHighSurrogate(c1) && pos < len) {
                    final char c2 = input.charAt(pos);
                    if (Character.isLowSurrogate(c2)) {
                      writer.write(c2);
                      pos++;
                    }
                }
                continue;
            }
            for (int pt = 0; pt < consumed; pt++) {
                pos += Character.charCount(Character.codePointAt(input, pos));
            }
        }
    }

    /**
     * Wrapper translate method to distinguish escape and unescape functionality
     *
     * @param input CharSequence that is being translated
     * @param index int representing the current point of translation
     * @param writer Writer to translate the text to
     * @param callNumeric boolean to determine whether we need to call numericTransalate method or not
     * @return int count of code points consumed
     * @throws IOException if and only if the Writer produces an IOException
     */
    public int translate(final CharSequence input, final int index, final Writer writer, final boolean callNumeric) throws IOException {
        final int consumed = lookupTranslate(input, index, writer);
        if (consumed != 0) {
            return consumed;
        } else if(callNumeric) {
            return numericTranslate(input, index, writer);
        }
        return 0;
    }

    /**
     * Translates a set of code points, represented by an int index into a CharSequence,
     * into another set of code points. The number of code points consumed are returned,
     * and the only IOExceptions thrown are from interacting with the Writer.
     *
     * @param input CharSequence that is being translated
     * @param index int representing the current point of translation
     * @param writer Writer to translate the text to
     * @return int count of code points consumed
     * @throws IOException if and only if the Writer produces an IOException
     */
    public int lookupTranslate(final CharSequence input, final int index, final Writer writer) throws IOException {
        // check if translation exists for the input at position index
        if (prefixSet.get(input.charAt(index))) {
            int max = longest;
            if (index + longest > input.length()) {
                max = input.length() - index;
            }
            // implement greedy algorithm by trying maximum match first
            for (int i = max; i >= shortest; i--) {
                final CharSequence subSeq = input.subSequence(index, index + i);
                final String result = lookupMap.get(subSeq.toString());

                if (result != null) {
                    writer.write(result);
                    return Character.codePointCount(subSeq, 0, subSeq.length());
                }
            }
        }
        return 0;
    }

    /**
     * Translates a set of code points, represented by an int index into a CharSequence,
     * into another set of code points. The number of code points consumed are returned,
     * and the only IOExceptions thrown are from interacting with the Writer.
     *
     * @param input CharSequence that is being translated
     * @param index int representing the current point of translation
     * @param writer Writer to translate the text to
     * @return int count of code points consumed
     * @throws IOException if and only if the Writer produces an IOException
     */
    public int numericTranslate(final CharSequence input, final int index, final Writer writer) throws IOException {
        final int seqEnd = input.length();
        // Uses -2 to ensure there is something after the &#
        if (input.charAt(index) == '&' && index < seqEnd - 2 && input.charAt(index + 1) == '#') {
            int start = index + 2;
            boolean isHex = false;

            final char firstChar = input.charAt(start);
            if (firstChar == 'x' || firstChar == 'X') {
                start++;
                isHex = true;

                // Check there's more than just an x after the &#
                if (start == seqEnd) {
                    return 0;
                }
            }

            int end = start;
            // Note that this supports character codes without a ; on the end
            while (end < seqEnd && (input.charAt(end) >= '0' && input.charAt(end) <= '9'
                                    || input.charAt(end) >= 'a' && input.charAt(end) <= 'f'
                                    || input.charAt(end) >= 'A' && input.charAt(end) <= 'F')) {
                end++;
            }

            final boolean semiNext = end != seqEnd && input.charAt(end) == ';';

            final int entityValue;
            try {
                if (isHex) {
                    entityValue = Integer.parseInt(input.subSequence(start, end).toString(), 16);
                } else {
                    entityValue = Integer.parseInt(input.subSequence(start, end).toString(), 10);
                }
            } catch (final NumberFormatException nfe) {
                return 0;
            }

            if (entityValue > 0xFFFF) {
                final char[] chrs = Character.toChars(entityValue);
                writer.write(chrs[0]);
                writer.write(chrs[1]);
            } else {
                writer.write(entityValue);
            }

            return 2 + end - start + (isHex ? 1 : 0) + (semiNext ? 1 : 0);
        }
        return 0;
    }
}

