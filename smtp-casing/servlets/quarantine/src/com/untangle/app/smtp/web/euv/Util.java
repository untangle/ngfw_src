/**
 * $Id$
 */
package com.untangle.app.smtp.web.euv;

import javax.servlet.ServletRequest;

/**
 * Utility methods
 */
public class Util {
    /**
     * Read a boolean parameter
     * 
     * @param  req       ServletRequest containing parameters.
     * @param  paramName Parameter to find.
     * @param  defaultValue       Default value to return if paramName is not found.
     * @return           Boolean value of the parameter value if found, otherwise the defaultValue.
     */
    public static boolean readBooleanParam(ServletRequest req, String paramName, boolean defaultValue) {
        String parameter = req.getParameter(paramName);
        if(parameter == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(parameter);
        }
        catch(Exception ex) { }

        return defaultValue;
    }

    /**
     * Read an int parameter
     * @param  req       ServletRequest containing parameters.
     * @param  paramName Parameter to find.
     * @param  defaultValue      Default value to return if paramName is not found.
     * @return           Integer value of the parameter value if found, otherwise the defaultValue.
     */
    public static int readIntParam(ServletRequest req, String paramName, int defaultValue) {
        String parameter = req.getParameter(paramName);
        if(parameter == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(parameter);
        }
        catch(Exception ex) { }

        return defaultValue;
    }
}
