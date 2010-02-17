/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;

/**
 * Wrapper around a simple exec (short-lived) which takes care of
 * timeouts and buffering output.  Does not handle stdin.
 */
public final class SimpleExec
{
    /**
     * The results of a call to {@link SimpleExec#exec exec}.
     */
    public static class SimpleExecResult
    {
        /**
         * The exit code of the process.
         */
        public final int exitCode;

        /**
         * The buffered output of the executed processes stdout.  If
         * stdout was not captured this may be null.
         */
        public final byte[] stdOut;

        /**
         * The buffered output of the executed processes stderr.  If
         * stderr was not captured this may be null.
         */
        public final byte[] stdErr;

        private SimpleExecResult(int exitCode,
                                 byte[] out,
                                 byte[] err) {
            this.exitCode = exitCode;
            this.stdOut = out;
            this.stdErr = err;
        }

        public String toString() {
            String newLine = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder();
            sb.append("Return Code: " + exitCode).append(newLine);
            if(stdOut == null) {
                sb.append("(no stdout)").append(newLine);
            }
            else {
                sb.append("----------- BEGIN stdout -----------").append(newLine);
                sb.append(new String(stdOut)).append(newLine);
                sb.append("----------- BEGIN stdout -----------").append(newLine);
            }
            if(stdErr == null) {
                sb.append("(no stderr)").append(newLine);
            }
            else {
                sb.append("----------- BEGIN stderr -----------").append(newLine);
                sb.append(new String(stdErr)).append(newLine);
                sb.append("----------- BEGIN stderr -----------").append(newLine);
            }
            return sb.toString();
        }
    }


    private static final String[] EMPTY_ARRAY = new String[0];
    private final Logger m_logger;
    private final long m_maxTime;
    private volatile boolean m_wasTimeout = false;
    private volatile boolean m_done = false;
    private final Object m_lock = new Object();

    private ByteArrayOutputStream m_stdOutBuf;
    private ByteArrayOutputStream m_stdErrBuf;
    private Process m_process;
    private InputStream m_stdOut;
    private InputStream m_stdErr;

    private SimpleExec(long maxTime,
                       Logger logAs)
    {


        m_logger = logAs==null?Logger.getLogger(SimpleExec.class):logAs;
        m_maxTime = maxTime;
    }

    /**
     * Callback method for the thread watching for timeouts.
     */
    private void timeoutWatcher() {
        long then = System.currentTimeMillis() + m_maxTime;
        try {
            synchronized(m_lock) {
                while((System.currentTimeMillis() < then) && ! m_done) {
                    long sleep = then - System.currentTimeMillis();
                    m_lock.wait(sleep>0?sleep:1);
                }
            }
            if(!m_done) {
                destroyProcess();
                m_wasTimeout = true;
            }
        }
        catch(Exception ignore) {}
    }


    /**
     * Class which drains a stream to a ByteArrayOutputStream.
     */
    private class StreamDrainer
        implements Runnable
    {
        private final InputStream m_in;
        private final ByteArrayOutputStream m_out;

        StreamDrainer(InputStream in,
                      ByteArrayOutputStream out) {
            m_in = in;
            m_out = out;
        }

        public void run() {
            try {
                int read = 0;
                byte[] buf = new byte[1024];
                while(((read = m_in.read(buf)) != -1) && !m_done) {
                    m_out.write(buf, 0, read);
                }
            }
            catch(Exception ex) {
            }
        }
    }

