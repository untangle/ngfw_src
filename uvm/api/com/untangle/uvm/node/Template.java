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

package com.untangle.uvm.node;

import java.util.ArrayList;

/**
 * Class for creating Strings from templates.  The templates
 * may contain variables in the form $VAR_NAME$.  Note that
 * begining and end delims were chosen so one could
 * place two substitution variables next to each other.  The
 * alternative (LWS as end token for variables) was rejected as
 * someone could not create two adjacent variables w/o
 * whitespace.
 * <p>
 * Variables are delimited by "$".  The escape character "\" is used
 * to escape literal $.  <b>The escape character only escapes
 * subsequent $ characters.  It has no effect if the next character
 * is not $.</b>
 * <p>
 * Values for the delimited keys are obtained at runtime from
 * an instance of {@link TemplateValues TemplateValues}.  If a given
 * key cannot be found within a TemplateValues instance, there are
 * two choices for behavior.  If {@link #isPrintUnassigned printUnassigned}
 * is false nothing will be substituted.  If
 * {@link #isPrintUnassigned printUnassigned} is true, then
 * the original <code>$key$</code> will be substituted.  The latter is
 * useful for debugging.
 * <p>
 * Use of this class is threadsafe.  Changes to the internal
 * template will not be seen-by (or mess-up) concurrent
 * calls to {@link #format format} or {@link #formatInto formatInto}.
 */
public class Template {

    private static final char DELIM = '$';
    private static final char ESC = '\\';

    private ParsedTemplate m_parsed;


    /**
     * Construct a new Template which does
     * <b>not</b> print unassigned keys.
     */
    public Template(String template) {
        this(template, false);
    }

    /**
     * Construct a new Tempalte
     *
     * @param template the template
     * @param printUnassigned should unassigned keys
     *        be printed in original form.
     */
    public Template(String template,
                    boolean printUnassigned) {
        m_parsed = parse(template, printUnassigned);
    }


    public String getTemplate() {
        return m_parsed.orig;
    }

    /**
     * Set the raw template
     *
     * @param template
     */
    public void setTemplate(String template) {
        set(template, m_parsed.printUnassigned);
    }

    public boolean isPrintUnassigned() {
        return m_parsed.printUnassigned;
    }
    public void setPrintUnassigned(boolean printUnassigned) {
        if(m_parsed.printUnassigned != printUnassigned) {
            set(m_parsed.orig, printUnassigned);
        }
    }

    /**
     * Set both the tempalte and printUnassigned properties
     * atomiclly
     *
     * @param template the template
     * @param printUnassigned if true, unassigned values
     *        will be printed in their "$key$" form.
     */
    public void set(String template, boolean printUnassigned) {
        m_parsed = parse(template, printUnassigned);
    }

    /**
     * Format the contents of this template into
     * the given StringBuilder with the given
     * TemplateValues
     *
     * @param sb the StringBuilder
     * @param tv the TemplateValues
     */
    public void formatInto(StringBuilder sb,
                           TemplateValues tv) {
        m_parsed.formatInto(sb, tv);
    }

    /**
     * Format the contents of this template, using the
     * provided TemplateValues.
     *
     * @param tv the TemplateValues
     *
     * @return the formatted String
     */
    public String format(TemplateValues tv) {
        return m_parsed.format(tv).toString();
    }

    /*
      public static void main(String[] args)
      throws Exception {


      String template = "  twoSpaces before \\$ that should be a dollar-sign " +
      "$foo$ that was foo $foo$$goo$ that was foo and goo.  " +
      "This is an unmapped var $doo$.  This is an " +
      "unterninated key $dooo";

      MapTemplateValues values = new MapTemplateValues();
      values.setProperty("foo", "fooValue");
      values.setProperty("goo", "gooValue");

      Template t = new Template(template, false);

      System.out.println("--------BEGIN ORIG-----------");
      System.out.println(template);
      System.out.println("--------ENDOF ORIG-----------");
      System.out.println("--------BEGIN PRINT-----------");
      System.out.println(new Template(template, true).format(values));
      System.out.println("--------ENDOF PRINT-----------");
      System.out.println("--------BEGIN NOPRINT-----------");
      System.out.println(new Template(template, false).format(values));
      System.out.println("--------ENDOF NOPRINT-----------");
      }
    */

