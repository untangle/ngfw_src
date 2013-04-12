/**
 * $Id$
 */
package com.untangle.node.smtp.mime;

import java.io.File;
import java.io.IOException;

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
public interface MIMESource
{
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
    public MIMEParsingInputStream getInputStream() throws IOException;

    public MIMEParsingInputStream getInputStream(long offset) throws IOException;

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
    public File toFile( ) throws IOException;


    /**
     * If a MIMESource is already in a File, it is permitted
     * to return the existing file and not create a new one
     * via the factory.
     */
    public File toFile( String name) throws IOException;
}
