/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.util;

import java.io.*;

/**
 * Useful IO routines.
 */
public class IOUtil {

  private static final int DEF_BUF_SZ = 1024*4;


  /**
   * Copy a file (a "real" file, not directory).  Details on
   * the "rules" as follows:
   * <br><br>
   * <ul>
   *   <li>
   *     If <code>source</code> or <code>dest</code> denote
   *     directories, an exception will be thrown
   *   </li>
   *   <li>
   *     If <code>dest</code> does not exist ant its parent
   *     directory(ies) do not exist, then the parent
   *     directories are implicitly created.  If this operation
   *     fails, an exception is thrown.  If the parent directories
   *     are implicitly created yet the copy fails, the newly
   *     created parents are <b>not</b> deleted (enhancement)
   *   </li>     
   *   <li>
   *     If <code>source</code> does not exist, the
   *     method returns without error.
   *   </li>
   *   <li>
   *     If <code>source</code> is not readable, an exception
   *     is thrown.
   *   </li>
   *   <li>
   *     If <code>dest</code> exists and is not writable, an exception
   *     is thrown.
   *   </li>
   *   <li>
   *     If <code>dest</code> does not exists yet its parent
   *     directory is not writable, an exception is thrown.
   *   </li>    
   *   <li>
   *     If <code>dest</code> exists, it will
   *     be overwritten unless it is not writable
   *     in which case an exception will be thrown.  If there
   *     is an error in overwritting the file, the original
   *     is clobbered and left in an indeterminate state
   *     (sorry - another enhancement).
   *   </li>   
   * </ul>
   *
   * Note that if an exception is thrown, no open streams remain.
   *
   * @param source the source file
   * @param dest the destination file
   *
   * @exception IOException if something goes wrong from the file system
   */
  public static void copyFile(File source,
    File dest) throws IOException {

    //Check for blank source
    if(!source.exists()) {
      return;
    }

    //Readable
    if(!source.canRead()) {
      throw new IOException("No read permission for file \"" +
        source + "\"");
    }
    //Check for dirs
    if(source.isDirectory()) {
      throw new IOException("Source file \"" + source +
        " is a directory");
    }
    if(dest.isDirectory()) {
      throw new IOException("Destination file \"" + dest +
        " is a directory");
    }

    //Writable
    if(dest.exists() && !dest.canWrite()) {
      throw new IOException("No write permission for file \"" +
        dest + "\"");
    }     

    //Create target parent, if required
    if(!dest.exists() && !dest.getParentFile().exists()) {
      if(!dest.getParentFile().mkdirs()) {
        throw new IOException("Unable to implicitly create parent " +
          "directory for destination \"" +
          dest.getParentFile() + "\"");
      }
    }

    FileInputStream fIn = null;
    FileOutputStream fOut = null;
    try {
      fIn = new FileInputStream(source);
      fOut = new FileOutputStream(dest);
      pipe(fIn, fOut);
      fOut.flush();
      close(fIn);
      close(fOut);
    }
    catch(Exception ex) {
      close(fIn);
      close(fOut);
      IOException newEx = new IOException("Unable to copy file");
      newEx.initCause(ex);
      throw newEx;
    }
  }

  /**
   * Remove a directory and any children.
   * <br><br>
   * <b>Warning - This is the same as "rm -rf"</b>.  Make
   * sure you really want stuff nuked.
   *
   * @param dir the dir to be nuked
   *
   * @return true if <code>dir</code> was deleted (or it
   *         did not exist).  False if <code>dir</code>
   *         is not a directory or it could not be deleted.
   */
  public static boolean rmDir(File dir) {
    //TODO bscott it is likely much faster
    //on Unix just to exec "rm -rf".
    if(!dir.exists()) {
      return true;
    }
    if(!dir.isDirectory()) {
      return false;
    }
    for(File kid : dir.listFiles()) {
      if(kid.isDirectory()) {
        if(!rmDir(kid)) {
          return false;
        }
      }
      else {
        if(!kid.delete()) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  public static long pipe(final InputStream in,
    final OutputStream out) throws IOException {

    return pipe(in, out, new byte[DEF_BUF_SZ], Long.MAX_VALUE);
  }  

  public static long pipe(final InputStream in,
    final OutputStream out,
    final long maxTransfer) throws IOException {

    return pipe(in, out, new byte[DEF_BUF_SZ], maxTransfer);
  }  

  public static long pipe(final InputStream in,
    final OutputStream out,
    final byte[] transferBuf) throws IOException {

    return pipe(in, out, transferBuf, Long.MAX_VALUE);
  }  

  /**
   * Pipe the contents of <code>in</code> to <code>out</code>
   *
   * @param in the source
   * @param out the destination
   * @param transferBuf a pre-allocated byte buffer used
   *        to perform transfer
   * @param maxTransfer the maximum number of bytes to transfer
   *
   * @return the actual number of bytes transfered
   *
   * @exception IOException from the underlying streams.
   */
  public static long pipe(final InputStream in,
    final OutputStream out,
    final byte[] transferBuf,
    final long maxTransfer) throws IOException {

    final int bufLen = transferBuf.length;
    long ret = 0;

    while(ret < maxTransfer) {
      long amtToRead = maxTransfer - ret;
      if(amtToRead > bufLen) {
        amtToRead = bufLen;
      }
      int read = in.read(transferBuf, 0, (int) amtToRead);
      if(read == -1) {
        break;
      }
      out.write(transferBuf, 0, read);
      ret+=read;
    }
    return ret;
  }

  /**
   * Safe close of an OutputStream (no exceptions,
   * even if reference is null).
   *
   * @param out the stream to close
   */
  public static void close(OutputStream out) {
    try {out.close();}
    catch(Exception ignore){}
  }

  /**
   * Safe close of an InputStream (no exceptions,
   * even if reference is null).
   *
   * @param in the stream to close
   */  
  public static void close(InputStream in) {
    try {in.close();}
    catch(Exception ignore){}
  }

  /**
   * Delete the file.  Throws no exception (even
   * if file is null).
   *
   * @param f the file (may be null).
   *
   * @return the outcome of <code>f.close</code>,
   *         or true if <code>f</code> is null.
   */ 
  public static boolean delete(File f) {
    if(f == null) {
      return true;
    }
    return f.delete();
  }  

}