    private ParsedTemplate parse(String template,
                                 boolean printUnassigned) {

        //If template is null, return "null"
        //always
        if(template == null) {
            return new ParsedTemplate(template,
                                      new Part[] {new FixedPart("null")},
                                      4,
                                      printUnassigned);
        }

        ArrayList<Part> list = new ArrayList<Part>();

        int index = 0;
        final int len = template.length();

        int numKeys = 0;

        //Iterate through the characters of the
        //template.  The "currentBuilder" accumulates
        //the current token.  "drainingKey" determines
        //if the token is plain or text.
        StringBuilder currentBuilder = new StringBuilder();
        boolean drainingKey = false;

        while(true) {
            if(index >=len) {
                break;
            }
            char c = template.charAt(index++);
            if(c == DELIM) {
                if(drainingKey) {
                    //This is the end of a key
                    if(currentBuilder.length() > 0) {
                        numKeys++;
                        list.add(new VariablePart(currentBuilder.toString(), printUnassigned));
                    }
                    drainingKey = false;
                    currentBuilder = new StringBuilder();
                    continue;
                }
                else {
                    //Place the last builder into the list.  Handle special
                    //case where the previous builder was blank (adjacent keys).
                    if(currentBuilder.length() > 0) {
                        list.add(new FixedPart(currentBuilder.toString()));
                    }
                    currentBuilder = new StringBuilder();
                    drainingKey = true;
                    continue;
                }
            }
            if(c == ESC) {
                if(index < len) {
                    if(template.charAt(index) == DELIM) {
                        currentBuilder.append(DELIM);
                        index++;
                        continue;
                    }
                }
            }
            currentBuilder.append(c);
        }

        //Catch remaining
        if(currentBuilder.length() > 0) {
            list.add(new FixedPart(currentBuilder.toString()));
        }

        return new ParsedTemplate(template,
                                  list.toArray(new Part[list.size()]),
                                  template.length() + (numKeys*20),
                                  printUnassigned);
    }


    //--------- Inner Class Separator -------------------

    private class ParsedTemplate {
        final String orig;
        final Part[] parts;
        final int estSize;
        final int numParts;
        final boolean printUnassigned;

        ParsedTemplate(String orig,
                       Part[] parts,
                       int estSize,
                       boolean printUnassigned) {

            this.orig = orig;
            this.parts = parts;
            this.estSize = estSize;
            this.numParts = parts.length;
            this.printUnassigned = printUnassigned;
        }

        private StringBuilder format(TemplateValues tv) {
            StringBuilder sb = new StringBuilder(estSize);
            formatInto(sb, tv);
            return sb;
        }

        private void formatInto(StringBuilder sb, TemplateValues tv) {
            for(int i = 0; i<numParts; i++) {
                parts[i].append(sb, tv);
            }
        }
    }


    //--------- Inner Class Separator -------------------

    private abstract class Part {
        abstract void append(StringBuilder sb, TemplateValues values);
    }


    //--------- Inner Class Separator -------------------

    private class FixedPart extends Part {

        private final String m_data;

        FixedPart(String data) {
            m_data = data;
        }
        void append(StringBuilder sb, TemplateValues values) {
            sb.append(m_data);
        }
    }


    //--------- Inner Class Separator -------------------

    private class VariablePart extends Part {

        private final String m_key;
        private final boolean m_printUnassigned;

        VariablePart(String key,
                     boolean printUnassigned) {
            m_key = key;
            m_printUnassigned = printUnassigned;
        }
        void append(StringBuilder sb, TemplateValues values) {
            String val = values.getTemplateValue(m_key);
            if(val == null) {
                if(m_printUnassigned) {
                    reform(sb);
                }
            }
            else {
                sb.append(val);
            }
        }
        private void reform(StringBuilder sb) {
            sb.append(DELIM);
            sb.append(m_key);
            sb.append(DELIM);
        }
    }
}
