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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import org.json.JSONException;

import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.util.ObjectMatcher;

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
    private final Logger logger = LogManager.getLogger(getClass());

    private JSONSerializer serializer = null;

    private Process proc = null;
    private OutputStreamWriter out = null;
    private BufferedReader in = null;
    private Process procSafe = null;
    private OutputStreamWriter outSafe = null;
    private BufferedReader inSafe = null;

    private Level level;

    private boolean showAllStatistics = true;
    private ConcurrentHashMap<String, ExecManagerStatus> execStatistics = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    protected ExecManagerImpl()
    {
        initDaemon();
        initSafeDaemon();
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
                logger.debug("UnSafe launcher stopped, pid=" + proc.pid());
            }
        } catch (Exception ex) {
        }
        in = null;
        out = null;
        proc = null;
    }


    /**
     * Closes all open objects
     */
    public synchronized void closeSafe()
    {
        if (inSafe != null || outSafe != null || procSafe != null) logger.debug("Shutting down ut-exec-safe-launcher...");
        try {
            if (outSafe != null) {
                outSafe.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (inSafe != null) {
                inSafe.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (procSafe != null) {
                procSafe.destroy();
                logger.debug("Safe launcher stopped, pid=" + procSafe.pid());
            }
        } catch (Exception ex) {
        }
        inSafe = null;
        outSafe = null;
        procSafe = null;
    }

    /**
     * Executes a command and returns the result object
     * 
     * @param cmd
     *        The String command to execute and optionally the rate limit flag
     * @return The execution result object
     */
    public synchronized ExecManagerResult exec(String cmd)
    {
        return exec(cmd, false, false, true);
    }

    /**
     * Executes a command and returns the result object
     * 
     * @param cmd
     *        The String command to execute and optionally the rate limit flag
     * @param rateLimit
     *        A boolean controlling if the output should be rate controlled or not
     * @return The execution result object
     */
    public synchronized ExecManagerResult exec(String cmd, boolean rateLimit)
    {
        return exec(cmd, rateLimit, false, true);
    }

    /**
     * Executes only safe command and returns the result object
     *
     * @param cmd
     *        The String command to execute and optionally the rate limit flag
     * @return The execution result object
     */
    public synchronized ExecManagerResult execSafe(String cmd)
    {
        return exec(cmd, false, true, true);
    }

    /**
     * Executes a command and returns the result object
     * 
     * @param cmd
     *        The String command to execute and optionally the rate limit flag
     * @param rateLimit
     *        A boolean controlling if the output should be rate controlled or not
     * @param safe
     *        If true, prevent call if suspicious characters found. Otherwise, if found just note.
     * @param logEnabled
     *        In case of password encryption we don't log command
     * @return The execution result object
     */
    public synchronized ExecManagerResult exec(String cmd, boolean rateLimit, boolean safe, boolean logEnabled)
    {

        if (in == null | out == null || proc == null) {
            initDaemon();
        }

        cmd = cmd.replace("\n", "");
        cmd = cmd.replace("\r", "");

        if (cmd.contains(";") || cmd.contains("&&") || cmd.contains("|") || cmd.contains(">") || cmd.contains("$(")) {
            if(safe){
                logger.log(this.level, "Suspicious command (" + cmd + "), blocked");
                return new ExecManagerResult();
            }else{
                logger.log(this.level, "Suspicious command (" + cmd + "), allowing");
            }
        }

        try {
            ExecManagerStatus status = execStatistics.get(cmd);
            if(status == null){
                status = new ExecManagerStatus();
                execStatistics.put(cmd, status);
            }
            boolean showStatus = status.showStatus(rateLimit);
            if(showStatus){
                if(logEnabled)
                    logger.log(this.level, "ExecManager.exec(" + cmd + ")");
            }
            // write the command to the launcher daemon
            out.write(cmd + "\n", 0, cmd.length() + 1);
            out.flush();
            // read the JSON result
            long t0 = System.currentTimeMillis();
            String line = in.readLine();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            ExecManagerResult result  = ObjectMatcher.parseJson(line, ExecManagerResult.class); 
            long t1 = System.currentTimeMillis();

            if (result == null) {
                logger.warn("Failed to serialize ExecManagerResult");
                return new ExecManagerResult(-1, "");
            }

            status.update(t0, t1);
            if(showStatus){
                if(status.calls == 1){
                    if(logEnabled)
                        logger.log(this.level, "ExecManager.exec(" + cmd + ") = " + result.getResult() + " took " + (t1 - t0) + " ms.");
                }else{
                    if(logEnabled)
                        logger.log(this.level, "ExecManager.exec(" + cmd + ") = " + result.getResult() + " (most recent) avg " + (status.interval/status.calls) + " ms in " + status.calls + " calls.");
                }
                status.clear();
            }

            return result;
        } catch (IOException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        } catch (JSONException | UnmarshallException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        }
    }



     /**
     * Executes a command and returns the result object
     * 
     * @param cmd
     *        The String command to execute
     * @param arguments
     *        The Command argument as String
     * @return The execution result object
     */
    public synchronized ExecManagerResult execCommand( String cmd , List<String> arguments) {
        
        
        if (inSafe == null || outSafe == null || procSafe == null) {
            initSafeDaemon();
        }

        try {

            // Build structured JSON manually (no command string)
            Map<String, Object> payload = new HashMap<>();
            payload.put("executable", cmd);
            payload.put("args", arguments);

            String json;
            try {
                json = serializer.toJSON(payload).toString();
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize exec request", e);
            }


            // Send JSON to launcher daemon
            outSafe.write(json);
            outSafe.write("\n");
            outSafe.flush();

            long t0 = System.currentTimeMillis();
            String line = inSafe.readLine();
            long t1 = System.currentTimeMillis();

            ExecManagerResult result =
                    ObjectMatcher.parseJson(line, ExecManagerResult.class);

            if (result == null) {
                logger.warn("Failed to parse ExecManagerResult");
                return new ExecManagerResult(-1, "");
            } else {
                logger.log(this.level, "ExecManager.execCommand(" + cmd + ") = " + result.getResult() + " took " + (t1 - t0) + " ms.");
            }

            return result;

        } catch (IOException exn) {
            logger.warn("Exception during ut-exec-safe-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        } catch (JSONException | UnmarshallException exn) {
            logger.warn("Exception during ut-exec-safe-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1, exn.toString());
        }   catch (Exception ex) {
            // command failed, launcher still healthy
            logger.warn("Command execution failedfor ut-exec-safe-launcher", ex);
            return new ExecManagerResult(-1, ex.toString());
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
     *        The String command to execute and optionally the rateLimit flag
     * @return The output from the command
     */
    public String execOutput(String cmd)
    {
        return exec(cmd, false, false, true).getOutput();
    }

     /**
     * Execute a command and return the command output
     * 
     * @param logEnabled
     *        The String command to execute and optionally the rateLimit flag
     * @param cmd
     *        
     * @return The output from the command
     */
    public String execOutput(boolean logEnabled, String cmd)
    {
        return exec(cmd, false, false, logEnabled).getOutput();
    }


    /**
     * Execute a command and return the command output
     * 
     * @param cmd
     *        The String command to execute and optionally the rateLimit flag
     * @param rateLimit
     *        If this request should be rate limited or not
     * @return The output from the command
     */
    public String execOutput(String cmd, boolean rateLimit)
    {

           return exec(cmd, rateLimit, false,true).getOutput();
    }

    /**
     * Execute a command and return the command output
     * 
     * @param cmd
     *        The String command to execute and optionally the rateLimit flag
     * @return The output from the command
     */
    public String execOutputSafe(String cmd)
    {
        return exec(cmd, false, true, true).getOutput();
    }


    /**
     * Execute a command and return the command output
     * 
     * @param cmd
     *        The String command to execute and optionally the rateLimit flag
     * @param rateLimit
     *        If this request should be rate limited or not
     * @return The output from the command
     */
    public String execOutputSafe(String cmd, boolean rateLimit)
    {

           return exec(cmd, rateLimit, true, true).getOutput();
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
        return execEvil(cmd, null);
    }


    /**
     * Execute a command in a new process and return the result object
     *
     * @param cmd
     *        The command to execute
     * @param env
     *        List of environment variables
     * @return The result object
     * @throws IOException
     */
    public ExecManagerResultReader execEvil(String cmd[], String env[]) throws IOException
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("ExecManager.execEvil( " + cmdStr + ")");
        }
        try {
            return new ExecManagerResultReader(Runtime.getRuntime().exec(cmd, env, null));
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
            logger.info("UnSafe launcher started, pid=" + proc.pid());
        } catch (IOException e) {
            logger.error("Couldn't start ut-exec-launcher", e);
            return;
        }

        out = new OutputStreamWriter(proc.getOutputStream());
        in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    }


    /**
     * Initialize our safe input, output, and process objects we use to do our thing
     */
    private void initSafeDaemon()
    {
        if (procSafe != null && procSafe.isAlive()) {
            return;
        }
        
        closeSafe();

        String launcher = System.getProperty("uvm.bin.dir") + "/ut-exec-safe-launcher";

        try {
            logger.debug("Launching ut-exec-safe-launcher: " + launcher);
            procSafe = Runtime.getRuntime().exec(launcher);
            logger.debug("Safe launcher started, pid=" + procSafe.pid());
        } catch (IOException e) {
            logger.error("Couldn't start ut-exec-safe-launcher", e);
            return;
        }

        outSafe = new OutputStreamWriter(procSafe.getOutputStream());
        inSafe = new BufferedReader(new InputStreamReader(procSafe.getInputStream()));
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
         * @param rateLimit if rateLimit should be done, always return true
         * @return true if lastRun is greater than minimum display interval.
         */
        public boolean showStatus(boolean rateLimit)
        {
            if (this.firstRun == 0 || !rateLimit) {
                return true;
            }
            return ( firstRun + MINIUM_LAST_RUN_INTERVAL) < System.currentTimeMillis();
        }
    }
}
