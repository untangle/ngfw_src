/**
 * $Id$
 */
package com.untangle.uvm.util;

public interface Worker
{
    public void work() throws InterruptedException;
    
    public void start();
    
    public void stop();
}
