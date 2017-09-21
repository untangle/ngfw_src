/**
 * $Id$
 */
package com.untangle.uvm;

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
 * It should only be used in the following conditions:
 * - your exec is short-lived (all exec calls a synchronized through a single monitor)
 * - your exec's output is relatively short and can be returned as a string
 *
 * This class launches a sub-process which does all the fork/exec to avoid JVM forking issues
 * documented here: http://developers.sun.com/solaris/articles/subprocess/subprocess.html 
 */
public class ExecManagerImpl implements ExecManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private JSONSerializer serializer = null;

    private Process       proc = null;
    private OutputStreamWriter out = null;
    private BufferedReader in  = null;

    private Level level;
    
    protected ExecManagerImpl()
    {
        initDaemon();
        level = Level.INFO;
    }

    public void setLevel( Level level )
    {
        this.level = level;
    }

    public void setSerializer(JSONSerializer serializer)
    {
        this.serializer = serializer;
    }

    public synchronized void close()
    {
        if (in != null || out != null || proc != null)
            logger.debug("Shutting down ut-exec-launcher...");
        try { in.close(); } catch (Exception ex) { }
        try { out.close(); } catch (Exception ex) { }
        try { proc.destroy(); } catch (Exception ex) { }
        in = null;
        out = null;
        proc = null;
    }

    public synchronized ExecManagerResult exec(String cmd)
    {
        if (in == null | out == null || proc == null) {
            initDaemon();
        }

        cmd.replace('\n',' ');
        
        try {
            logger.log( this.level, "ExecManager.exec(" + cmd + ")" );
            // write the command to the launcher daemon
            out.write(cmd + "\n", 0, cmd.length() + 1);
            out.flush();
            // read the JSON result
            long t0 = System.currentTimeMillis();
            String line = in.readLine();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            ExecManagerResult result = (ExecManagerResult) serializer.fromJSON(line);
            long t1 = System.currentTimeMillis();

            logger.log( this.level, "ExecManager.exec(" + cmd + ") = " + result.getResult() + " took " + (t1-t0) + " ms.");

            if (result == null) {
                logger.warn("Failed to serialize ExecManagerResult");
                return new ExecManagerResult(-1,"");
            }
            
            return result;
        } catch (IOException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1,exn.toString());
        } catch (UnmarshallException exn) {
            logger.warn("Exception during ut-exec-launcher", exn);
            initDaemon();
            return new ExecManagerResult(-1,exn.toString());
        }
    }

    public Integer execResult(String cmd)
    {
        return exec(cmd).getResult();
    }

    public String execOutput(String cmd)
    {
        return exec(cmd).getOutput();
    }

    public ExecManagerResultReader execEvil(String cmd[]) throws IOException
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0 ; i < cmd.length; i++) {
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

    public Process execEvilProcess(String cmd[])
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0 ; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("ExecManager.execEvil( " + cmdStr + ")");
        }
        try {
            return Runtime.getRuntime().exec(cmd, null, null);
        } catch (IOException e) {
            logger.warn("exec error:",e);
            return null;
        }
    }

    public Process execEvilProcess(String cmd)
    {
        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = st.nextToken();
        }

        return execEvilProcess(cmdArray);
    }
    
    public ExecManagerResultReader execEvil(String cmd) throws IOException
    {
        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = st.nextToken();
        }

        return execEvil(cmdArray);
    }

    public String argBuilder(String[] args)
    {
        String argStr = "";

        for( String arg : args ) {
            argStr += " \"" + arg.replaceAll("\"", "\\\"") + "\" ";
        }

        return argStr;
    }
    
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
        in  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    }
}
