/**
 * $Id$
 */
package com.untangle.node.smtp.mime;

import static com.untangle.node.util.Ascii.DASH;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import com.untangle.node.util.BASE64InputStream;
import com.untangle.node.util.QPInputStream;
import com.untangle.node.util.TruncatedInputStream;

/**
 * 
 */
public class MIMEPart
{
    private final Logger m_logger = Logger.getLogger(MIMEPart.class);

    private MIMEPartHeaders m_headers;
    private List<MIMEPart> m_children;

    /**
     * Made protected only for the "AttachedMIMEMessage" subclass
     */
    protected MyMIMEPartObserver m_childObserver = new MyMIMEPartObserver();//Only when Multipart

    private MyHeadersObserver m_headersObserver = new MyHeadersObserver();

    private MIMEPartObserver m_observer;

    private MIMESourceRecord m_sourceRecord;
    private List<MIMESourceRecord> m_oldSourceRecords;
    private ContentXFerEncodingHeaderField.XFreEncoding m_sourceEncoding;
    private long m_preambleLen;//Only when Multipart
    private long m_epilogueLen;//Only when Multipart

    private MIMESourceRecord m_decodedContentRecord;//Only when SimplePart

    private MIMESourceRecord m_rawContentRecord;//Only when SimplePart

    private boolean m_changed = false;
    private boolean m_disposed = false;

    private MIMEPart m_parent;

    private Object m_userObj;

    public MIMEPart()
    {
        this(new MIMEPartHeaders());
    }

    public MIMEPart(MIMEPartHeaders headers)
    {
        m_headers = headers;
    }

    /**
     * Construct a MIME part, reading until the outerBoundary.
     */
    public MIMEPart(MIMEParsingInputStream stream,
                    MIMESource source,
                    boolean ownsSource,
                    MIMEPolicy policy,
                    String outerBoundary) throws IOException,
                                                 InvalidHeaderDataException,
                                                 HeaderParseException,
                                                 MIMEPartParseException
    {

        parse(new MIMEPartHeaderFieldFactory(),
              stream,
              source,
              ownsSource,
              policy,
              outerBoundary);

        m_logger.debug("[<init>] Created " +
                       (isMultipart()?"multipart ":"") +
                       (isAttachment()?"attachment ":"") +
                       "part with preamble len: " + m_preambleLen +
                       " and epilogue len: " + m_epilogueLen +
                       " and content length: " + m_sourceRecord.len +
                       " with content starting at: " + m_sourceRecord.start +
                       " of source record");


    }

    /**
     * Construct a MIME part using the already-parsed headers.
     */
    public MIMEPart(MIMEParsingInputStream stream,
                    MIMESource source,
                    boolean ownsSource,
                    MIMEPolicy policy,
                    String outerBoundary,
                    MIMEPartHeaders headers) throws IOException,
                                                    InvalidHeaderDataException,
                                                    HeaderParseException,
                                                    MIMEPartParseException
    {

        m_headers = headers;
        m_headers.setObserver(m_headersObserver);

        parseAfterHeaders(stream,
                          source,
                          ownsSource,
                          policy,
                          outerBoundary);

        m_logger.debug("[<init>] Created " +
                       (isMultipart()?"multipart ":"") +
                       (isAttachment()?"attachment ":"") +
                       "part with preamble len: " + m_preambleLen +
                       " and epilogue len: " + m_epilogueLen +
                       " and content length: " + m_sourceRecord.len +
                       " with content starting at: " + m_sourceRecord.start +
                       " of source record");
    }

    //==============================
    // Factory Methods
    //==============================


    //==============================
    // Lifecycle Methods
    //==============================

    /**
     * Get the parent part of this MIME part.  Note that
     * this may be null if this is a top-level (or unattached)
     * part.
     */
    public MIMEPart getParent()
    {
        return m_parent;
    }

    public void setParent(MIMEPart parent)
    {
        m_parent = parent;
    }

    /**
     * Dispose of this Part and
     * any children (if {@link #isMultipart multipart}.
     * <br>
     * After this method is called, all other methods
     * will throw IllegalStateException except
     * {@link #isDisposed isDisposed()}.
     */
    public void dispose()
    {
        if(m_disposed) {
            return;
        }
        if(isMultipart()) {
            for(MIMEPart child : getChildList()) {
                child.setObserver(null);
                child.dispose();
            }
        }
        m_headers.setObserver(null);
        m_headers = null;
        closeSourceRecord(m_sourceRecord);
        m_sourceRecord = null;
        closeSourceRecord(m_decodedContentRecord);
        m_decodedContentRecord = null;
        closeSourceRecord(m_rawContentRecord);
        m_rawContentRecord = null;
        if(m_oldSourceRecords != null) {
            for(MIMESourceRecord record : m_oldSourceRecords) {
                closeSourceRecord(record);
            }
            m_oldSourceRecords.clear();
            m_oldSourceRecords = null;
        }
        m_disposed = true;
    }

