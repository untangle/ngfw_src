/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */
 
package com.metavize.tran.util;

import java.util.concurrent.TimeoutException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmContextFactory;

/**
 * Wrapper around a simple exec (short-lived) which takes care of timeouts and buffering
 * output.  Does not handle stdin.
 * 
 */
public final class SimpleExec {

  /**
   * The results of a call to {@link SimpleExec#exec exec}
   */
  public static class SimpleExecResult {
    /**
     * The exit code of the process
     */
    public final int exitCode;
    /**
     * The buffered output of the executed processes stdout.  If stdout
     * was not captured this may be null
     */
    public final byte[] stdOut;
    /**
     * The buffered output of the executed processes stderr.  If stderr
     * was not captured this may be null
     */
    public final byte[] stdErr;

    private SimpleExecResult(int exitCode,
      byte[] out,
      byte[] err) {
      this.exitCode = exitCode;
      this.stdOut = out;
      this.stdErr = err;
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
    Logger logAs) {

    
    m_logger = logAs==null?Logger.getLogger(SimpleExec.class):logAs;
    m_maxTime = maxTime;
  }

  /**
   * Callback method for the thread watching for timeouts
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
   * Class which drains a stream to a ByteArrayOutputStream
   */
  private class StreamDrainer
    implements Runnable {
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
    boolean useMVVMThread) throws IOException, TimeoutException {

    //Assemble full command
    args = args==null?EMPTY_ARRAY:args;
    String[] fullCmd = new String[1 + args.length];
    fullCmd[0] = cmd;
    System.arraycopy(args, 0, fullCmd, 1, args.length);

    m_process = Runtime.getRuntime().exec(fullCmd, env, rootDir);

    //If we're here, we created a process
    try {
      m_stdOut = m_process.getInputStream();
      m_stdErr = m_process.getErrorStream();

      if(bufferStdOut) {
        m_stdOutBuf = new ByteArrayOutputStream();
        createThread(new StreamDrainer(m_stdOut, m_stdOutBuf), useMVVMThread).start();
      }
      if(bufferStdErr) {
        m_stdErrBuf = new ByteArrayOutputStream();
        createThread(new StreamDrainer(m_stdErr, m_stdErrBuf), useMVVMThread).start();
      }
      createThread(new Runnable() {public void run() {timeoutWatcher();}}, useMVVMThread).start();

      m_process.waitFor();
      done();
      closeStreams();
      if(m_wasTimeout) {
        throw new TimeoutException();
      }
      int retCode = m_process.exitValue();

      return new SimpleExecResult(retCode, 
        bufferStdOut?m_stdOutBuf.toByteArray():null,
        bufferStdErr?m_stdErrBuf.toByteArray():null);
    }
    catch(InterruptedException ex) {
      done();
      closeStreams();
      destroyProcess();
      if(m_wasTimeout) {
        throw new TimeoutException();
      }
      else {
        throw new IOException("Interrupted");
      }
    }
    
  }

  private void done() {
    synchronized(m_lock) {
      m_done = true;
      m_lock.notify();
    }
  }

  private void closeStreams() {
    try{m_stdOut.close();}catch(Exception ignore){}
    try{m_stdErr.close();}catch(Exception ignore){}
  }

  private void destroyProcess() {
    try {m_process.destroy();}catch(Exception ignore){}
  }

  private Thread createThread(Runnable r, boolean useMVVMThread) {
    return useMVVMThread?MvvmContextFactory.context().newThread(r):new Thread(r);
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
   * @param useMVVMThread if running in MVVM, this is true.  False otherwise.
   */
  public static SimpleExecResult exec(String cmd,
    String[] args,
    String[] env,
    File rootDir,
    boolean bufferStdOut,
    boolean bufferStdErr,
    long maxTime,
    Logger logAs,
    boolean useMVVMThread) throws TimeoutException, IOException {

    SimpleExec se = new SimpleExec(maxTime, logAs);

    return se.doIt(cmd, args, env, rootDir, bufferStdOut, bufferStdErr, useMVVMThread);
  }

/*
  public static void main(String[] args) throws Exception {
    SimpleExecResult result = SimpleExec.exec(
      "openssl",
      new String[] {
        "req",
        "-x509",
        "-nodes",
        "-days",
        "365",
        "-subj",
        "/C=US/ST=CA/L=San Mateo/CN=gobbles.metavize.com",
        "-newkey",
        "rsa:1024"
      },
      null,
      null,
      true,
      true,
      5000,
      null,
      false);

    System.out.println("*****************");
    System.out.println(result.exitCode);
    System.out.println("*****************");
    System.out.println(new String(result.stdOut));
    System.out.println("*****************");
    System.out.println(new String(result.stdErr));

    result = SimpleExec.exec(
      "ls",
      new String[] {"-al"},
      null,
      null,
      true,
      true,
      5000,
      null,
      false);

    System.out.println("*****************");
    System.out.println(result.exitCode);
    System.out.println("*****************");
    System.out.println(new String(result.stdOut));
    System.out.println("*****************");
    System.out.println(new String(result.stdErr));    
    
  }
*/
}

