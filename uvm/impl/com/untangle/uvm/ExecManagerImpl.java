/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.ExecManagerResultReader;

/**
 * ExecManagerImpl is a simple manager for all exec() calls.
 * 
 * It should only be used in the following conditions: - your exec is
 * short-lived (all exec calls a synchronized through a single monitor) - your
 * exec's output is relatively short and can be returned as a string
 * 
 * This class launches a sub-process which does all the fork/exec to avoid JVM
 * forking issues documented here:
 * http://developers.sun.com/solaris/articles/subprocess/subprocess.html
 */
public class ExecManagerImpl implements ExecManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private JSONSerializer serializer = null;

    private Process proc = null;
    private OutputStreamWriter out = null;
    private BufferedReader in = null;

    private Level level;

    private boolean showAllStatistics = false;
    private ConcurrentHashMap<String, ExecManagerStatus> execStatistics = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    protected ExecManagerImpl()
    {
        initDaemon();
        level = Level.INFO;
    }

    /**
     * Override default statistics display control.
     * @param showAllStatistics If true, show all exec stats, otherwise use time limiting.
     */
    public void showAllStatistics(boolean showAllStatistics)
    {
        this.showAllStatistics = showAllStatistics;
    }

    /**
     * Sets the log level we should use
     * 
     * @param level
     *        The log level
     */
    public void setLevel(Level level)
    {
        this.level = level;
    }

    /**
     * Sets the JSON serializer we should use
     * 
     * @param serializer
     *        The serializer
     */
    public void setSerializer(JSONSerializer serializer)
    {
        this.serializer = serializer;
    }

    /**
     * Closes all open objects
     */
    public synchronized void close()
    {
        if (in != null || out != null || proc != null) logger.debug("Shutting down ut-exec-launcher...");
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (proc != null) {
                proc.destroy();
            }
        } catch (Exception ex) {
        }
        in = null;
        out = null;
        proc = null;
    }

    /**
     * Executes a command and returns the result object
     * 
     * @param cmd
     *        The command to execute
     * @return The execution result object
     */
    public synchronized ExecManagerResult exec(String cmd)
    {
        if (in == null | out == null || proc == null) {
            initDaemon();
        }

        cmd = cmd.replace("\n", "");
        cmd = cmd.replace("\r", "");

        // !!! also global flag to show all

        try {
            ExecManagerStatus status = execStatistics.get(cmd);
            if(status == null){
                status = new ExecManagerStatus();
                execStatistics.put(cmd, status);
            }
            boolean showStatus = status.showStatus();
            if(this.showAllStatistics){
                showStatus = true;
            }
            if(showStatus){
                logger.log(this.level, "ExecManager.exec(" + cmd + ")");
            }
            // write the command to the launcher daemon
            out.write(cmd + "\n", 0, cmd.length() + 1);
            out.flush();
            // read the JSON result
            long t0 = System.currentTimeMillis();
            String line = in.readLine();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            ExecManagerResult result = (ExecManagerResult) serializer.fromJSON(line);
            long t1 = System.currentTimeMillis();

            if (result == null) {
                logger.warn("Failed to serialize ExecManagerResult");
                return new ExecManagerResult(-1, "");
            }

            status.update(t0, t1);
            if(showStatus){
                if(status.calls == 1){
                    logger.log(this.level, "ExecManager.exec(" + cmd + ") = " + result.getResult() + " took " + (t1 - t0) + " ms.");
                }else{
                    logger.log(this.level, "ExecManager.exec(" + cmd + ") = " + result.getResult() + " (most recent) avg " + (status.interval/status.calls) + " ms in " + status.calls + " calls.");
                }
                status.clear();
            }

            return result;
        } catch (IOException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        } catch (UnmarshallException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        }
    }

    /**
     * Execute a command and return the exit code
     * 
     * @param cmd
     *        The command to execute
     * @return The exit code
     */
    public Integer execResult(String cmd)
    {
        return exec(cmd).getResult();
    }

    /**
     * Execute a command and return the command output
     * 
     * @param cmd
     *        The command to execute
     * @return The output from the command
     */
    public String execOutput(String cmd)
    {
        return exec(cmd).getOutput();
    }

    /**
     * Execute a command in a new process and return the result object
     * 
     * @param cmd
     *        The command to execute
     * @return The result object
     * @throws IOException
     */
    public ExecManagerResultReader execEvil(String cmd[]) throws IOException
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("ExecManager.execEvil( " + cmdStr + ")");
        }
        try {
            return new ExecManagerResultReader(Runtime.getRuntime().exec(cmd, null, null));
        } catch (IOException exc) {
            String msg = exc.getMessage();
            if (msg.contains("Cannot allocate memory")) {
                logger.error("Virtual memory exhausted in Process.exec()");
                UvmContextImpl.getInstance().fatalError("UvmContextImpl.exec", exc);
                return null;
            } else {
                throw exc;
            }
        }
    }

    /**
     * Execute a command in a new process and return the process
     * 
     * @param cmd
     *        The command list to concatenate and execute
     * @return The process
     */
    public Process execEvilProcess(String cmd[])
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("ExecManager.execEvil( " + cmdStr + ")");
        }
        try {
            return Runtime.getRuntime().exec(cmd, null, null);
        } catch (IOException e) {
            logger.warn("exec error:", e);
            return null;
        }
    }

    /**
     * Execute a command in a new process and return the process
     * 
     * @param cmd
     *        The command to execute
     * @return The process
     */
    public Process execEvilProcess(String cmd)
    {
        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = st.nextToken();
        }

        return execEvilProcess(cmdArray);
    }

    /**
     * Execute a command in a new process and return the result object
     * 
     * @param cmd
     *        The command to execute
     * @return The result object
     * @throws IOException
     */
    public ExecManagerResultReader execEvil(String cmd) throws IOException
    {
        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = st.nextToken();
        }

        return execEvil(cmdArray);
    }

    /**
     * Creates a single string from a list of strings
     * 
     * @param args
     *        The string array
     * @return The resulting string
     */
    public String argBuilder(String[] args)
    {
        String argStr = "";

        for (String arg : args) {
            argStr += " \"" + arg.replaceAll("\"", "\\\"") + "\" ";
        }

        return argStr;
    }

    /**
     * Initialize our input, output, and process objects we use to do our thing
     */
    private void initDaemon()
    {
        close();

        String launcher = System.getProperty("uvm.bin.dir") + "/ut-exec-launcher";

        try {
            logger.debug("Launching ut-exec-launcher: " + launcher);
            proc = Runtime.getRuntime().exec(launcher);
        } catch (IOException e) {
            logger.error("Couldn't start ut-exec-launcher", e);
            return;
        }

        out = new OutputStreamWriter(proc.getOutputStream());
        in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    }

    /**
     * Hold information about exec timing and calls.
     */
    private class ExecManagerStatus {
        static final private int MINIUM_LAST_RUN_INTERVAL = 60 * 1000;
        public long firstRun = 0;
        public long lastRun = 0;
        public long interval = 0;
        public int calls = 0;

        /**
         * Update with new statistics
         * @param start Start time in ms.
         * @param stop Stop time in ms.
         */
        public void update(long start, long stop)
        {
            this.calls++;
            this.lastRun = stop;
            this.interval += (stop - start);
            if( (this.firstRun + MINIUM_LAST_RUN_INTERVAL ) < stop){
                this.firstRun = stop;
            }
        }

        /**
         * Clear all values.
         */
        public void clear(){
            this.calls = 0;
            this.interval = 0;
        }

        /**
         * Determine if should show status
         * @return true if lastRun is greater than minimum display interval.
         */
        public boolean showStatus()
        {
            if (this.firstRun == 0){
                return true;
            }
            return ( firstRun + MINIUM_LAST_RUN_INTERVAL) < System.currentTimeMillis();
        }
    }
}
