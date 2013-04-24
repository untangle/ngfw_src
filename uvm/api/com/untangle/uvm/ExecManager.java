/**
 * $Id: ExecManager.java,v 1.00 2012/01/31 23:04:51 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.IOException;

public interface ExecManager
{
    /**
     * Execute the specified command and return the result
     */
    ExecManagerResult exec( String cmd );

    /**
     * Execute the specified command and return the exit code
     */
    Integer execResult( String cmd );

    /**
     * Execute the specified command and return the stdout
     */
    String  execOutput( String cmd );

    /**
     * Execute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    ExecManagerResultReader execEvil( String cmd[] ) throws IOException;

    /**
     * Execute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    ExecManagerResultReader execEvil( String cmd ) throws IOException;

    /**
     * Close this instance of exec manager
     * This shuts down the "launcher" process
     * after this this ExecManager can no longer be used
     */
    void close();

    /**
     * Just combines all the arguments into one string and quotes them
     * Also escapes any quote characters in the arguments themselves
     */
    String argBuilder( String[] args );
}
