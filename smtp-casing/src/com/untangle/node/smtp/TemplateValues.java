/**
 * $Id$
 */
package com.untangle.node.smtp;

/**
 * A TemplateValues instance is used in conjunction with a
 * {@link com.untangle.uvm.util.Template Template}.  The TemplateValues
 * provides Strings which are mapped to the keys found in a template.
 * <br>
 */
public interface TemplateValues {

    /**
     * Access the value for the given key.  Null
     * is returned if the key cannot be mapped
     * to a value.
     *
     * @param key the key
     * @return the value, or null.
     */
    public String getTemplateValue(String key);
}
