/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.IOException;
import java.util.List;
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
    ExecManagerResult execCommand( String cmd, List<String> arguments );

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
     * Fire-and-forget process launch with stdout/stderr redirected to /dev/null
     * and SIGHUP ignored by the child, so the spawned process survives when
     * the JVM exits. Behaviorally equivalent to shell
     * "nohup CMD &gt;/dev/null 2&gt;&amp;1 &amp;" without invoking a shell.
     *
     * Differs from execEvilProcess() which uses Runtime.exec() and leaves the
     * child's stdout/stderr connected as pipes into the JVM.
     *
     * Internally prepends /usr/bin/nohup to the argv so the child inherits
     * SIG_IGN for SIGHUP. Required because the UVM JVM is started under
     * start-stop-daemon --background, which makes it a session leader; when
     * the JVM exits, the kernel sends SIGHUP to every process in its session.
     * Without nohup the spawned child would die on JVM exit, matching the
     * OLD shell "nohup ... &amp;" semantics.
     *
     * @param cmd
     *        argv array (element 0 is the executable, rest are its arguments)
     * @return the spawned Process, or null if launch failed
     */
    Process execEvilProcessDetached(String cmd[]);

    /**
     * Close this instance of exec manager
     * This shuts down the "launcher" process
     * after this this ExecManager can no longer be used
     */
    void close();

      /**
     * Close this instance of exec manager
     * This shuts down the safe "launcher" process
     * after this this ExecManager can no longer be used
     */
    void closeSafe();

    /**
     * Just combines all the arguments into one string and quotes them
     * Also escapes any quote characters in the arguments themselves
     */
    String argBuilder( String[] args );
}
