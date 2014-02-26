/**
 * $Id$
 */
package com.untangle.uvm;

import javax.servlet.ServletContext;

public interface TomcatManager
{
    final String UVM_WEB_MESSAGE_ATTR = "com.untangle.uvm.web.message";

    public ServletContext loadServlet(String urlBase, String rootDir);

    public ServletContext loadServlet(String urlBase, String rootDir, boolean requireAdminPrivs);

    public boolean unloadServlet(String contextRoot);
}