    private SimpleExecResult doIt(String cmd,
                                  String[] args,
                                  String[] env,
                                  File rootDir,
                                  boolean bufferStdOut,
                                  boolean bufferStdErr,
                                  boolean useUVMThread) throws IOException
    {
        //Assemble full command
        args = args==null?EMPTY_ARRAY:args;
        String[] fullCmd = new String[1 + args.length];
        fullCmd[0] = cmd;
        System.arraycopy(args, 0, fullCmd, 1, args.length);

        m_process = useUVMThread ? LocalUvmContextFactory.context().exec(fullCmd, env, rootDir) : Runtime.getRuntime().exec(fullCmd, env, rootDir);

        //If we're here, we created a process
        try {
            m_stdOut = m_process.getInputStream();
            m_stdErr = m_process.getErrorStream();

            if(bufferStdOut) {
                m_stdOutBuf = new ByteArrayOutputStream();
                createThread(new StreamDrainer(m_stdOut, m_stdOutBuf), useUVMThread).start();
            }
            if(bufferStdErr) {
                m_stdErrBuf = new ByteArrayOutputStream();
                createThread(new StreamDrainer(m_stdErr, m_stdErrBuf), useUVMThread).start();
            }
            createThread(new Runnable() {public void run() {timeoutWatcher();}}, useUVMThread).start();

            m_process.waitFor();
            done();
            closeStreams();
            if(m_wasTimeout) {
                String stdOutStr = bufferStdOut?new String(m_stdOutBuf.toByteArray()):"";
                String stdErrStr = bufferStdErr?new String(m_stdErrBuf.toByteArray()):"";
                throw new IOException("Captured stdout \"" + stdOutStr +
                                      "\", stderr \"" + stdErrStr + "\"");
            }
            int retCode = m_process.exitValue();

            return new SimpleExecResult(retCode,
                                        bufferStdOut?m_stdOutBuf.toByteArray():null,
                                        bufferStdErr?m_stdErrBuf.toByteArray():null);
        } catch (InterruptedException ex) {
            done();
            closeStreams();
            destroyProcess();
            if(m_wasTimeout) {
                String stdOutStr = bufferStdOut?new String(m_stdOutBuf.toByteArray()):"";
                String stdErrStr = bufferStdErr?new String(m_stdErrBuf.toByteArray()):"";
                throw new IOException("Captured stdout \"" + stdOutStr +
                                      "\", stderr \"" + stdErrStr + "\"");
            }
            else {
                throw new IOException("Interrupted");
            }
        }
    }

    private void done()
    {
        synchronized (m_lock) {
            m_done = true;
            m_lock.notify();
        }
    }

    private void closeStreams()
    {
        try{m_stdOut.close();}catch(Exception ignore){}
        try{m_stdErr.close();}catch(Exception ignore){}
    }

    private void destroyProcess()
    {
        try {m_process.destroy();}catch(Exception ignore){}
    }

    private Thread createThread(Runnable r, boolean useUVMThread) {
        return useUVMThread?LocalUvmContextFactory.context().newThread(r):new Thread(r);
    }

    /**
     * Execute the given command.
     *
     * @param cmd the command (e.g. "ls")
     * @param args arguments (e.g. "-al").  May be null
     * @param env the environment (in "name=value" form).  May be null
     * @param rootDir the directory to run-in.  If null, the $PWD is used
     * @param bufferStdOut If true, stdout of the process will be buffered
     *        and available on the returned {@link #SimpleExecResult}
     * @param bufferStdErr If true, stderr of the process will be buffered
     *        and available on the returned {@link #SimpleExecResult}
     * @param maxTime the max time the process should run before it is killed
     */
    public static SimpleExecResult exec(String cmd,
                                        String[] args,
                                        String[] env,
                                        File rootDir,
                                        boolean bufferStdOut,
                                        boolean bufferStdErr,
                                        long maxTime) throws IOException {
        return exec(cmd,
                    args,
                    env,
                    rootDir,
                    bufferStdOut,
                    bufferStdErr,
                    maxTime,
                    null,
                    false);
    }

    /**
     * Execute the given command.
     *
     * @param cmd the command (e.g. "ls")
     * @param args arguments (e.g. "-al").  May be null
     * @param env the environment (in "name=value" form).  May be null
     * @param rootDir the directory to run-in.  If null, the $PWD is used
     * @param bufferStdOut If true, stdout of the process will be buffered
     *        and available on the returned {@link #SimpleExecResult}
     * @param bufferStdErr If true, stderr of the process will be buffered
     *        and available on the returned {@link #SimpleExecResult}
     * @param maxTime the max time the process should run before it is killed
     * @param logAs a log instance, so log messages may be made from a more
     *        suitable context.
     * @param useUVMThread if running in UVM, this is true.  False otherwise.
     */
    public static SimpleExecResult exec(String cmd,
                                        String[] args,
                                        String[] env,
                                        File rootDir,
                                        boolean bufferStdOut,
                                        boolean bufferStdErr,
                                        long maxTime,
                                        Logger logAs,
                                        boolean useUVMThread) throws IOException {

        SimpleExec se = new SimpleExec(maxTime, logAs);

        return se.doIt(cmd, args, env, rootDir, bufferStdOut, bufferStdErr, useUVMThread);
    }
}

