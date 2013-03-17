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

package com.untangle.node.smtp;

import java.nio.ByteBuffer;

import com.untangle.node.token.Token;


/**
 * Token which follows a {@link com.untangle.node.smtp.BeginMIMEToken BeginMIMEToken}.
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
     * {@link com.untangle.node.smtp.MIMEAccumulator#MIMEChunk MIMEChunk}.
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
