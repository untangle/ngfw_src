/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.IOException;
import org.apache.logging.log4j.Level;

public interface ExecManager
{
    /**
     * Execute the specified command and return the result
     */
    ExecManagerResult exec( String cmd );

    /**
    * Execute the specified command and return the result
    */
    ExecManagerResult exec(String cmd, boolean rateLimit, boolean safe, boolean logEnabled);

    /**
     * Execute the specified command and return the stdout
     */
    ExecManagerResult exec( String cmd, boolean rateLimit );

    /**
     * Execute the specified command if safe and return the result
     */
    ExecManagerResult execSafe( String cmd );

    void setLevel( Level level );
    
    /**
     * Execute the specified command and return the exit code
     */
    Integer execResult( String cmd );

    /**
     * Execute the specified command and return the stdout
     */
    String execOutput( String cmd );

    /**
     * Execute the specified command and return the stdout
     */
    String execOutput( boolean logEnabled, String cmd );

    /**
     * Execute the specified command and return the stdout
     */
    String execOutput( String cmd, boolean rateLimit );

    /**
     * Execute the specified command and return the stdout
     */
    String execOutputSafe( String cmd );

    /**
     * Execute the specified command and return the stdout
     */
    String execOutputSafe( String cmd, boolean rateLimit );

    /**
     * Execute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    ExecManagerResultReader execEvil( String cmd[] ) throws IOException;

    /**
     * Execute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    ExecManagerResultReader execEvil( String cmd[], String env[] ) throws IOException;

    /**
     * Execute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    ExecManagerResultReader execEvil( String cmd ) throws IOException;

    /**
     * Excecute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    Process execEvilProcess(String cmd);

    /**
     * Excecute the specified command and return the process
     * Called "execEvil" because it calls fork which can cause issues
     */
    Process execEvilProcess(String cmd[]);
    
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
