/**
 * $Id$
 */
package com.untangle.app.smtp;

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
     * @param template String to initialize template with.
     * @return instance of Template.
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
     * @return instance of Template.
     */
    public Template(String template,
                    boolean printUnassigned) {
        m_parsed = parse(template, printUnassigned);
    }

    /**
     * Get template.
     * @return String of template.
     */
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

    /**
     * Determine if print is unassigned.
     * @return true if print is unassigned, false otherwise.
     */
    public boolean isPrintUnassigned() {
        return m_parsed.printUnassigned;
    }
    /**
     * Set print unassigned.
     * @param printUnassigned if true set unassigned, otherwise set assigned.
     */
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
   
   /**
    * Parse the template.
    * @param  template        String of template.
    * @param  printUnassigned If true, print unassigned, otherwise print assigned.
    * @return                 ParsedTemplate instance.
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

    /**
     * Parsed template.
     */
    private class ParsedTemplate {
        final String orig;
        final Part[] parts;
        final int estSize;
        final int numParts;
        final boolean printUnassigned;

        /**
         * Initialize instance of ParsedTemplate.
         * @param orig Original template.
         * @param parts Array of Parts.
         * @param estSize Estimated size.
         * @param printUnassigned if true, unassigned, false otherwise.
         * @return instance of ParsedTemplate
         */
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

        /**
         * Format template.
         * @param  tv Template values.
         * @return    template with replaced vaues.
         */
        private StringBuilder format(TemplateValues tv) {
            StringBuilder sb = new StringBuilder(estSize);
            formatInto(sb, tv);
            return sb;
        }

        /**
         * Format template into  stringbuilder.
         * @param sb StringBuilder to format into.
         * @param  tv Template values.
         */
        private void formatInto(StringBuilder sb, TemplateValues tv) {
            for(int i = 0; i<numParts; i++) {
                parts[i].append(sb, tv);
            }
        }
    }


    //--------- Inner Class Separator -------------------

    /**
     * Part
     */
    private abstract class Part {
      /**
       * Append to part.
       * @param sb     StringBuilder to append to.
       * @param values TemplateValues to use.
       */
        abstract void append(StringBuilder sb, TemplateValues values);
    }


    //--------- Inner Class Separator -------------------

    /**
     * Fixed prt.
     */
    private class FixedPart extends Part {

        private final String m_data;

        /**
         * Initialize instnace of FixedPart.
         * @param data Data to go into part.
         * @return Instance of FixedPart.
         */
        FixedPart(String data) {
            m_data = data;
        }

        /**
         * Add to fixed part.
         * @param sb     StringBuilder to append to.
         * @param values TemplateValues to use.
         */
        void append(StringBuilder sb, TemplateValues values) {
            sb.append(m_data);
        }
    }


    //--------- Inner Class Separator -------------------

    /**
     * Variable part.
     */
    private class VariablePart extends Part {

        private final String m_key;
        private final boolean m_printUnassigned;

        /**
         * Initialize instance of VariablePart.
         * @param key String to use.
         * @param printUnassigned if true, print unasigned, otherwise false.
         */
        VariablePart(String key,
                     boolean printUnassigned) {
            m_key = key;
            m_printUnassigned = printUnassigned;
        }

        /**
         * Append to variable.
         * @param sb     StringBuilder to write to.
         * @param values TemplateValues to use.
         */
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
        /**
         * Reformat using delimeter.
         * @param sb     StringBuilder to write to.
         */
        private void reform(StringBuilder sb) {
            sb.append(DELIM);
            sb.append(m_key);
            sb.append(DELIM);
        }
    }
}
