/**
 * $Id: TomcatManager.java,v 1.00 2012/07/09 13:10:06 dmorris Exp $
 */
package com.untangle.uvm;

import javax.servlet.ServletContext;

public interface TomcatManager
{
    public ServletContext loadServlet(String urlBase, String rootDir);

    public boolean unloadServlet(String contextRoot);
}
