/**
 * $Id: ExecManager.java,v 1.00 2012/01/31 23:04:51 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.IOException;

public interface ExecManager
{
    ExecManagerResult exec(String cmd);

    Integer execResult(String cmd);

    String execOutput(String cmd);

    Process execEvil(String cmd[]) throws IOException;

    Process execEvil(String cmd) throws IOException;

    void close();
}