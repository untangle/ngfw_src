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
import com.metavize.tran.util.*;
import static com.metavize.tran.mime.HeaderNames.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import javax.mail.internet.MailDateFormat;

/**
 * Utility methods for working with MIME
 */
public class MIMEUtil {

  /**
   * Wraps the given MIMEMessage in a new MIMEMessage with
   * the old as an RFC822 attachment, with the new
   * plaintext (not multipart-alt) body.
   *
   * @param plainBodyContent the new body content (should be line-formatted
   *        such that lines are not longer than 76 chars).
   * @param oldMsg the old message
   */
  public static MIMEMessage simpleWrap(String plainBodyContent,
    MIMEMessage oldMsg) throws Exception/*TODO bscott exception is lazy*/ {



    //First, we need to "steal" the old headers.  This
    // is easiest by simply re-parsing them
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MIMEOutputStream mimeOut = new MIMEOutputStream(baos);
    oldMsg.getMMHeaders().writeTo(mimeOut);
    mimeOut.flush();

    ByteArrayMIMESource bams = new ByteArrayMIMESource(baos.toByteArray());
    MIMEMessageHeaders newHeaders = MIMEMessageHeaders.parseMMHeaders(
      bams.getInputStream(), bams);

    //Modify new headers, changing the Content-XXX stuff
    newHeaders.removeHeaderFields(CONTENT_TYPE_LC);
    newHeaders.removeHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
    newHeaders.removeHeaderFields(CONTENT_DISPOSITION_LC);
    newHeaders.addHeaderField(CONTENT_TYPE,
      ContentTypeHeaderField.MULTIPART_MIXED +
      "; boundary=\"" + makeBoundary() + "\"");

    newHeaders.addHeaderField(CONTENT_TRANSFER_ENCODING,
      ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

    //Create the new message
    MIMEMessage ret = new MIMEMessage(newHeaders);      

    //Create the body part of the new message
    MIMEPart bodyPart = new MIMEPart();
    bodyPart.getMPHeaders().addHeaderField(CONTENT_TYPE,
      ContentTypeHeaderField.TEXT_PLAIN);
    bodyPart.getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING,
      ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
    byte[] bytes = plainBodyContent.getBytes();
    bodyPart.setContent(
      new MIMESourceRecord(new ByteArrayMIMESource(bytes),
        0,
        bytes.length,
        false));

    //Add the new body to the returned message
    ret.addChild(bodyPart);

    //Add the wrapped old message to the new (returned) message
    ret.addChild(new AttachedMIMEMessage(oldMsg));

    return ret;
    
  }

  /**
   * Get the RFC822-compliant representation of
   * the current time
   *
   * @return the formatted String
   */
  public static String getRFC822Date() {
    return getRFC822Date(new Date());
  }  

  /**
   * Get the RFC822-compliant representation of
   * the given Date
   *
   * @param d the date
   * @return the formatted String
   */
  public static String getRFC822Date(Date d) {
    //Cheat and use JavaMail
    return new MailDateFormat().format(d);
  }

  /**
   * Removes the child from its parent.  Unlike the method
   * with a similar name on MIMEPart itself, this method
   * fixes-up any parent container issues (for example,
   * the parent is "multipart" yet the removal causes
   * the parent to have no children).
   * <br><br>
   * If the child has no parent, then we assume that the
   * child is a top-level MIMEMessage.  In that case, we take
   * different action.  We assume that the intent is to
   * remove some "nasty" content, so we preserve the headers
   * (except for the "Content-XXX") stuff and replace
   * the body with blank text.  This is done
   * via {@link #convertToEmptyTextPart convertToEmptyTextPart()}.
   *
   * @param child the child to be removed from its parent.
   */
  public static void removeChild(MIMEPart child)
    throws HeaderParseException {
    
    MIMEPart parent = child.getParent();
    //Boundary-case.  If the parent is itself
    //a top-level MIMEMessage, and there are no other
    //children.  This really means "nuke my content"
    if(parent == null) {
      convertToEmptyTextPart(child);
      return;
    }
    parent.removeChild(child);
    
    if(parent.getNumChildren() == 0) {
      //If we just created an empty multipart, go up to the parent-parent
      //and remove
      removeChild(parent);
    }
    
  }

  /**
   * Changes the part into an empty "text/plain" part, discarding
   * any previous content
   */
  public static void convertToEmptyTextPart(MIMEPart part)
    throws HeaderParseException /*TODO bscott what about this exception!?!*/{
    part.getMPHeaders().removeHeaderFields(CONTENT_TYPE_LC);
    part.getMPHeaders().removeHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
    part.getMPHeaders().removeHeaderFields(CONTENT_DISPOSITION_LC);

    part.getMPHeaders().addHeaderField(CONTENT_TYPE,
      ContentTypeHeaderField.TEXT_PLAIN);
    part.getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING,
      ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

    part.setContent(
      new MIMESourceRecord(new ByteArrayMIMESource(new byte[0]),
        0,
        0,
        false));
  }

  /**
   * Creates a unique boundary value
   */
  public static String makeBoundary() {
    StringBuilder sb = new StringBuilder();
    sb.append("----");
    sb.append(System.identityHashCode(sb));
    sb.append('_');
    sb.append("060105_");
    sb.append(System.currentTimeMillis());
    return sb.toString();
  }



//------------- Debug/Test ---------------  

  public static void main(String[] args) throws Exception {

    File f = new File(args[0]);

    FileMIMESource source = new FileMIMESource(f);
    
    MIMEMessage mp = new MIMEMessage(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);

    MIMEMessage wrapped = simpleWrap("This is wrapped\r\n", mp);
    
    FileOutputStream fOut = new FileOutputStream(new File(args[0] + "_WRAPPED"));
    wrapped.writeTo(new MIMEOutputStream(fOut));
    fOut.flush();
    fOut.close();
  }

//------------- Debug/Test --------------- 
  
}