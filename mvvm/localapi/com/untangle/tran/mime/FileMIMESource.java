/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mime;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import com.untangle.tran.util.*;

/**
 * MIMESource implementation which wrapps a file.
 */
public class FileMIMESource
  implements MIMESource {

  private File m_file;
  private final boolean m_deleteFileOnClose;
  private MIMEParsingInputStream m_cachedStream;
  private Set<MIMEParsingInputStream> m_openStreams;//Cached is always in OpenStreams


  /**
   * Construct a new FileMIMESource against the
   * given file which <b>will</b> delete the file
   * when {@link #close closed}.
   *
   * @param file the file
   */
  public FileMIMESource(File file) {
    this(file, true);
  }

  /**
   * Construct a new FileMIMESource against the
   * given file.
   *
   * @param file the file
   * @param deleteFileOnClose if true, the file
   *        will be implicitly deleted when this
   *        {@link #close source is closed}
   */
  public FileMIMESource(File file, boolean deleteFileOnClose) {
    m_file = file;
    m_deleteFileOnClose = deleteFileOnClose;
    m_openStreams = new HashSet<MIMEParsingInputStream>();
  }


  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public MIMEParsingInputStream getInputStream()
    throws IOException {
    return getInputStream(0);
  }

  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public MIMEParsingInputStream getInputStream(long offset)
    throws IOException {

    MIMEParsingInputStream ret = null;

    if(m_cachedStream != null) {
      if(m_cachedStream.position() <= offset) {
        ret = m_cachedStream;
      }
      else {
        destroyStream(m_cachedStream);
        ret = createBaseStream();
      }
      m_cachedStream = null;//Make sure we do not give this out twice
    }
    else {
      ret = createBaseStream();
    }

    //Now, advance the stream
    try {
      long diff = offset - ret.position();
      while(diff > 0) {
        diff-=ret.skip(diff);
      }
    }
    catch(IOException ex) {
      destroyStream(ret);
      IOException ex2 = new IOException("Unable to advance stream");
      ex2.initCause(ex);
      throw ex2;
    }

    return new WrappedMIMEParsingInputStream(ret);
  }

  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public void close() {
    //Bug 779 - Copy contents of open streams before
    //          attempting to iterate, as iteration
    //          removes from the list
    MIMEParsingInputStream[] openStreams =
      (MIMEParsingInputStream[]) m_openStreams.toArray(
        new MIMEParsingInputStream[m_openStreams.size()]);

    for(MIMEParsingInputStream s : openStreams) {
      destroyStream(s);
    }

    destroyStream(m_cachedStream);//Should be redundant
    m_cachedStream = null;
    if(m_deleteFileOnClose) {
      try {m_file.delete();}catch(Exception ignore){}
      m_file = null;
    }
  }

  /**
   * Get the underlying file.  Note that this file
   * <b>may be deleted</b> when this source is closed,
   * so if you want the file to exist for longer
   * you should copy the file.
   */
  public File getFile() {
    return m_file;
  }

  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public File toFile(FileFactory factory) throws IOException {
    return getFile();
  }

  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public File toFile(FileFactory factory, String name) throws IOException {
    return getFile();
  }

  private MIMEParsingInputStream createBaseStream()
    throws IOException {
    MIMEParsingInputStream ret = new MIMEParsingInputStream(
      new BufferedInputStream(
        new FileInputStream(m_file)));
    m_openStreams.add(ret);
    return ret;
  }

  private void destroyStream(MIMEParsingInputStream stream) {
    if(stream == null) {return;}
    try {stream.close();}catch(Exception ignore){}
    try {m_openStreams.remove(stream);}catch(Exception ignore){}
  }

  private void returnStream(MIMEParsingInputStream stream) {
    if(m_cachedStream != null) {
      //Test if we should replace the cached stream.  We do
      //this if the new stream is at a lower position (more
      //likely to be useful).
      if(m_cachedStream.position() < stream.position()) {
        destroyStream(stream);
      }
      else {
        destroyStream(m_cachedStream);
        m_cachedStream = stream;
      }
    }
    else {
      m_cachedStream = stream;
    }
  }

  /**
    * Used since we're subclasses (rather than implementing
    * an interface) and needed a dummy stream.
    */
  private class NOOPInputStream extends InputStream {
    public int read() {
      return -1;
    }
  }

  /**
   * Wrapper which lets us keep track of
   * underlying open streams.
   */
  private class WrappedMIMEParsingInputStream
    extends MIMEParsingInputStream {

    private final MIMEParsingInputStream m_wrap;
    private boolean m_closed = false;

    public WrappedMIMEParsingInputStream(MIMEParsingInputStream wrap) {
      super(new NOOPInputStream());
      m_wrap = wrap;
    }

    @Override
    public long position() {
      checkClosedRE();
      return m_wrap.position();
    }

    @Override
    public void unread(int b)
      throws IOException {
      checkClosed();
      m_wrap.unread(b);
    }

    @Override
    public void unread(byte[] b)
      throws IOException {
      checkClosed();
      m_wrap.unread(b);
    }

    @Override
    public void unread(byte[] b, int off, int len)
      throws IOException {
      checkClosed();
      m_wrap.unread(b, off, len);
    }

    @Override
    public int read()
      throws IOException {
      checkClosed();
      return m_wrap.read();
    }

    @Override
    public int read(byte[] b)
      throws IOException {
      checkClosed();
      return m_wrap.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len)
      throws IOException {
      checkClosed();
      return m_wrap.read(b, off, len);
    }

    @Override
    public Line readLine(int maxLen)
      throws IOException, LineTooLongException {
      checkClosed();
      return m_wrap.readLine(maxLen);
    }

    @Override
    public Line readLine()
      throws IOException, LineTooLongException {
      checkClosed();
      return m_wrap.readLine();
    }

    @Override
    public void unreadLine(Line line)
      throws IOException {
      checkClosed();
      m_wrap.unreadLine(line);
    }

    @Override
    public BoundaryResult skipToBoundary(String boundaryStr,
      final boolean leaveBoundary)
      throws IOException {
      checkClosed();
      return m_wrap.skipToBoundary(boundaryStr, leaveBoundary);
    }

    @Override
    public void advanceToNextLine()
      throws IOException {
      checkClosed();
      m_wrap.advanceToNextLine();
    }

    @Override
    public void advanceToEOF()
      throws IOException {
      checkClosed();
      m_wrap.advanceToEOF();
    }

    @Override
    public long skip(long n)
      throws IOException {
      checkClosed();
      return m_wrap.skip(n);
    }

    @Override
    public int available()
      throws IOException {
      checkClosed();
      return m_wrap.available();
    }

    @Override
    public void close()
      throws IOException {
      if(m_closed) {
        return;
      }
      m_closed = true;
      returnStream(m_wrap);
    }

    @Override
    public void mark(int readlimit) {
      checkClosedRE();
      m_wrap.mark(readlimit);
    }

    @Override
    public void reset()
      throws IOException {
      checkClosed();
      m_wrap.reset();
    }

    @Override
    public boolean markSupported() {
      checkClosedRE();
      return m_wrap.markSupported();
    }

    private void checkClosed() throws IOException {
      if(m_closed) {
        throw new IOException("Stream already closed");
      }
    }
    private void checkClosedRE() throws RuntimeException {
      if(m_closed) {
        throw new RuntimeException("Stream already closed");
      }
    }
  }

}