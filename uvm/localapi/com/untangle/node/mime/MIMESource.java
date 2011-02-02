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

package com.untangle.node.mime;

import java.io.File;
import java.io.IOException;

import com.untangle.node.util.FileFactory;

/**
 * This interface is a strange beast.  It is intended as an
 * abstraction to the bytes which comprise a MIME message, which
 * may be within a file (or memory, esp for testing).  Since we are
 * limited by the file semantics, we cannot offer random access to
 * any byte[] for the underlying MIME content.  Instead, instances
 * of this interface offer a Stream.  Here's where things get ugly.
 * <p>
 * In an ideal world, this would inherit from InputStream.  However, things
 * get a bit confusing if that path is taken.  First of all, most of the heavy
 * lifting is done by a class {@link MIMEParsingInputStream MIMEParsingInputStream}.
 * I'd like to be able to use that class independent of this interface.  Instead,
 * we offer a method to produce such a Stream on instances of this class.
 * <p>
 *
 */
public interface MIMESource {

    /**
     * Access a Stream for the underlying bytes of the MIME.
     * Each time this method is called, a new Stream is produced
     * positioned at the start of the MIME bytes.  The actual position
     * may be accessed via {@link MIMEParsingInputStream#position}.  Note
     * that the returned stream may <b>not</b> return 0 as the initial
     * position, but this should be considered the start of the logical
     * stream for the consumer.
     *
     * @return the input stream.
     */
    public MIMEParsingInputStream getInputStream()
        throws IOException;

    public MIMEParsingInputStream getInputStream(long offset)
        throws IOException;

    /**
     * Close this Source.  Any associated resources (i.e. Files)
     * should be closed and deleted.  Any open MIMEParsingInputStreams
     * will also be implicitly closed.
     */
    public void close();

    /**
     * If a MIMESource is already in a File, it is permitted
     * to return the existing file and not create a new one
     * via the factory.
     */
    public File toFile(FileFactory factory) throws IOException;


    /**
     * If a MIMESource is already in a File, it is permitted
     * to return the existing file and not create a new one
     * via the factory.
     */
    public File toFile(FileFactory factory, String name) throws IOException;
}
