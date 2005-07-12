 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id:$
  */
package com.metavize.tran.mime;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import static com.metavize.tran.util.Ascii.*;
import com.metavize.tran.util.*;


/**
 * <b>Work in progress</b>
 */
public class MIMEPart {

  private final Logger m_logger = Logger.getLogger(MIMEPart.class);  

  private MIMEPartHeaders m_headers;
  private List<MIMEPart> m_children;
  
  private MyMIMEPartObserver m_childObserver = 
    new MyMIMEPartObserver();//Only when Multipart
  
  private MyHeadersObserver m_headersObserver = 
    new MyHeadersObserver();
  
  private MIMEPartObserver m_observer;
  
  private MIMESourceRecord m_sourceRecord;
  private ContentXFerEncodingHeaderField.XFreEncoding m_sourceEncoding;
  private long m_preambleLen;//Only when Multipart
  private long m_epilogueLen;//Only when Multipart
  
  private MIMESourceRecord m_decodedContentRecord;//Only when SimplePart
  
  private MIMESourceRecord m_rawContentRecord;//Only when SimplePart
  
  private boolean m_changed = false;
  private boolean m_disposed = false;
  
  protected MIMEPart() {
  }
 
  
  /**
   * Construct a MIME part, reading until the outerBoundary.
   */
  public MIMEPart(MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary) throws IOException,
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {    
      
    parse(new MIMEPartHeaderFieldFactory(),
      stream,
      source,
      policy,
      outerBoundary);
  }

  
  
 //==============================
 // Factory Methods
 //==============================
    
//  public static MIMEPart createMultipart(String subtype) {
//    MIMEPart part = new MIMEPart();
//    m_headers = new MIMEPartHeaders(new MIMEPartHeaderFieldFactory());
//    m_children = new ArrayList<MIMEPart>();
//  }
  
    
  
 //==============================
 // Lifecycle Methods
 //==============================
  
