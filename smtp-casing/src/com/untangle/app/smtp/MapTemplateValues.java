/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Implementation of TemplateValues which acts as
 * a map (really, a java.util.Properties).
 *
 * @see Template
 */
@SuppressWarnings("serial")
public class MapTemplateValues
    extends java.util.Properties
    implements TemplateValues {


    public String getTemplateValue(String key) {
        return getProperty(key);
    }

}
