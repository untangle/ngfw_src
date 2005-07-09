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
  private List<Line> m_preamble;//Including CRLF, such that first boundary is on next line
  private List<Line> m_epilogue;
  
  private MyMIMEPartObserver m_childObserver = 
    new MyMIMEPartObserver();//Only when Multipart
  
  private MyHeadersObserver m_headersObserver = 
    new MyHeadersObserver();
  
  private MIMEPartObserver m_observer;
  
  private MIMESourceRecord m_sourceRecord;
  private ContentXFerEncodingHeaderField.XFreEncoding m_sourceEncoding;
  
  private MIMESourceRecord m_decodedContentRecord;
  
  private MIMESourceRecord m_rawContentRecord;
  
  private boolean m_changed = false;
  
  private boolean m_disposed = false;
  
 
  
  /**
   * Construct a MIME part, reading until the outerBoundary.
   * At the conclusion, the outer boundary is left in the stream.  If the
   * outer boundary is null, then EOF is considered the terminator.
   */
  public MIMEPart(MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary) throws IOException,
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {    
      
    parseAfterHeaders(
      (MIMEPartHeaders) new HeadersParser().parseHeaders(stream,
        source,
        new MIMEPartHeaderFieldFactory(),
        policy),
      stream,
      source,
      policy,
      outerBoundary);
  }

  
    
  
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
    m_headers.setObserver(null);
    m_headers = null;
    if(!m_sourceRecord.isShared()) {
      m_sourceRecord.source.close();
    }      
    m_sourceRecord = null;
    m_preamble = null;
    m_epilogue = null;
    if(isMultipart()) {
      for(MIMEPart child : m_children) {
        child.setObserver(null);
        child.dispose();
      }
    }

  }
  public boolean isDisposed() {
    return m_disposed;
  }
  
  private void checkDisposed()
    throws IllegalStateException {
    if(m_disposed) {
      throw new IllegalStateException("MIMEPart already disposed");
    }
  }
  
  private void changed() {
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
   * This method is intended only for leaf parts (i.e.
   * parts which are {@link #isMultipart not multipart}.
   */
  public File getContentAsFile(FileFactory factory,
    boolean decoded) 
    throws IOException {

    
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
   * 
   */
  public final void writeTo(MIMEOutputStream out)
    throws IOException {  
    
    checkDisposed();
    
    //===== Write Headers =======
    //NOTE: Headers for a given part always  
    //maintain their own raw/assembled state (to  
    //allow header parsing in advance of body).
    m_headers.writeTo(out);
    
    if(!m_changed && m_sourceRecord != null) {
      //======= Write from Source =======   
      out.write(m_sourceRecord);
    }
    else {
      //======= Re-Assemble =======
      if(isMultipart()) {
        String boundary = m_headers.getContentTypeHF().getBoundary();//Shouldn't be null
        for(Line line : m_preamble) {
          out.write(line);
        }
        out.writeLine();
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
        for(Line line : m_epilogue) {
          out.write(line);
        }        
      }
      else {
        //TODO bscott Access the "correct" body, perhaps even causing a 
        //(re)encoding to take place
        if(m_sourceRecord == null) {
          //TODO bscott should we assert or something
        }
        else {
          out.write(m_sourceRecord);
 //         out.pipe(m_sourceRecord.source.getInputStream(m_sourceRecord.start), m_sourceRecord.len);
        }
      }
    }
  }
  

  
  
  
  
  
 //==============================
 // Parse Methods
 //==============================
    
  protected void parseAfterHeaders(MIMEPartHeaders headers,
    MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary)  throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException { 
      
    m_headers = headers;
    m_headers.setObserver(m_headersObserver);
    
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
        parseBodyContent(stream,
          source,
          (int) stream.position(),
          policy,
          outerBoundary);
      }
      else {
        parseMultipartContent(stream,
          source,
          (int) stream.position(),
          policy,
          innerBoundary,
          outerBoundary);
      }
    }//ENDOF Multipart
    else {
      parseBodyContent(stream,
        source,
        (int) stream.position(),
        policy,
        outerBoundary);
    }
  }
  
  private void parseBodyContent(MIMEParsingInputStream stream,
    MIMESource source,
    int contentStart,
    MIMEPolicy policy,
    String outerBoundary) 
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {
      
    m_logger.debug("About to parse body content to boundary \"" + outerBoundary + "\"");    
    
    if(outerBoundary == null) {
      stream.advanceToEOF();
    }
    else {
      stream.advanceToBoundary(outerBoundary, true, true);
    }
    m_sourceRecord = new MIMESourceRecord(source,
      contentStart,
      ((int) stream.position()) - contentStart,
      true);
    m_sourceEncoding = getXFerEncoding();
  }
  
  private void parseMultipartContent(MIMEParsingInputStream stream,
    MIMESource source,
    int contentStart,
    MIMEPolicy policy,
    String innerBoundary,
    String outerBoundary) 
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {
    
    m_logger.debug("About to parse a multipart part with inner boundary \"" + 
      innerBoundary + 
      "\" and outer boundary \"" + 
      outerBoundary + 
      "\"");  
      
    //Not really needed, but for completeness.
    m_sourceEncoding = getXFerEncoding();
    
    m_children = new ArrayList<MIMEPart>();  
    String dashDashInnerBoundary = "--" + innerBoundary;
    String dashDashOuterBoundary = outerBoundary==null?
      null:
      "--" + outerBoundary;
    
    try {
    
      //************ Read Preamble ****************  
    
      //This makes me a bit nervous, assuming the lines
      //are reasonable in length
      m_preamble = stream.readLinesTillBoundary(innerBoundary,
        true,
        false,
        policy.getMaxBodyLineLengthForMultipart());
        
      if(m_preamble == null) {
        m_epilogue = new ArrayList<Line>();
        m_preamble = new ArrayList<Line>();
        m_logger.warn("Encountered a multipart part without any parts inside"); 
        m_sourceRecord = new MIMESourceRecord(source,
          contentStart,
          ((int) stream.position()) - contentStart,
          true);
        return;  
      }     
     
      //************ Read Child Parts ****************
      Line aLine = stream.readLine(policy.getMaxBodyLineLengthForMultipart());
      while(
        aLine != null && 
        !aLine.bufferEndsWith("--")
        ) {
        //TODO: bscott Should we alter the policy to make sure we explicitly allow null headers?
        m_logger.debug("BEGIN Add child MIMEPart"); 
        MIMEPart newChild = new MIMEPart(stream, source, policy, innerBoundary);
        newChild.setObserver(m_childObserver);
        m_children.add(newChild);
        
        m_logger.debug("ENDOF Add child MIMEPart"); 
        //Now, the stream should have been advanced until the next boundary, and the boundary
        //left in-place
        aLine = stream.readLine(policy.getMaxBodyLineLengthForMultipart());
      }
      
      //************ Read Epilogue ****************
      
      //Here, we may have an epilogue, or simply the end of our part
      m_epilogue = new ArrayList<Line>();
      if(aLine != null) {
        //If the last line is not null, then it must have been
        //the terminating "--" of the inner boundary.  Discard it
        //and read any epilogue
        aLine = stream.readLine(policy.getMaxBodyLineLengthForMultipart());
        while(aLine != null) {
          if(dashDashOuterBoundary != null && aLine.bufferStartsWith(dashDashOuterBoundary)) {
            stream.unreadLine(aLine);
            break;
          }
          m_epilogue.add(aLine);
          aLine = stream.readLine(policy.getMaxBodyLineLengthForMultipart());
        }
      }
      m_sourceRecord = new MIMESourceRecord(source,
        contentStart,
        ((int) stream.position()) - contentStart,
        true);
    }
    catch(LineTooLongException ex) {
      throw new MIMEPartParseException(ex);
    }
  }
  
  

  
  //============== Inner Class =================
  
  private class MyMIMEPartObserver 
    implements MIMEPartObserver {
    
    public void mIMEPartChanged(MIMEPart part) {
      changed();
    }
  }//ENDOF MyMIMEPartObserver
  
  
  //============== Inner Class =================
  
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
    FileMIMESource source = new FileMIMESource(f);
    
    MIMEPart mp = new MIMEPart(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);
    
    System.out.println("");
    mp.dump("");

    File tempDir = new File(new File(System.getProperty("user.dir")),
      "mimeFiles");
    if(!tempDir.exists()) {
      tempDir.mkdirs();
    }
    
    TempFileFactory factory = new TempFileFactory(tempDir);
      

    MIMEPart[] children = mp.getLeafParts(true);
    File file = null;
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
    FileOutputStream fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
    mp.writeTo(new MIMEOutputStream(fOut));
    fOut.flush();
    fOut.close();
    
  }
  
  private void dump(String indent) {
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
      System.out.println(indent + "preamble is " + m_preamble.size() + " lines long");
      System.out.println(indent + "epilogue is " + m_epilogue.size() + " lines long");
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