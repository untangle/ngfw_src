/**
 * $Id: WebFilterReplacementGenerator.java 41284 2015-09-18 07:03:39Z dmorris $
 */

package com.untangle.app.web_filter;

import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for Web Filter.
 */
public class WebFilterReplacementGenerator extends WebFilterBaseReplacementGenerator
{
    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param app
     *        The application
     */
    public WebFilterReplacementGenerator(AppSettings appSettings, WebFilterBase app)
    {
        super(appSettings, app);
    }

}