    private void closeSourceRecord(MIMESourceRecord record)
    {
        if(record != null && !record.isShared()) {
            record.source.close();
        }
    }

    /**
     * Test if this MIMEPart has been disposed.  Once disposed,
     * this is the only method which can be called which will
     * not throw an IllegalStateException
     *
     * @return true if disposed
     */
    public boolean isDisposed()
    {
        return m_disposed;
    }

    protected void checkDisposed() throws IllegalStateException
    {
        if(m_disposed) {
            throw new IllegalStateException("MIMEPart already disposed");
        }
    }

    /**
     * <b>This has been made public for tests.  Do not call it.  It
     * is intended to subclasses</b>
     */
    public void changed()
    {
        m_changed = true;
        if(m_observer != null) {
            m_observer.mIMEPartChanged(this);
        }
    }

    protected MIMESourceRecord getSourceRecord()
    {
        return m_sourceRecord;
    }

    public boolean isChanged()
    {
        return m_changed || (null != m_headers && m_headers.isChanged());
    }

    /**
     * Associate an arbitrary Object with this Part.
     * This is useful when iterating through a collection
     * of MIMEParts, to detect if a given MIMEPart (which has
     * no real unique identifier) has been visited.
     * <br><br>
     * This class does nothing with the user object except
     * store it for the consumer.
     *
     * @param obj the user object (may be null).
     */
    public void setUserObject(Object obj)
    {
        m_userObj = obj;
    }

    /**
     * Get the {@link #setUserObject arbitrary object}
     * associated with this part.
     *
     * @return the user object, or null if
     *         one was not {@link #setUserObject set}
     */
    public Object getUserObject()
    {
        return m_userObj;
    }

    //==============================
    // "Property" (business) Methods
    //==============================

    public void setObserver(MIMEPartObserver observer)
    {
        m_observer = observer;
    }

    public MIMEPartObserver getObserver()
    {
        return m_observer;
    }

    public MIMEPartHeaders getMPHeaders()
    {
        checkDisposed();
        return m_headers;
    }

    public boolean isMultipart()
    {
        checkDisposed();
        return m_headers.getContentTypeHF() != null &&
            m_headers.getContentTypeHF().isMultipart();
    }

    public boolean isAttachment()
    {
        checkDisposed();
        return m_headers.getContentDispositionHF() != null &&
            m_headers.getContentDispositionHF().isAttachment();
    }

    /**
     * Obviously only applies if {@link #isAttachment this is an attachment}.
     *
     *
     */
    public String getAttachmentName()
    {
        checkDisposed();
        return !isAttachment()?
            null:
            m_headers.getContentDispositionHF().getFilename();
    }

    /**
     * This method will always return something.  If the
     * encoding is not specified, "SEVEN_BIT" is assumed.
     */
    public ContentXFerEncodingHeaderField.XFreEncoding getXFerEncoding()
    {
        checkDisposed();
        return m_headers.getContentXFerEncodingHF() != null?
            m_headers.getContentXFerEncodingHF().getEncodingType():
            ContentXFerEncodingHeaderField.XFreEncoding.SEVEN_BIT;
    }


    /**
     * Test if this part has child parts.  This will always
     * return false if {@link #isMultipart not multipart},
     * and may return true if a multipart Part happens
     * to have no children.
     */
    public boolean hasChildren()
    {
        return getNumChildren() > 0;
    }

    /**
     * Gets the number of children (only applies to multiparts)
     */
    public int getNumChildren()
    {
        checkDisposed();
        return ( isMultipart() ? getChildList().size() : 0 );
    }

    /**
     * Return an array of the direct children of this part.  If the
     * children are multipart, this method must be called on them
     * to walk the tree.
     * <br>
     * If you are interested in the complete set of all leaf-parts (non-
     * multipart), use {@link #getLeafParts getLeafParts}
     * <br>
     * Will always return null if not {@link #isMultipart multipart}.
     * May return a zero-length array if there are no children.
     *
     */
    public MIMEPart[] getChildParts()
    {
        checkDisposed();
        if(!isMultipart()) {
            return null;
        }
        //Copy array, so no one can manipulate
        //without us knowing
        return mpListToArray(getChildList());
    }

