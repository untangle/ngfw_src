/**
 * $Id: ExecManager.java,v 1.00 2012/01/31 23:04:51 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.InputStream;
import java.io.OutputStream;

public interface ProcessWrapper
{
   void destroy();
   int exitValue();
   InputStream getErrorStream();
   InputStream getInputStream();
   OutputStream getOutputStream();
   int waitFor() throws InterruptedException;
   String getOuptut();
}