  /**
   * Dispose of this Part and 
   * any children (if {@link #isMultipart multipart}.
   * <br>
   * After this method is called, all other methods
   * will throw IllegalStateException except
   * {@link #isDisposed isDisposed()}.
   */  
  public void dispose() {
    m_disposed = true;
    if(isMultipart()) {
      for(MIMEPart child : m_children) {
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
  }
  
  private void closeSourceRecord(MIMESourceRecord record) {
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
  public boolean isDisposed() {
    return m_disposed;
  }
  
  private void checkDisposed()
    throws IllegalStateException {
    if(m_disposed) {
      throw new IllegalStateException("MIMEPart already disposed");
    }
  }
  
  protected void changed() {
    m_changed = true;
    if(m_observer != null) {
      m_observer.mIMEPartChanged(this);
    }
  }  
  
  
  
   
  
 //==============================
 // "Property" (business) Methods
 //==============================
  
  public void setObserver(MIMEPartObserver observer) {
    m_observer = observer;
  }
  public MIMEPartObserver getObserver() {
    return m_observer;
  }  

  
  public MIMEPartHeaders getMPHeaders() {
    checkDisposed();
    return m_headers;
  }
  
  public boolean isMultipart() {
    checkDisposed();
    return m_headers.getContentTypeHF() != null &&
      m_headers.getContentTypeHF().isMultipart();
  }
  
  public boolean isAttachment() {
    checkDisposed();
    return m_headers.getContentDispositionHF() != null &&
      m_headers.getContentDispositionHF().isAttachment();
  }
  
  /**
   * Obviously only applies if {@link #isAttachment this is an attachment}.
   *
   *
   */
  public String getAttachmentName() {
    checkDisposed();
    return isAttachment()?
      null:
      m_headers.getContentDispositionHF().getFilename();
  }
  
  /**
   * This method will always return something.  If the 
   * encoding is not specified, "SEVEN_BIT" is assumed.
   */
  public ContentXFerEncodingHeaderField.XFreEncoding getXFerEncoding() {
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
  public boolean hasChildren() {
    checkDisposed();
    return isMultipart()?
      m_children != null && m_children.size() > 0:
      false;
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
  public MIMEPart[] getChildParts() {
    checkDisposed();
    if(!isMultipart()) {
      return null;
    }
    //Copy array, so no one can manipulate
    //without us knowing
    return mpListToArray(m_children);
  }
  
  /**
   * Get all leaf (non-multipart) parts.  The <code>recurse></code>
   * flag will return any leaf parts of this part's children (i.e.
   * the whole tree).
   */
  public MIMEPart[] getLeafParts(boolean recurse) {
    checkDisposed();
    ArrayList<MIMEPart> list = new ArrayList<MIMEPart>();
    getLeafPartsInto(list, recurse);
    return mpListToArray(list);    
  }
  
  private void getLeafPartsInto(List<MIMEPart> list, 
    boolean recurse) {
    for(MIMEPart child : m_children) {
    
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
  
  public MIMEPart[] getAttachments() {
    checkDisposed();
    ArrayList<MIMEPart> list = new ArrayList<MIMEPart>();
    addAttachmentsInto(this, list);
    return mpListToArray(list);
  }
  
  /**
   * <b>Note that the child is disposed after this
   * method is called.  If the part is to be re-used in
   * another message, it must be copied first</b>
   */
  public void removeChild(MIMEPart doomed) {
    checkDisposed();
    if(!isMultipart()) {
      return;
    }    
    ListIterator<MIMEPart> it = m_children.listIterator();
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
  
  public boolean containsChild(MIMEPart part) {
    if(!isMultipart()) {
      return false;
    }
    return m_children.contains(part);
  }
  /**
   * This method throws an UnsupportedOperationException if the
   * part is not Multipart
   */
  public void addChild(MIMEPart child) {
    checkDisposed();
    if(!isMultipart()) {
      throw new UnsupportedOperationException("Cannot add children if not multipart");
    }
    m_children.add(child);
    child.setObserver(m_childObserver);
    changed();
  }
  
  private static void addAttachmentsInto(MIMEPart source,
    List<MIMEPart> target) {
    if(source.isMultipart()) {
      for(MIMEPart part : source.m_children) {
        addAttachmentsInto(part, target);
      }
    }
    else if(source.isAttachment()) {
      target.add(source);
    }
  }
  
  private MIMEPart[] mpListToArray(List<MIMEPart> list) {
    return (MIMEPart[]) list.toArray(new MIMEPart[list.size()]);
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
   * @param factory a FileFactory, which may be needed if the content
   *        is not already in a file
   * @param decoded should the content be decoded in the returned File
   */
  public File getContentAsFile(FileFactory factory,
    boolean decoded) 
    throws IOException {
    checkDisposed();
    
    String fileName = isAttachment()?
      getAttachmentName():
      "MIMEPART" + System.identityHashCode(this);
        
    if(!decoded) {
      return getRawContentRecord(factory).source.toFile(factory, fileName);
    }
    else {
      if(m_decodedContentRecord != null) {
        return m_decodedContentRecord.source.toFile(factory, fileName);
      }
      else {
        switch(m_sourceEncoding) {
          case QP:
            decodedContentToFileSource(factory, fileName, new QPDecoderFactory());
            return ((FileMIMESource) m_decodedContentRecord.source).getFile();
          case BASE64:
            decodedContentToFileSource(factory, fileName, new BASE64DecoderFactory());
            return ((FileMIMESource) m_decodedContentRecord.source).getFile();          
          case SEVEN_BIT:
          case EIGHT_BIT:
          case BINARY:
          case UUENCODE://For now, don't attempt uudecode
          case UNKNOWN:          
          default:
             return getRawContentRecord(factory).source.toFile(factory, fileName);
        }
      }
    }
  }
  
  private MIMESourceRecord getRawContentRecord(FileFactory factory)
    throws IOException {
    if(!m_sourceRecord.isShared()) {
      return m_sourceRecord;
    }
    if(m_rawContentRecord == null) {
      File file = decodeToFile(factory, 
        "RAWMIMEPART" + System.identityHashCode(this), 
        new NOOPDecoderFactory());
        
      m_rawContentRecord = new MIMESourceRecord(
        new FileMIMESource(file),
        0,
        (int) file.length(),//Someday when we have 2+ gig files.......
        false); 
    }
    return m_rawContentRecord;
  }
  
  private void decodedContentToFileSource(FileFactory factory, 
    String fileName,
    DecoderFactory decoderFactory) 
    throws IOException {
    File file = decodeToFile(factory, fileName, decoderFactory);
    m_decodedContentRecord = new MIMESourceRecord(
      new FileMIMESource(file),
      0,
      (int) file.length(),//Someday when we have 2+ gig files.......
      false);    
  }
  
  private void pipeToFile(InputStream in, File f)
    throws IOException {
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
  
  private File decodeToFile(FileFactory factory, 
    String fileName,
    DecoderFactory decoderFactory) 
    throws IOException {
    
    MIMEParsingInputStream mpIS = null;
    File newFile = null;
    
    try {
      mpIS = m_sourceRecord.source.getInputStream(m_sourceRecord.start);
      TruncatedInputStream tis = 
        new TruncatedInputStream(mpIS, m_sourceRecord.len);
      InputStream decodeStream = decoderFactory.createDecoder(tis);
      newFile = factory.createFile(fileName);
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
  private abstract class DecoderFactory {
    abstract InputStream createDecoder(InputStream wrapMe);
  }
  
  //============== Inner Class =================
  private class QPDecoderFactory 
    extends DecoderFactory {
    InputStream createDecoder(InputStream wrapMe) {
      return new QPInputStream(wrapMe);
    }
  }  
  
  //============== Inner Class =================
  private class BASE64DecoderFactory 
    extends DecoderFactory {
    InputStream createDecoder(InputStream wrapMe) {
      return new BASE64InputStream(wrapMe);
    }
  }  
  
  //============== Inner Class =================
  private class NOOPDecoderFactory 
    extends DecoderFactory {
    InputStream createDecoder(InputStream wrapMe) {
      return wrapMe;
    }
  }
  
  
  private void close(InputStream in) {  
    try {in.close();}catch(Exception ignore){}
  }
  private void close(OutputStream out) {
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
  public final void writeTo(MIMEOutputStream out)
    throws IOException {  
    
    checkDisposed();
    
    //===== Write Headers =======
    //NOTE: Headers for a given part always  
    //maintain their own raw/assembled state (to  
    //allow header parsing in advance of body).
    m_headers.writeTo(out);
    
    if(!m_changed && m_sourceRecord != null) {//BEGIN Source unchanged
      //======= Write from Source =======   
      out.write(m_sourceRecord);
    }//ENDOF Source unchanged
    else {//BEGIN Source Changed
      //======= Re-Assemble =======
      if(isMultipart()) {//BEGIN Multi Part
        MIMEParsingInputStream mpis = null;
        try {
          String boundary = m_headers.getContentTypeHF().getBoundary();//Shouldn't be null
          
          //Write preamble (if there was one)
          if(m_preambleLen > 0) {
            mpis = m_sourceRecord.source.getInputStream(m_sourceRecord.start);
            out.pipe(mpis, m_preambleLen);
            mpis.close();
          }
          out.write((byte)DASH);
          out.write((byte)DASH);
          out.write(boundary);
          for(MIMEPart child : m_children) {
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
          if(m_epilogueLen > 0) {
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
  protected MIMEParsingInputStream.BoundaryResult parse(
    MIMEPartHeaderFieldFactory headerFactory,
    MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary) throws IOException,
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {    
    
    m_logger.debug("BEGIN Parse headers, (position: " + stream.position() + ")");
    m_headers = (MIMEPartHeaders) new HeadersParser().parseHeaders(stream,
      source,
      headerFactory,
      policy);
    m_logger.debug("ENDOF Parse headers (position: " + stream.position() + ")");
    m_headers.setObserver(m_headersObserver);
      
    return parseAfterHeaders(stream,
      source,
      policy,
      outerBoundary);
  }
 
    
  protected MIMEParsingInputStream.BoundaryResult parseAfterHeaders(MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary)  throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException { 
    
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
          policy,
          outerBoundary);
      }
      else {
        return parseMultipartContent(stream,
          source,
          (int) stream.position(),
          policy,
          innerBoundary,
          outerBoundary);
      }
    }//ENDOF Multipart
    else {
      return parseBodyContent(stream,
        source,
        (int) stream.position(),
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
    MIMEPolicy policy,
    String outerBoundary) 
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {
      
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
      true);
    m_sourceEncoding = getXFerEncoding();
    m_logger.debug("ENDOF parse body content (position: " + 
      stream.position() + ")");     
    return ret;
  }
  
  private MIMEParsingInputStream.BoundaryResult parseMultipartContent(MIMEParsingInputStream stream,
    MIMESource source,
    int contentStart,
    MIMEPolicy policy,
    String innerBoundary,
    String outerBoundary) 
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {
    
    m_logger.debug("BEGIN parse multipart part with inner boundary \"" + 
      innerBoundary + 
      "\" and outer boundary \"" + 
      outerBoundary + 
      "\" (position: " + stream.position() + ")");  
      
    //Not really needed, but for completeness.
    m_sourceEncoding = getXFerEncoding();
    
    m_children = new ArrayList<MIMEPart>();  

  
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
        true);
      m_logger.debug("ENDOF parse multipart part (position: " + 
        stream.position() + ")");
      return boundaryResult;       
    }
    
    //Record the preamble length
    m_preambleLen = ((stream.position() - boundaryResult.boundaryLen) - pos)-1;
  
    while(boundaryResult.boundaryFound && !boundaryResult.boundaryWasLast) {
      MIMEPart newChild = new MIMEPart();
      m_logger.debug("BEGIN Add Child Part (position: " + stream.position() + ")");
      boundaryResult = newChild.parse(new MIMEPartHeaderFieldFactory(),
        stream, 
        source, 
        policy, 
        innerBoundary);
      m_logger.debug("ENDOF Add Child Part (position: " + stream.position() + ")");
      newChild.setObserver(m_childObserver);
      m_children.add(newChild);        
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
      true);
    m_logger.debug("ENDOF parse multipart part (position: " + 
      stream.position() + ")");        
    return boundaryResult;
  }
  
  

  
  //============== Inner Class =================
  
  /**
   * Added to any children as an Observer, so we can tell
   * if things changed.  
   */
  private class MyMIMEPartObserver 
    implements MIMEPartObserver {
    
    public void mIMEPartChanged(MIMEPart part) {
      changed();
    }
  }//ENDOF MyMIMEPartObserver
  
  
  //============== Inner Class =================
  /**
   * Added to headers so we can tell
   * if things changed.  
   */  
  private class MyHeadersObserver
    implements HeadersObserver {

    //TODO bscott Add some intelegence for when the ConentType changes
    //or the ContentXFerEncoding
    
    public void headerFieldsRemoved(LCString headerName) {
//      changed();
    }
  
    public void headerFieldAdded(HeaderField field) {
//      changed();
    }
    
    public void headerFieldChanged(HeaderField field) {
//      changed();
    }
  }//ENDOF MyHeadersObserver
  
 
//------------- Debug/Test ---------------  

  public static void main(String[] args) throws Exception {

    File f = new File(args[0]);
    
    File tempDir = new File(new File(System.getProperty("user.dir")),
      "mimeFiles");
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
      new MIMEPolicy(),
      null);
    
    System.out.println("");
    mp.dump("");


    
    TempFileFactory factory = new TempFileFactory(tempDir);
    
    File file = null;    
    if(mp.isMultipart()) {  

      MIMEPart[] children = mp.getLeafParts(true);

      System.out.println("Now, decode the " + children.length + " leaf children");
      for(MIMEPart part : children) {
        if(!part.isMultipart()) {
          file = part.getContentAsFile(factory, false);
          System.out.println("Raw part to: " + file.getName());      
          file = part.getContentAsFile(factory, true);
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
      file = mp.getContentAsFile(factory, false);
      System.out.println("Raw part to: " + file.getName());      
      file = mp.getContentAsFile(factory, true);
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
  
  protected void dump(String indent) {
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

//------------- Debug/Test ---------------
    
  
  

}