    /**
     * Get all leaf (non-multipart) parts.  The <code>recurse></code>
     * flag will return any leaf parts of this part's children (i.e.
     * the whole tree).
     */
    public MIMEPart[] getLeafParts( boolean recurse )
    {
        checkDisposed();
        ArrayList<MIMEPart> list = new ArrayList<MIMEPart>();
        getLeafPartsInto(list, recurse);
        return mpListToArray(list);
    }

    private void getLeafPartsInto( List<MIMEPart> list, boolean recurse )
    {
        for(MIMEPart child : getChildList()) {

            if(child.isMultipart()) {
                if(recurse) {
                    child.getLeafPartsInto(list, true);
                }
            }
            else {
                list.add(child);
            }
        }
    }

    public MIMEPart[] getAttachments()
    {
        checkDisposed();
        ArrayList<MIMEPart> list = new ArrayList<MIMEPart>();
        addAttachmentsInto(this, list);
        return mpListToArray(list);
    }

    /**
     * Remove the given child part.  This method only applies
     * to Multipart parts.
     * <br><br>
     * <b>Use this method only if you know what you're doing.
     * Otherwise, I recommend calling
     * {@link com.untangle.node.smtp.mime.MIMEUtil#removeChild MIMEUtil} to
     * fixup alignment/content type issues.</b>
     * <br><br>
     * <b>Note that the child is disposed after this
     * method is called.  If the part is to be re-used in
     * another message, it must be copied first</b>
     */
    public void removeChild( MIMEPart doomed )
    {
        checkDisposed();
        if(!isMultipart()) {
            return;
        }
        ListIterator<MIMEPart> it = getChildList().listIterator();
        while(it.hasNext()) {
            MIMEPart child = it.next();
            if(child.equals(doomed)) {
                child.setObserver(null);
                child.dispose();
                it.remove();
                changed();
            }
        }
    }

    public boolean containsChild( MIMEPart part )
    {
        if(!isMultipart()) {
            return false;
        }
        return getChildList().contains(part);
    }
    /**
     * This method throws an UnsupportedOperationException if the
     * part is not Multipart
     */
    public void addChild( MIMEPart child )
    {
        checkDisposed();
        if(!isMultipart()) {
            throw new UnsupportedOperationException("Cannot add children if not multipart");
        }
        getChildList().add(child);
        child.setParent(this);
        child.setObserver(m_childObserver);
        changed();
    }


    /**
     * Use this method carefully.  It assumes that the content
     * is encoded, unless the encoding defined by the headers
     * on this part do not require decoding (such as 7bit).
     * <br><br>
     * This only applies to non-multiparts.
     */
    public void setContent( MIMESourceRecord record )
    {
        m_logger.debug("[setContent()] Called");
        changed();
        if(m_decodedContentRecord != null && !m_decodedContentRecord.isShared()) {
            m_decodedContentRecord.source.close();
        }
        if(m_rawContentRecord != null && !m_rawContentRecord.isShared()) {
            m_rawContentRecord.source.close();
        }
        if(m_sourceRecord != null && !m_sourceRecord.isShared()) {
            m_sourceRecord.source.close();
        }
        m_decodedContentRecord = null;
        m_rawContentRecord = null;
        m_decodedContentRecord = record;
        m_sourceRecord = record;
        m_preambleLen = 0;
        m_epilogueLen = 0;
    }

    private static void addAttachmentsInto( MIMEPart source, List<MIMEPart> target )
    {
        if(source.isMultipart()) {
            for(MIMEPart part : source.getChildList()) {
                addAttachmentsInto(part, target);
            }
        }
        else if(source.isAttachment()) {
            target.add(source);
        }
    }

    private MIMEPart[] mpListToArray(List<MIMEPart> list)
    {
        return list.toArray(new MIMEPart[list.size()]);
    }

    /**
     * Access to the internal List of children.
     */
    protected List<MIMEPart> getChildList()
    {
        if(m_children == null) {
            m_children = new ArrayList<MIMEPart>();
        }
        return m_children;
    }





    //==============================
    // To File Methods
    //==============================


