/**
 * $Id: ExecResultReader.java,v 1.00 2013/04/19 23:04:51 vdumitrescu Exp $
 */
package com.untangle.uvm;


public interface ExecResultReader
{
   Integer getResult();
   String readFromOutput();
   void destroy();
}
