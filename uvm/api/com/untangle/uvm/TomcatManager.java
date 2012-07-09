/**
 * $Id: TomcatManager.java,v 1.00 2012/07/09 13:10:06 dmorris Exp $
 */
package com.untangle.uvm;

import javax.servlet.ServletContext;

import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.Valve;

public interface TomcatManager
{
    public ServletContext loadSystemApp(String urlBase, String rootDir, Valve valve);

    public ServletContext loadInsecureApp(String urlBase, String rootDir);

    public ServletContext loadInsecureApp(String urlBase, String rootDir, Valve valve);

    public boolean unloadWebApp(String contextRoot);
    
}