    /**
     * Get the content as a File.
     * <br>
     * This method is intended only for leaf parts (i.e.
     * parts which are {@link #isMultipart not multipart}.
     *
     * @param decoded should the content be decoded in the returned File
     */
    public File getContentAsFile( boolean decoded )
        throws IOException
    {
        checkDisposed();

        if(isMultipart()) {
            throw new IOException("Cannot get content for non-leaf part");
        }

        // 8/11/05 jdi -- User provided file name removed.
        String fileNamePrefix = "mimepart";

        if(!decoded) {
            return getRawContentRecord().source.toFile( fileNamePrefix );
        }
        else {
            if(m_decodedContentRecord != null) {
                return m_decodedContentRecord.source.toFile( fileNamePrefix );
            }
            else {
                if(m_sourceEncoding == null) {
                    m_sourceEncoding = ContentXFerEncodingHeaderField.XFreEncoding.UNKNOWN;
                }
                switch(m_sourceEncoding) {
                case QP:
                    decodedContentToFileSource( fileNamePrefix, new QPDecoderFactory() );
                    return ((FileMIMESource) m_decodedContentRecord.source).getFile();
                case BASE64:
                    decodedContentToFileSource( fileNamePrefix, new BASE64DecoderFactory() );
                    return ((FileMIMESource) m_decodedContentRecord.source).getFile();
                case SEVEN_BIT:
                case EIGHT_BIT:
                case BINARY:
                case UUENCODE://For now, don't attempt uudecode
                case UNKNOWN:
                default:
                    return getRawContentRecord().source.toFile( fileNamePrefix );
                }
            }
        }
    }

    private MIMESourceRecord getRawContentRecord()
        throws IOException
    {
        if(!m_sourceRecord.isShared()) {
            return m_sourceRecord;
        }
        if(m_rawContentRecord == null) {
            File file = decodeToFile( "RAWMIMEPART" + System.identityHashCode(this), new NOOPDecoderFactory());

            m_rawContentRecord = new MIMESourceRecord(
                                                      new FileMIMESource(file),
                                                      0,
                                                      (int) file.length(),//Someday when we have 2+ gig files.......
                                                      false);
        }
        return m_rawContentRecord;
    }

    private void decodedContentToFileSource( String fileName, DecoderFactory decoderFactory )
        throws IOException
    {
        File file = decodeToFile( fileName, decoderFactory);
        m_decodedContentRecord = new MIMESourceRecord(
                                                      new FileMIMESource(file),
                                                      0,
                                                      (int) file.length(),//Someday when we have 2+ gig files.......
                                                      false);
    }

