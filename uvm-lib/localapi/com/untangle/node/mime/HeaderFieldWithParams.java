/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mime;

import static com.untangle.node.util.Ascii.COLON;

import java.io.IOException;
import java.util.Iterator;


/**
 * Base class for MIME headers which follow the general
 * format "HNAME: VALUE[; name=value]".  I cannot seem
 * to find anything in the RFCs to define this format,
 * so I'll call the first VALUE the "primaryValue".  The
 * subclass is thus responsible for providing implementations
 * to {@link #parsePrimaryValue parse} and
 * {@link #writePrimaryValue unparse} this "primaryValue".
 * <br><br>
 * There is also a lot of goofyness around how mailers
 * open the spec.  There are observed cases which make no
 * sense when reading the spec.  To handle this, subclasses
 * also instruct the base class about how given parameters
 * should be parsed via the {@link #getParamParsePolicy getParamParsePolicy}
 * method.
 */
public abstract class HeaderFieldWithParams
    extends HeaderField {

    /**
     * Indicators for parsing.
     */
    protected enum ParamParsePolicy {
        /**
         * Either a quoted token, or anyting up to the "end"
         */
        QTEXT_OR_ALL,
        /**
         * A single token, but may be quoted
         */
        ATOM_OR_QTEXT,
        /**
         * Anything up-to the next preceived param name
         */
        LOOSE
    };

    private final ParamList m_paramList = new ParamList();

    public HeaderFieldWithParams(String name, LCString lcString) {
        super(name, lcString);
    }

    /**
     * Set the named parameter.  Passing null
     * as the <code>value</code> is an implicit
     * {@link #removeParam remove}.
     *
     * @param key the param name
     * @param value the param value
     */
    public void setParam(String key, String value) {
        m_paramList.set(key, value);
    }

    /**
     * Remove the named parameter.  If the param
     * is not mapped, no error is encountered
     *
     * @param key the name of the parameter to remove
     */
    public void removeParam(LCString key) {
        m_paramList.remove(key);
    }

    /**
     * Test if the named parameter is mapped
     *
     * @param key the key
     *
     * @return true if a call to {@link #getParam getParam}
     *         will return a non-null value
     */
    public boolean containsParam(LCString key) {
        return m_paramList.contains(key);
    }

    /**
     * Get the named parameter
     *
     * @param key the key
     *
     * @return the value, or null if
     *         {@link #containsParam not mapped}
     */
    public String getParam(LCString key) {
        return m_paramList.get(key);
    }

    /**
     * Remove all key/value pairs from this list
     */
    public void clearParams() {
        m_paramList.clear();
    }
    /**
     * Get all parameter names (keys)
     */
    public Iterator<LCString> paramKeys() {
        return m_paramList.keys();
    }


    /**
     * For subclasses to define how a given param name should be parsed.
     * For example, Outlook and Outlook Express take the following:
     * <pre>
     * boundary="------------070407010503030002060104"xRND_CRAP; foo="moo"
     * </pre>
     * And assume the boundary is ------------070407010503030002060104.  This
     * is an example of "QTEXT_OR_ALL".
     *
     * By default, this method returns "LOOSE"
     */
    protected ParamParsePolicy getParamParsePolicy(String paramName) {
        return ParamParsePolicy.LOOSE;
    }

    /**
     * Helper method which parses the standard key/value
     * list of parameters.  While this method is being called,
     * the {@link #getParamParsePolicy getParamParsePolicy} method
     * will be called for each encountered parameter.
     *
     * @param t the tokenizer, positioned after any initial
     *        header value information.
     */
    private final void parseParams(HeaderFieldTokenizer t)
        throws HeaderParseException {
        HeaderFieldTokenizer.Token token = null;
        while(true) {

            token = t.nextTokenIgnoreComments();
            if(token == null) {
                break;
            }
            //      System.out.println("Loop Top: " + token.toString());
            //Always begin this loop looking for the param name.  Assume
            //leftover from previous parse
            if(token.getType() == HeaderFieldTokenizer.TokenType.DELIM) {
                if(token.getDelim() == ' ' ||
                   token.getDelim() == '\t' ||
                   token.getDelim() == ';' ||
                   token.getDelim() == ':') {
                    //          System.out.println("False start, continue");
                    continue;
                }
            }
            //Consume all tokens until we find an "=".
            //If we hit a ";", reset
            StringBuilder nameB = new StringBuilder();
            while(token != null &&
                  token.getDelim() != '=') {
                nameB.append(token.toString());
                //        System.out.println("Append " + token.toString() + " to field name");
                //If we hit a ";", discard
                if(token.getDelim() == ';') {
                    nameB = new StringBuilder();
                    //          System.out.println(";, reset field name");
                }
                token = t.nextTokenIgnoreComments();
                //        System.out.println("Looking for Field name.  Read: " + token);
            }

            //Check if we hit the end
            if(token == null) {
                //        System.out.println("Token null.  Punt");
                return;
            }
            //Consume value
            String paramName = nameB.toString().trim();
            if("".equals(paramName)) {
                continue;//More crap
            }

            ParamParsePolicy policy = getParamParsePolicy(paramName);
            StringBuilder valueB = new StringBuilder();
            //TODO bscott what about name=;
            switch(policy) {
            case ATOM_OR_QTEXT:
                token = t.nextTokenIgnoreComments();
                if(token == null) {
                    break;
                }
                if(token.getType() == HeaderFieldTokenizer.TokenType.QTEXT ||
                   token.getType() == HeaderFieldTokenizer.TokenType.ATOM) {
                    m_paramList.set(paramName, token.toString());
                    break;
                }
                break;
            case QTEXT_OR_ALL:
                token = t.nextTokenIgnoreComments();
                if(token == null) {
                    break;
                }
                if(token.getType() == HeaderFieldTokenizer.TokenType.QTEXT) {
                    m_paramList.set(paramName, token.toString());
                    break;
                }
                //Follow through
                valueB.append(token.toString());
            default:
                while((token = t.nextTokenIgnoreComments()) != null) {
                    if(token.getDelim() == ';') {
                        m_paramList.set(paramName, valueB.toString());
                        break;
                    }
                    valueB.append(token.toString());
                }
                if(token == null) {
                    m_paramList.set(paramName, valueB.toString());
                }
            }
        }
    }

    /**
     * Subclasses should read the primaryValue from the tokenizer.
     *
     * @param t the tokenizer
     *
     * @exception HeaderParseException if the subclass doesn't
     *            like the primary value.
     */
    protected abstract void parsePrimaryValue(HeaderFieldTokenizer t)
        throws HeaderParseException;

    @Override
    protected final void parseStringValue()
        throws HeaderParseException {
        HeaderFieldTokenizer t = new HeaderFieldTokenizer(getValueAsString());
        parsePrimaryValue(t);
        parseParams(t);
    }

    /*
      public static void main(String[] args) throws Exception {
      test("foo=goo");
      test("foo=$#_goo; doo=eoo");
      test("foo=\"goo\"");
      test("foo=goo doo=foo");
      test("foo=goo foo doo");
      }
      private static void test(String str) throws Exception {
      System.out.println("\n\n=======================\n");
      System.out.println(str + "\n--------------");
      HeaderFieldWithParams hfwp = new HeaderFieldWithParams("foo", new LCString("foo"));
      HeaderFieldTokenizer t = new HeaderFieldTokenizer(str);
      hfwp.parseParams(t);

      StringBuilder sb = new StringBuilder();
      hfwp.m_paramList.writeOut(sb, 0);
      System.out.println("--------");
      System.out.println(sb.toString());

      }
    */



    @Override
    public final void parseLines()
        throws HeaderParseException {
        parseStringValue();
    }

    /**
     * Write the "primaryValue".  The StringBuilder is positioned
     * just-past the colon (":") delimiting the header name.
     *
     * @param sb the StringBuilder into-which the
     *        primary value should be written.
     */
    protected abstract void writePrimaryValue(StringBuilder sb);

    @Override
    public final void writeToAssemble(MIMEOutputStream out)
        throws IOException {
        out.writeLine(toString());
    }

    /**
     * For debugging
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(COLON);
        writePrimaryValue(sb);
        m_paramList.writeOut(sb, sb.length());
        return sb.toString();
    }
}
