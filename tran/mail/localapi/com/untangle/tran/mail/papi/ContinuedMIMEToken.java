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

package com.untangle.tran.mail.papi;

import java.nio.ByteBuffer;

import com.untangle.tran.token.Token;


/**
 * Token which follows a {@link com.untangle.tran.mail.papi.BeginMIMEToken BeginMIMEToken}.
 * There may be one or more ContinuedMIMETokens after the begin token.  Remaining
 * interesting properties about this token are found in the internal
 * {@link #getMIMEChunk MIMEChunk}.
 */
public final class ContinuedMIMEToken
  implements Token {

  private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

  private final MIMEAccumulator.MIMEChunk m_chunk;

  public ContinuedMIMEToken(MIMEAccumulator.MIMEChunk chunk) {
    m_chunk = chunk;
  }

  /**
   * Get the internal
   * {@link com.untangle.tran.mail.papi.MIMEAccumulator#MIMEChunk MIMEChunk}.
   *
   * @return the chunk
   */
  public MIMEAccumulator.MIMEChunk getMIMEChunk() {
    return m_chunk;
  }
  /**
   * Convienence method.  Equivilant to
   * <code>getMIMEChunk().shouldUnparse()</code>.
   * This <b>must</b> be called before unparsing
   * (calling {@link #getBytes getBytes}) as the
   * data in this chunk may have already been unparsed
   * as a result of being in buffer-and-passthru
   * mode.
   *
   * @return true if this token should be
   *         unparsed (its <code>getBytes()</code>
   *         method can then be used to get the unparsed
   *         bytes).
   */
  public boolean shouldUnparse() {
    return m_chunk.shouldUnparse();
  }

  /**
   * Convienence method.  Equivilant to
   * <code>getMIMEChunk().isLast()</code>.
   *
   * @return true if this token represents
   *         the last of the MIME chunks
   */
  public boolean isLast() {
    return m_chunk.isLast();
  }

  /**
   * Get the bytes for this chunk (unparse).  WARNING - this
   * will always return data, even if {@link #shouldUnparse shouldUnparse}
   * returns false.
   *
   * @return the bytes, as-per Token contract
   */
  public ByteBuffer getBytes() {
    return m_chunk.hasData()?m_chunk.getData():EMPTY_BUFFER;
  }

  /**
   * Returns the number of bytes in this chunk of MIME.  May return
   * zero if the internal chunk is null.
   */
  public int length() {
    return m_chunk==null?0:m_chunk.length();
  }

    public int getEstimatedSize()
    {
        return m_chunk.hasData()?m_chunk.getData().remaining():0;
    }
}