    private void pipeToFile( InputStream in, File f )
        throws IOException
    {
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
            BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
            MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
            mimeOut.pipe(in);
            mimeOut.flush();
            bufOut.flush();
            fOut.flush();
            fOut.close();
        }
        catch(IOException ex) {
            close(fOut);
            IOException ex2 = new IOException();
            ex2.initCause(ex);
            throw ex2;
        }
    }

    private File decodeToFile( String fileName, DecoderFactory decoderFactory )
        throws IOException
    {
        MIMEParsingInputStream mpIS = null;
        File newFile = null;

        try {
            mpIS = m_sourceRecord.source.getInputStream(m_sourceRecord.start);
            TruncatedInputStream tis = new TruncatedInputStream(mpIS, m_sourceRecord.len);
            InputStream decodeStream = decoderFactory.createDecoder(tis);
            newFile = File.createTempFile(fileName, null);
            pipeToFile(decodeStream, newFile);
            mpIS.close();
            return newFile;
        }
        catch(IOException ex) {
            close(mpIS);
            if(newFile != null) {
                newFile.delete();
            }
            IOException ex2 = new IOException();
            ex2.initCause(ex);
            throw ex2;
        }
    }

    //============== Inner Class =================
    /**
     * Used as an abstraction, so we can use the same
     * methods while performing differrent (or no)
     * decoding
     */
    private abstract class DecoderFactory
    {
        abstract InputStream createDecoder(InputStream wrapMe);
    }

    //============== Inner Class =================
    private class QPDecoderFactory extends DecoderFactory
    {
        InputStream createDecoder(InputStream wrapMe)
        {
            return new QPInputStream(wrapMe);
        }
    }

    //============== Inner Class =================
    private class BASE64DecoderFactory extends DecoderFactory
    {
        InputStream createDecoder(InputStream wrapMe)
        {
            return new BASE64InputStream(wrapMe);
        }
    }

    //============== Inner Class =================
    private class NOOPDecoderFactory extends DecoderFactory
    {
        InputStream createDecoder(InputStream wrapMe)
        {
            return wrapMe;
        }
    }

    private void close(InputStream in)
    {
        try {in.close();}catch(Exception ignore){}
    }

    private void close(OutputStream out)
    {
        try {out.close();}catch(Exception ignore){}
    }

    //==============================
    // Output Methods
    //==============================

    /**
     * Write this MIMEPart and all children to
     * the given stream.  This method first
     * attempts to use the cached MIMESourceRecord
     * for itself and its children, provided this
     * object and its children have not changed
     * since initial parsing.
     *
     * @param out the output stream
     */
    public void writeTo( MIMEOutputStream out ) throws IOException
    {
        checkDisposed();

        //===== Write Headers =======
        //NOTE: Headers for a given part always
        //maintain their own raw/assembled state (to
        //allow header parsing in advance of body).
        m_headers.writeTo(out);

        if(!m_changed && m_sourceRecord != null) {//BEGIN Source unchanged
            //======= Write from Source =======
            m_logger.debug("[writeTo()] Writing out to stream from source record (did not change)");
            out.write(m_sourceRecord);
        }//ENDOF Source unchanged
        else {//BEGIN Source Changed
            //======= Re-Assemble =======
            if(isMultipart()) {//BEGIN Multi Part
                m_logger.debug("[writeTo()] Re-assemble mutlipart from components");
                MIMEParsingInputStream mpis = null;
                try {
                    String boundary = m_headers.getContentTypeHF().getBoundary();//Shouldn't be null

                    //Write preamble (if there was one)
                    if(m_preambleLen > 0 && m_sourceRecord!= null) {
                        m_logger.debug("[writeTo()] write preamble");
                        mpis = m_sourceRecord.source.getInputStream(m_sourceRecord.start);
                        out.pipe(mpis, m_preambleLen);
                        mpis.close();
                        out.writeLine();
                    }
                    out.write((byte)DASH);
                    out.write((byte)DASH);
                    out.write(boundary);
                    for(MIMEPart child : getChildList()) {
                        m_logger.debug("[writeTo()] writing child");
                        out.writeLine();
                        child.writeTo(out);
                        out.writeLine();
                        out.write((byte)DASH);
                        out.write((byte)DASH);
                        out.write(boundary);
                    }
                    out.write((byte)DASH);
                    out.write((byte)DASH);
                    out.writeLine();
                    //Write epilogue (if there was one)
                    if(m_epilogueLen > 0 && m_sourceRecord!= null) {
                        m_logger.debug("[writeTo()] writing epilogue");
                        mpis = m_sourceRecord.source.getInputStream(m_sourceRecord.len - m_epilogueLen);
                        out.pipe(mpis, m_epilogueLen);
                        mpis.close();
                    }
                }
                catch(IOException ex) {
                    close(mpis);
                    IOException ex2 = new IOException();
                    ex2.initCause(ex);
                    throw ex2;
                }
            }//ENDOF Multi Part
            else {//BEGIN Simple Part
                //TODO bscott Access the "correct" body, perhaps even causing a
                //(re)encoding to take place
                if(m_sourceRecord == null) {
                    //TODO bscott should we assert or something
                }
                else {
                    m_logger.debug("[writeTo()] writing leaf part from source record");
                    out.write(m_sourceRecord);
                }
            }//ENDOF Simple Part
        }//ENDOF Source Changed
    }

    //==============================
    // Parse Methods
    //==============================


    /**
     * Cause this MIMEPart to assume the contents parsed
     * from the stream/source combination.
     *
     * @param outerBoundary the outer boundary, indicating when this
     *        part is terminated.  If null, then this part will assume
     *        its contents extend to the end of the stream.
     */
    protected MIMEParsingInputStream.BoundaryResult parse(MIMEPartHeaderFieldFactory headerFactory,
                                                          MIMEParsingInputStream stream,
                                                          MIMESource source,
                                                          boolean ownsSource,
                                                          MIMEPolicy policy,
                                                          String outerBoundary)
        throws IOException, InvalidHeaderDataException, HeaderParseException,
               MIMEPartParseException
    {

        m_logger.debug("BEGIN Parse headers, (position: " + stream.position() + ")");
        m_headers = (MIMEPartHeaders) new HeadersParser().parseHeaders(stream,
                                                                       source,
                                                                       headerFactory,
                                                                       policy);
        m_logger.debug("ENDOF Parse headers (position: " + stream.position() + ")");
        m_headers.setObserver(m_headersObserver);

        return parseAfterHeaders(stream,
                                 source,
                                 ownsSource,
                                 policy,
                                 outerBoundary);
    }


    protected MIMEParsingInputStream.BoundaryResult parseAfterHeaders(MIMEParsingInputStream stream,
                                                                      MIMESource source,
                                                                      boolean ownsSource,
                                                                      MIMEPolicy policy,
                                                                      String outerBoundary)
        throws IOException, InvalidHeaderDataException, HeaderParseException,
               MIMEPartParseException
    {

        //Determine if this is a composite type
        //of interest.  This means multipart/*
        ContentTypeHeaderField ctHeader = m_headers.getContentTypeHF();
        if(ctHeader != null && ctHeader.isMultipart()) {//BEGIN Multipart

            String innerBoundary = ctHeader.getBoundary();

            //As-per RFC2046, the Content-Type header requires
            //a boundary.  If null, we must consult our
            //MIME policy
            if(innerBoundary == null) {
                switch(policy.getBadMultipartPartPolicy()) {
                case TREAT_AS_TEXT_AND_CONVERT_TYPE:
                    m_logger.warn("Encountered a multipart content type (\"" +
                                  ctHeader.getContentType() + "\") without a boundary.  Convert " +
                                  "Content-Type to \"text/plain\" and treat as text as-per policy");
                    ctHeader.setPrimaryType(ContentTypeHeaderField.TEXT_PRIM_TYPE_STR);
                    ctHeader.setSubType(ContentTypeHeaderField.PLAIN_SUB_TYPE_STR);
                    break;
                case TREAT_AS_TEXT:
                    m_logger.warn("Encountered a multipart content type (\"" +
                                  ctHeader.getContentType() + "\") without a boundary.  Treat as " +
                                  "text (without altering the type) as-per policy");
                    break;
                case RAISE_EXCEPTION:
                    throw new MIMEPartParseException("Encountered a multipart content type (\"" +
                                                     ctHeader.getContentType() + "\") without a boundary.  Exception " +
                                                     "raised as-per policy");
                }
                //If we're here, the policy was "TREAT_AS_TEXT_AND_CONVERT_TYPE"
                //or "TREAT_AS_TEXT"
                return parseBodyContent(stream,
                                        source,
                                        (int) stream.position(),
                                        ownsSource,
                                        policy,
                                        outerBoundary);
            }
            else {
                return parseMultipartContent(stream,
                                             source,
                                             (int) stream.position(),
                                             ownsSource,
                                             policy,
                                             innerBoundary,
                                             outerBoundary);
            }
        }//ENDOF Multipart
        else {
            return parseBodyContent(stream,
                                    source,
                                    (int) stream.position(),
                                    ownsSource,
                                    policy,
                                    outerBoundary);
        }
    }

    /**
     * Parse a simple (non-multipart) part's body
     */
    private MIMEParsingInputStream.BoundaryResult parseBodyContent(MIMEParsingInputStream stream,
                                                                   MIMESource source,
                                                                   int contentStart,
                                                                   boolean ownsSource,
                                                                   MIMEPolicy policy,
                                                                   String outerBoundary)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException,
               MIMEPartParseException
    {

        m_logger.debug("BEGIN parse body content to boundary \"" + outerBoundary + "\" (position: " +
                       stream.position() + ")");

        MIMEParsingInputStream.BoundaryResult ret = null;

        if(outerBoundary == null) {
            stream.advanceToEOF();
            ret = MIMEParsingInputStream.BOUNDARY_NOT_FOUND;
        }
        else {
            ret = stream.skipToBoundary(outerBoundary, false);
        }

        long recordLen = stream.position() - contentStart;
        if(ret.boundaryFound) {
            recordLen-=ret.boundaryLen;
        }


        m_sourceRecord = new MIMESourceRecord(source,
                                              contentStart,
                                              (int) recordLen,
                                              !ownsSource);
        m_sourceEncoding = getXFerEncoding();
        m_logger.debug("ENDOF parse body content (position: " +
                       stream.position() + ")");
        return ret;
    }

    private MIMEParsingInputStream.BoundaryResult parseMultipartContent(MIMEParsingInputStream stream,
                                                                        MIMESource source,
                                                                        int contentStart,
                                                                        boolean ownsSource,
                                                                        MIMEPolicy policy,
                                                                        String innerBoundary,
                                                                        String outerBoundary)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException,
               MIMEPartParseException
    {

        m_logger.debug("BEGIN parse multipart part with inner boundary \"" +
                       innerBoundary +
                       "\" and outer boundary \"" +
                       outerBoundary +
                       "\" (position: " + stream.position() + ")");

        //Not really needed, but for completeness.
        m_sourceEncoding = getXFerEncoding();

        //    m_children = new ArrayList<MIMEPart>();


        //************ Read Preamble ****************
        m_logger.debug("BEGIN read preamble (position: " + stream.position() + ")");
        long pos = stream.position();
        MIMEParsingInputStream.BoundaryResult boundaryResult =
            stream.skipToBoundary(innerBoundary, false);
        m_logger.debug("ENDOF read preamble (position: " + stream.position() + ")");

        //Check for boundary case of no opening boundary found
        if(!boundaryResult.boundaryFound) {
            m_logger.warn("Encountered a multipart part without any parts inside");
            m_preambleLen = 0;
            m_epilogueLen = 0;
            m_sourceRecord = new MIMESourceRecord(source,
                                                  contentStart,
                                                  ((int) stream.position()) - contentStart,
                                                  !ownsSource);
            m_logger.debug("ENDOF parse multipart part (position: " +
                           stream.position() + ")");
            return boundaryResult;
        }

        //Record the preamble length
        m_preambleLen = ((stream.position() - boundaryResult.boundaryLen) - pos);

        while(boundaryResult.boundaryFound && !boundaryResult.boundaryWasLast) {
            MIMEPartHeaders childHeaders = (MIMEPartHeaders) new HeadersParser().parseHeaders(stream,
                                                                                              source,
                                                                                              new MIMEPartHeaderFieldFactory(),
                                                                                              policy);

            MIMEPart newChild = null;
            m_logger.debug("BEGIN Add Child Part (position: " + stream.position() + ")");
            //Special-case the attached message
            if(childHeaders.getContentTypeHF() != null &&
               childHeaders.getContentTypeHF().isMessageRFC822()) {
                m_logger.debug("BEGIN Child is itself a MIMEMessage (\"attached mime message\")");
                newChild = new AttachedMIMEMessage(childHeaders);
                boundaryResult = ((AttachedMIMEMessage) newChild).parseChild(
                                                                             stream,
                                                                             source,
                                                                             policy,
                                                                             innerBoundary);
                m_logger.debug("ENDOF Child is itself a MIMEMessage (\"attached mime message\")");
            }
            else {
                newChild = new MIMEPart(childHeaders);
                boundaryResult = newChild.parseAfterHeaders(stream,
                                                            source,
                                                            false,
                                                            policy,
                                                            innerBoundary);
            }
            m_logger.debug("ENDOF Add Child Part (position: " + stream.position() + ")");
            newChild.setObserver(m_childObserver);
            newChild.setParent(this);
            getChildList().add(newChild);
            /*
              MIMEPart newChild = new MIMEPart();
              m_logger.debug("BEGIN Add Child Part (position: " + stream.position() + ")");
              boundaryResult = newChild.parse(new MIMEPartHeaderFieldFactory(),
              stream,
              source,
              false,
              policy,
              innerBoundary);
              m_logger.debug("ENDOF Add Child Part (position: " + stream.position() + ")");
              newChild.setObserver(m_childObserver);
              newChild.setParent(this);
              getChildList().add(newChild);
            */
        }


        //************ Read Epilogue ****************
        m_logger.debug("BEGIN Read Epilogue (position: " + stream.position() + ")");
        pos = stream.position();
        boundaryResult = stream.skipToBoundary(outerBoundary, false);
        m_logger.debug("ENDOF Read Epilogue (position: " + stream.position() + ")");


        long recordLen = stream.position() - contentStart;
        if(boundaryResult.boundaryFound) {
            recordLen-=boundaryResult.boundaryLen;
            m_epilogueLen = (stream.position() - boundaryResult.boundaryLen) - pos;
        }
        else {
            m_epilogueLen = 0;
        }

        m_sourceRecord = new MIMESourceRecord(source,
                                              contentStart,
                                              (int) recordLen,
                                              !ownsSource);
        m_logger.debug("ENDOF parse multipart part (position: " +
                       stream.position() + ")");
        return boundaryResult;
    }




    //============== Inner Class =================

    /**
     * Added to any children as an Observer, so we can tell
     * if things changed.
     */
    private class MyMIMEPartObserver implements MIMEPartObserver
    {
        public void mIMEPartChanged(MIMEPart part)
        {
            changed();
        }
    }//ENDOF MyMIMEPartObserver


    //============== Inner Class =================
    /**
     * Added to headers so we can tell
     * if things changed.
     */
    private class MyHeadersObserver implements HeadersObserver
    {
        //TODO bscott Add some intelegence for when the ConentType changes
        //or the ContentXFerEncoding

        public void headerFieldsRemoved(LCString headerName)
        {
            //      changed();
        }

        public void headerFieldAdded(HeaderField field)
        {
            //      changed();
        }

        public void headerFieldChanged(HeaderField field)
        {
            //      changed();
        }
    }//ENDOF MyHeadersObserver


    //------------- Debug/Test ---------------

    /**
     * Debugging method which describes a MIME message
     * in terms loosely found in the IMAP spec.
     */
    public String describe()
    {
        StringBuilder sb = new StringBuilder();
        describeImpl(sb, null, 0);
        return sb.toString();
    }

    protected void describeImpl( StringBuilder sb, String numSoFar, int thisIndex )
    {
        String newLine = System.getProperty("line.separator", "\n");

        String prefix = numSoFar==null?"":(numSoFar + Integer.toString(thisIndex));
        sb.append(prefix).append("  ");
        sb.append("HEADER: ").
            append(getMPHeaders().getNumHeaderFields()).
            append(" fields").append(newLine);
        if(m_headers.getContentTypeHF() != null) {
            sb.append(prefix).
                append("  ").
                append("HEADER: Content-Type: ").
                append(m_headers.getContentTypeHF().getContentType()).
                append(newLine);
        }
        if(isMultipart()) {
            sb.append(prefix).
                append("  ").
                append("BODY: multipart").
                append(newLine);
            MIMEPart[] kids = getChildParts();
            if(!("".equals(prefix))) {
                prefix = prefix + ".";
            }
            for(int i = 0; i<kids.length; i++) {
                kids[i].describeImpl(sb, prefix, i+1);
            }
        }
        else {
            sb.append(prefix).
                append("  ").
                append("BODY: len: ").
                append(Integer.toString(m_sourceRecord.len)).
                append(newLine);
            if(isAttachment()) {
                sb.append(prefix).
                    append("  ").
                    append("BODY: attachment.  Name: ").
                    append(getMPHeaders().getFilename()).
                    append(newLine);
            }
        }

    }

    public static void main( String[] args ) throws Exception
    {
        File f = new File(args[0]);

        File tempDir = new File("/tmp/mimeFiles");

        if(!tempDir.exists()) {
            tempDir.mkdirs();
        }

        //Dump file to another file, with byte offsets.  This
        //makes troubleshooting really easy
        FileInputStream fIn = new FileInputStream(f);
        FileOutputStream fOut =
            new FileOutputStream(new File("byteMap.txt"));
        int rawRead = fIn.read();
        int counter = 0;
        while(rawRead != -1) {
            fOut.write((counter + ": ").getBytes());
            if(rawRead < 33 || rawRead > 126) {
                fOut.write(("(unprintable)" + rawRead).getBytes());
            }
            else {
                fOut.write((byte) rawRead);
            }
            fOut.write(System.getProperty("line.separator").getBytes());
            rawRead = fIn.read();
            counter++;
        }
        fIn.close();
        fOut.flush();
        fOut.close();

        FileMIMESource source = new FileMIMESource(f);

        MIMEPart mp = new MIMEPart(source.getInputStream(),
                                   source,
                                   true,
                                   new MIMEPolicy(),
                                   null);

        System.out.println("");
        mp.dump("");

        File file = null;
        if(mp.isMultipart()) {

            MIMEPart[] children = mp.getLeafParts(true);

            System.out.println("Now, decode the " + children.length + " leaf children");
            for(MIMEPart part : children) {
                if(!part.isMultipart()) {
                    file = part.getContentAsFile( false );
                    System.out.println("Raw part to: " + file.getName());
                    file = part.getContentAsFile( true );
                    System.out.println("Decoded part to: " + file.getName());
                }
            }

            for(MIMEPart part : children) {
                part.m_changed = true;
                part.getObserver().mIMEPartChanged(part);
                part.getMPHeaders().addHeaderField("FooBar", "Goo");
                part.getMPHeaders().removeHeaderFields(new LCString("FooBar"));
            }
            System.out.println("Try writing it out (after declaring changed)");
            fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
            mp.writeTo(new MIMEOutputStream(fOut));
            fOut.flush();
            fOut.close();
        }
        else {
            file = mp.getContentAsFile( false );
            System.out.println("Raw part to: " + file.getName());
            file = mp.getContentAsFile( true );
            System.out.println("Decoded part to: " + file.getName());
            System.out.println("Try writing it out (after declaring changed)");
            mp.m_changed = true;
            //        mp.getObserver().mIMEPartChanged(part);
            mp.getMPHeaders().addHeaderField("FooBar", "Goo");
            mp.getMPHeaders().removeHeaderFields(new LCString("FooBar"));
            fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
            mp.writeTo(new MIMEOutputStream(fOut));
            fOut.flush();
            fOut.close();
        }

    }

    protected void dump(String indent)
    {
        System.out.println(indent + "---BEGIN---");
        System.out.println(indent + m_headers.getNumHeaderFields() + " headers");
        String contentType = null;
        if(m_headers.getContentTypeHF() != null) {
            contentType = m_headers.getContentTypeHF().getContentType();
        }
        System.out.println(indent + "Content Type: " + contentType);
        if(isMultipart()) {
            String childIndent = "    " + indent;
            MIMEPart[] kids = getChildParts();
            System.out.println(indent + "multipart (" + kids.length + " kids)");
            System.out.println(indent + "preamble is " + m_preambleLen + " bytes long");
            System.out.println(indent + "epilogue is " + m_epilogueLen + " bytes long");
            for(int i = 0; i<kids.length; i++) {
                kids[i].dump(childIndent);
            }
        }
        else {
            if(isAttachment()) {
                System.out.println(indent + "(attachment)");
            }
            System.out.println(indent + "length: " + m_sourceRecord.len);
        }
        System.out.println(indent + "---ENDOF---");
    }
}
