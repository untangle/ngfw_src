/**
 * $Id: ExecManagerImpl.java,v 1.00 2012/01/31 22:27:44 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.ExecResultReader;

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

    protected ExecManagerImpl()
    { 
        initDaemon();
    }

    public void setSerializer(JSONSerializer serializer)
    {
        this.serializer = serializer;
    }

    public synchronized void close()
    {
        if (in != null || out != null || proc != null)
            logger.info("Shutting down ut-exec-launcher...");
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
            logger.info("ExecManager.exec(" + cmd + ")");
            // write the command to the launcher daemon
            out.write(cmd + "\n", 0, cmd.length() + 1);
            out.flush();
            // read the JSON result
            long t0 = System.currentTimeMillis();
            String line = in.readLine();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            ExecManagerResult result = (ExecManagerResult) serializer.fromJSON(line);
            long t1 = System.currentTimeMillis();

            logger.info("ExecManager.exec(" + cmd + ") = " + result.getResult() + " took " + (t1-t0) + " ms.");

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

    public ExecResultReader execEvil(String cmd[]) throws IOException
    {
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0 ; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("ExecManager.execEvil(" + cmdStr + ")");
        }
        try {
            return new ExecResultReaderImpl(Runtime.getRuntime().exec(cmd, null, null));
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

    public ExecResultReader execEvil(String cmd) throws IOException
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
            logger.info("Launching ut-exec-launcher: " + launcher);
            proc = Runtime.getRuntime().exec(launcher);
        } catch (IOException e) {
            logger.error("Couldn't start ut-exec-launcher", e);
            return;
        }

        out = new OutputStreamWriter(proc.getOutputStream());
        in  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    }
}
