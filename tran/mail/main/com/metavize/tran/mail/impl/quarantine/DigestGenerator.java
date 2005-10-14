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

package com.metavize.tran.mail.impl.quarantine;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;

import static com.metavize.tran.util.Ascii.CRLF;
import com.metavize.tran.util.Pair;

import com.metavize.tran.mime.ContentTypeHeaderField;
import com.metavize.tran.mime.ContentXFerEncodingHeaderField;
import com.metavize.tran.mime.HeaderNames;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.mime.MIMEUtil;
import com.metavize.tran.mime.MIMESourceRecord;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.mime.RcptType;
import com.metavize.tran.mime.EmailAddress;
import com.metavize.tran.mime.ByteArrayMIMESource;

import org.apache.log4j.Logger;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;

import java.util.Properties;
import java.util.Arrays;

import java.net.URLEncoder;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import com.metavize.tran.util.IOUtil;

/**
 *
 */
class DigestGenerator {

  //Template name stuff
  private static final String RESOURCE_ROOT =
    "com/metavize/tran/mail/impl/quarantine/";
  private static final String HTML_TEMPLATE_NAME = "DigestEmail_HTML.vm";
  private static final String TEXT_TEMPLATE_NAME = "DigestEmail_TXT.vm";

  //Variables within the Velocity templates.  Note that these must align
  //with the contents of the Velocity ("*.vm") templates.
  private static final String USER_EMAIL_VV = "user_email";
  private static final String IMAGE_ROOT_VV = "image_root";
  private static final String LINK_GENERATOR_VV = "linkgenerator";
  private static final String INBOX_RECORDS_VV = "inboxrecords";
  private static final String HAS_RECORDS_VV = "hasRecords";
  private static final String HAS_RECORDS_NOT_SHOWN_VV = "hasRecsNotShown";
  private static final String NUM_RECS_NOT_SHOWN_VV = "numRecsNotShown";


  private static final Integer MAX_RECORDS_PER_EMAIL = new Integer(10);

  private static String DIGEST_SUBJECT = "You've Got Spam!";

  //We save the lengths of the templates, so we can make a guestimate
  //of how large the byte[] to accumulate them need be.  Avoids
  //a lot of stupid copies.
  private int m_htmlTemplateLen = 0;
  private int m_txtTemplateLen = 0;
  private static final int PER_RECORD_LOAD = 1000;//Number of bytes added for each record.

  private final Logger m_logger =
    Logger.getLogger(DigestGenerator.class);

  private VelocityEngine m_velocityEngine;
  private Template m_htmlTemplate;
  private Template m_txtTemplate;

  DigestGenerator() {

    //We have to extract the template files
    //to a temp directory, then tell Velocity to
    //use that directory.  This is a workaround
    //the fact that we cannot use their
    //"ClasspathResourceLoader" due to classloader
    //issues
    String templatedDirName = null;
    FileOutputStream fOut = null;
    InputStream in = null;
    try {
      File tempDir = new File(System.getProperty("bunnicula.tmp.dir"));
      File templateRoot = new File(tempDir, "velocity" + File.separator + "quarantine");
      if(!templateRoot.exists()) {
        templateRoot.mkdirs();
      }
      templatedDirName = templateRoot.getAbsolutePath();

      in = getClass().getClassLoader().getResourceAsStream(
        RESOURCE_ROOT + HTML_TEMPLATE_NAME);
      fOut = new FileOutputStream(new File(templateRoot, HTML_TEMPLATE_NAME));
      IOUtil.pipe(in, fOut);
      fOut.flush();
      IOUtil.close(fOut);
      IOUtil.close(in);

      in = getClass().getClassLoader().getResourceAsStream(
        RESOURCE_ROOT + TEXT_TEMPLATE_NAME);
      fOut = new FileOutputStream(new File(templateRoot, TEXT_TEMPLATE_NAME));
      IOUtil.pipe(in, fOut);
      fOut.flush();
      IOUtil.close(fOut);
      IOUtil.close(in);    
      m_logger.debug("Created template files in \"" +
        templatedDirName + "\"");
    }
    catch(Exception ex) {
      IOUtil.close(fOut);
      IOUtil.close(in);      
      m_logger.error("Unable to copy velocity template files to \"" +
        templatedDirName + "\"", ex);
    }
  
    m_velocityEngine = new VelocityEngine();

    Properties props = new Properties();

    //Sets the $velocityCount variable to start at 1
    //instead of 0 when iterating through a "foreach"
    props.put("directive.foreach.counter.initial.value", "1");

    //----Set how resources are loaded----
    props.put("resource.loader",
      "file");
    //Turn-on caching (recommended for production environments)
    props.put("file.resource.loader.cache",
      "true");
    //Turn-off looking for updates
    props.put("file.resource.loader.modificationCheckInterval", "1");//Integer.toString(Integer.MAX_VALUE));
    
    //Assign the "path" for the file loader
    props.put("file.resource.loader.path", templatedDirName);
/*    
        
    //----Set how resources are loaded----
    props.put("resource.loader",
      "ClasspathResourceLoader");
    //Turn-on caching (recommended for production environments)
    props.put("ClasspathResourceLoader.resource.loader.cache",
      "true");
    //Assign the "classpath" class loader's class
    props.put("ClasspathResourceLoader.resource.loader.class",
      "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
*/
    //---Logging---
    props.put("runtime.log.logsystem.class",
      "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
    //I don't know why it doesn't just use its classnames like everyone else...
    props.put("runtime.log.logsystem.log4j.category",
      "org.apache.velocity");

    try {
      m_velocityEngine.init(props);
    }
    catch(Exception ex) {
      m_logger.error("Unable to initialize Velocity engine", ex);
    }
      
    //Load the templates
    try {
      m_htmlTemplate = m_velocityEngine.getTemplate(HTML_TEMPLATE_NAME);
      m_txtTemplate = m_velocityEngine.getTemplate(TEXT_TEMPLATE_NAME);
    }
    catch(Exception ex) {
      m_logger.error("Unable to load templates", ex);
    }

  }


  MIMEMessage generateMsg(InboxIndex index,
    String serverHost,
    String to,
    String from,
    Quarantine quarantine) {

    try {
      MIMEMessageHeaders headers = new MIMEMessageHeaders();
  
      
      //Take care of boiler-plate headers
      headers.addHeaderField(HeaderNames.DATE, MIMEUtil.getRFC822Date());
      headers.addHeaderField(HeaderNames.MIME_VERSION, "1.0");
  
      //Add content-centric headers
      headers.addHeaderField(HeaderNames.CONTENT_TYPE,
        ContentTypeHeaderField.MULTIPART_ALTERNATIVE +
        "; boundary=\"" + MIMEUtil.makeBoundary() + "\"");
  
      headers.addHeaderField(HeaderNames.CONTENT_TRANSFER_ENCODING,
        ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
  
      //Subject
      headers.setSubject(DIGEST_SUBJECT);
  
      //Sender/Recipient
      EmailAddress toAddress =
        EmailAddress.parseNE(to);
      if(toAddress == null) {
        m_logger.warn("Cannot create digest email, because recipient email address \""
          + to + "\" could not be parsed");
        return null;
      }
      headers.addRecipient(toAddress, RcptType.TO);

      
      
      EmailAddress fromAddress =
        EmailAddress.parseNE(from);
      if(fromAddress == null) {
        m_logger.warn("Cannot create digest email, because sender email address \""
          + from + "\" could not be parsed");
        return null;
      }
      headers.setFrom(fromAddress);
  
      //Create the MIME message, initialized on these headers
      MIMEMessage ret = new MIMEMessage(headers);


      //Create the auth token
      String authToken = quarantine.createAuthToken(to.trim());      

      //Create the Velocity context, for template generation
      VelocityContext context = new VelocityContext();
      context.put(USER_EMAIL_VV, to);
      context.put(IMAGE_ROOT_VV, "http://" + serverHost + "/quarantine/images");
      context.put(LINK_GENERATOR_VV, new LinkGenerator(serverHost, authToken));

      InboxRecord[] allRecords = index.getAllRecords();
      if(allRecords == null || allRecords.length == 0) {
        context.put(HAS_RECORDS_VV, Boolean.FALSE);
        context.put(HAS_RECORDS_NOT_SHOWN_VV, Boolean.FALSE);
        context.put(NUM_RECS_NOT_SHOWN_VV, new Integer(0));
      }
      else {
        InboxRecord[] recsToDisplay = null;
        context.put(HAS_RECORDS_VV, Boolean.TRUE);
        if(allRecords.length > MAX_RECORDS_PER_EMAIL) {
          context.put(HAS_RECORDS_NOT_SHOWN_VV, Boolean.TRUE);
          context.put(NUM_RECS_NOT_SHOWN_VV, new Integer(allRecords.length - MAX_RECORDS_PER_EMAIL));
          recsToDisplay = new InboxRecord[MAX_RECORDS_PER_EMAIL];
          System.arraycopy(allRecords, 0, recsToDisplay, 0, recsToDisplay.length);
        }
        else {
          context.put(HAS_RECORDS_NOT_SHOWN_VV, Boolean.FALSE);
          context.put(NUM_RECS_NOT_SHOWN_VV, new Integer(0));
          recsToDisplay = allRecords;
        }
        context.put(INBOX_RECORDS_VV, recsToDisplay);
      }

  
      //Create the (text) Body part
      MIMEPart textPart = new MIMEPart();
      textPart.getMPHeaders().addHeaderField(HeaderNames.CONTENT_TYPE,
        ContentTypeHeaderField.TEXT_PLAIN);
      textPart.getMPHeaders().addHeaderField(HeaderNames.CONTENT_TRANSFER_ENCODING,
        ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
      byte[] textBytes = mergeTemplate(context,
        m_txtTemplate,
        m_txtTemplateLen + (index.size()*PER_RECORD_LOAD));
      textPart.setContent(
        new MIMESourceRecord(new ByteArrayMIMESource(textBytes),
          0,
          textBytes.length,
          false));
  
      //Add the text body to the returned message
      ret.addChild(textPart);


  
      //Create the (html) Body part
      MIMEPart htmlPart = new MIMEPart();
      htmlPart.getMPHeaders().addHeaderField(HeaderNames.CONTENT_TYPE,
        ContentTypeHeaderField.TEXT_HTML);
      htmlPart.getMPHeaders().addHeaderField(HeaderNames.CONTENT_TRANSFER_ENCODING,
        ContentXFerEncodingHeaderField.SEVEN_BIT_STR);


      byte[] htmlBytes = mergeTemplate(context,
        m_htmlTemplate,
        m_htmlTemplateLen + (index.size()*PER_RECORD_LOAD));
      if(htmlBytes == null) {
        m_logger.warn("Returning null.  Unable to merge HTML template");
        return null;
      }
      htmlPart.setContent(
        new MIMESourceRecord(new ByteArrayMIMESource(htmlBytes),
          0,
          htmlBytes.length,
          false));      
      
      //Add the html body to the returned message
      ret.addChild(htmlPart);
  
      return ret;
    }
    catch(Exception ex) {
      m_logger.warn("Exception attempting to generate digest email", ex);
      return null;
    }
  
  }

  private byte[] mergeTemplate(VelocityContext context,
    Template template,
    int estSize) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(estSize);
      OutputStreamWriter writer = new OutputStreamWriter(baos);
      template.merge(context, writer);
      writer.flush();
      return baos.toByteArray();
    }
    catch(Exception ex) {
      m_logger.error("Unable to merge template", ex);
      return null;
    }
  }
/*
  private static String makeURL(String host, String subPath,
    Pair<String, String>...arguments) {
    StringBuilder sb = new StringBuilder();
    sb.append("http://").append(host).append(subPath);
    if(arguments.length > 0) {
      sb.append("?");
      boolean first = true;
      for(Pair<String, String> p : arguments) {
        if(first) {first = false;}
        else {sb.append('&');}
        sb.append(p.a);
        sb.append('=');
        sb.append(URLEncoder.encode(p.b));
      }
    }
    return sb.toString();
  }
*/
  //$inbox.url$
  //$title$
  //$help.url$
  //$digest.recipient$
  private static final String BEGIN_HTML_TEMPLATE = 
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"+
    "<html>"+
    "<head>"+
    "  <meta http-equiv=\"Content-Type\""+
    " content=\"text/html; charset=iso-8859-1\">"+
    "  <style>"+
    "  .maintable { font:  normal 11 Geneva,Arial; background-color: #FFFFFF; color: #000000;border: 0px solid #335687;}"+
    "  .headingtable { border-bottom: 1px solid #; }"+
    "  .labeldiv {  border-left: 1px solid #3666BB;}"+
    "  .linkheader { font:  normal 11 Geneva,Arial; color: #FFFFFF; }"+
    "  .digestable { font:  bold 11 Geneva,Arial; background-color: #F1F1F1; color: #372F2F; border: 1px solid #CDCDCD; }"+
    "  .digesttable { background-color: #FFFFFF; font-size: bold 11 Geneva,Arial color: #5E76AA; border: 1px solid #335687; }"+
    "  .tableitem0 { font: normal 11 Geneva,Arial; background-color: #FFFFFF; color: #372F2F; }"+
    "  .tableitem1 { font: normal 11 Geneva,Arial; background-color: #F1F1F1; color: #372F2F; }"+
    "  .tableheader { font:  bold 13 Geneva,Arial; background-color: #3666BB; color: #FFFFFF; }"+
    "  .tableheaderc { font:  bold 11 Geneva,Arial; color: #000000; background-color:#B2CCF7; }"+
    "  .bigheader { font:  bold 15 Geneva,Arial; color: #372F2F; }"+
    "  .subbigheader { font:  bold 13 Geneva,Arial; color: #372F2F;}"+
    "  .infotext { font:  normal 13 Geneva,Arial; color: #372F2F;}"+
    "  .errortext { font:  bold 13 Geneva,Arial; color: #DD0000;}"+
    "  .successtext { font:  bold 13 Geneva,Arial; color: #188218;}"+
    "  .logo { font:  bold 18 arial, helvetica, sans serif; color: #686868; }"+
    "  .logosub {  font:  bold 13 arial, helvetica, sans serif; color: #ED8D53; }"+
    "  .custlogo { font:  bold 18 Geneva,Arial; color:#000000; }"+
    "  .custsublogo {  font:  bold 13 Geneva,Arial; color:#000000; }"+
    "  .commands { font:  normal 11 Geneva,Arial; }"+
    "  .copyright { font:  normal 8 Geneva,Arial; text-align: center;}"+
    "  .headercommands { font:  bold 11 Geneva,Arial; color: #FFFFFF; background-color:#3666BB; }"+
    "  .buttonTable { font: bold 11 Geneva,Arial; color: #335587; background: #E7E9ED; border-bottom: 2px solid #716F64; border-right: 2px solid #716F64; border-top: 2px solid #FFFFFF; border-left: 2px solid #FFFFFF; }"+
    "  .formButton { color : 335687; background-color : E7E9ED; font: bold 9 Geneva,Arial;vertical-align : middle; border-width : 1px;}"+
    "  .table-head { color : 383333;background-color : BAB7B0; font : bold 10 Geneva,Arial; height : 17px; letter-spacing : 1px; text-transform: uppercase; }"+
    "  .table-subhead { color : 686663; background-color : E9E8E6; font : bold 10 Geneva,Arial; height : 25px; vertical-align : middle; }"+
    "  .table-cell { color : 335687; background-color : FFFFFF; font : bold 9 Geneva,Arial; height : 20px; vertical-align : middle; }"+
    "  .table-cell-list { color : 335687;background-color : FFFFFF; font : bold 9 Geneva,Arial; height : 14; vertical-align : middle; }"+
    "  .mailListBorder { background-color: #c0c0c0; }"+
    "  .mailViewHeader { font-size: 10px; font-weight: bold;background-color: #E9E8E6;text-align: center;  color: #4f4f50; }"+
    "  .mailViewHeaderLeft { font-size: 10px; font-weight: bold; background-color: #ECE9DB; text-align: left; color: #4f4f50; }"+
    "  .mailViewRowUnreadOdd,.mvo {font-size: 10px; font-weight: bold; background-color: #ffffff; }"+
    "  .mailViewRowUnreadEven { font-size: 10px; font-weight: bold; background-color: #ffffff; }"+
    "  .mailViewCheckbox,.mvc { background-color: #ffffff; }"+
    "  .mailViewRowReadEven { font-size: 10px; background-color: #ffffff; font-weight: normal; }"+
    "  .mailViewRowReadOdd { font-size: 10px; background-color: #ffffff; font-weight: normal; }"+
    "  .mailViewSmall { color: #5e76aa; text-decoration: none; text-indent: 0pt; font-size: 8px;text-align: left;}"+
    "  .reportViewHeader { font-size: 10px; font-weight: bold; background-color: #ECE9DB; color: #4f4f50; }"+
    "  </style>"+
    "  <title>$title$</title>"+
    "</head>"+
    "<body alink=\"#3666bb\" bgcolor=\"#ffffff\" link=\"#3666bb\" vlink=\"#3666bb\">"+
    ""+
    "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"+
    "  <tbody>"+
    "    <tr>"+
    "      <td>"+
    "      <table class=\"headingtable\" border=\"0\" cellpadding=\"0\" cellspacing=\"3\" height=\"50\" width=\"100%\">"+
    "        <tbody>"+
    "          <tr>"+
    "            <td>&nbsp;</td>"+
    "            <td width=\"200\">"+
    "            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "              <tbody>"+
    "                <tr>"+
    "                  <td class=\"logo\"><font color=\"#006aff\" face=\"arial, helvetica, sans serif\" size=\"4\">Metavize</font></td>"+
    "                </tr>"+
    "                <tr>"+
    "                  <td class=\"logosub\"><font color=\"#686868\" face=\"arial, helvetica, sans serif\" size=\"3\">EdgeGuard</font></td>"+
    "                </tr>"+
    "              </tbody>"+
    "            </table>"+
    "            </td>"+
    "            <td>"+
    "            <table style=\"border-left: 1px solid rgb(204, 204, 204);\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "              <tbody>"+
    "                <tr>"+
    "                  <td width=\"10\">&nbsp;</td>"+
    "                  <td class=\"bigheader\" nowrap=\"nowrap\">"+
    "                    <font face=\"Geneva,Arial\" size=\"2\">"+
    "                      Quarantine Digest"+
    "                    </font></td>"+
    "                </tr>"+
    "                <tr>"+
    "                  <td width=\"10\">&nbsp;</td>"+
    "                  <td class=\"subbigheader\" nowrap=\"nowrap\">"+
    "                    <font face=\"Geneva,Arial\" size=\"2\">"+
    "                      for $digest.recipient$"+
    "                    </font>"+
    "                  </td>"+
    "                </tr>"+
    "              </tbody>"+
    "            </table>"+
    "            </td>"+
    "          </tr>"+
    "        </tbody>"+
    "      </table>"+
    "      <table class=\"maintable\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"+
    "        <tbody>"+
    "          <tr>"+
    "            <td>"+
    "            <table border=\"0\" cellpadding=\"5\" cellspacing=\"0\" width=\"100%\">"+
    "              <tbody>"+
    "                <tr>"+
    "                  <td>&nbsp;</td>"+
    "                </tr>"+
    "                <tr>"+
    "                  <td class=\"infotext\">"+
    "                    <font face=\"Geneva,Arial\" size=\"2\">"+
    "This is some blurb at the top of the digest email.  It may be conditional"+
    "based on if there is anything in the inbox at all"+
    "                    </font>"+
    "                  </td>"+
    "                </tr>"+
    "                <tr>"+
    "                  <td>"+
    "                  <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"+
    "                    <tbody>"+
    "                      <tr>"+
    "                        <td class=\"commands\" align=\"right\">&nbsp;&nbsp;"+
    "                          <a href=\"$help.url$\">"+
    "                            <font face=\"Geneva,Arial\" size=\"2\">"+
    "                              Help"+
    "                            </font>"+
    "                          </a>"+
    "                          &nbsp;"+
    "                        </td>"+
    "                      </tr>"+
    "                    </tbody>"+
    "                  </table>"+
    "                  </td>"+
    "                </tr>"+
    "              </tbody>"+
    "            </table>"+
    "            <table class=\"digestable\" bgcolor=\"#3666bb\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"+
    "              <tbody>"+
    "                <tr>"+
    "                  <td align=\"left\">"+
    "                  <table border=\"0\" cellpadding=\"3\" cellspacing=\"0\" width=\"100%\">"+
    "                    <tbody>"+
    "                      <tr>"+
    "                        <td class=\"tableheader\" nowrap=\"nowrap\">"+
    "                          <font color=\"#ffffff\" face=\"Geneva,Arial\" size=\"2\">Quarantine Digest&nbsp;&nbsp;</font>"+
    "                        </td>"+
    "                        <td class=\"headercommands\" align=\"right\" nowrap=\"nowrap\">"+
    "                          <font face=\"Geneva,Arial\" size=\"2\">"+
    "                            &nbsp;&nbsp;&nbsp;"+
    "                            <a class=\"linkheader\" href=\"$inbox.url$\">"+
    "                              <font color=\"#ffffff\">"+
    "                                <font color=\"#ffffff\" face=\"Geneva,Arial\" size=\"2\">"+
    "                                  Visit Most Current Messages"+
    "                                </font>"+
    "                              </font>"+
    "                            </a> &nbsp;"+
    "                          </font>"+
    "                        </td>"+
    "                      </tr>"+
    "                    </tbody>"+
    "                  </table>"+
    "                  </td>"+
    "                </tr>"+
    "                <tr>"+
    "                  <td>"+
    "                  <table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" width=\"100%\">"+
    "                    <tbody>"+
    "                      <tr class=\"tableheaderc\" bgcolor=\"#b2ccf7\">"+
    "                        <td>&nbsp;</td>"+
    "                        <td class=\"labeldiv\"><font face=\"Geneva,Arial\" size=\"2\">From</font></td>"+
    "                        <td class=\"labeldiv\"><font face=\"Geneva,Arial\" size=\"2\">Subject</font></td>"+
    "                      </tr>";

  //$sender$
  //$subject$
  //$rescue.url$
  //$purge.url$
  private static final String EVEN_ROW_TEMPLATE =
    "                      <tr class=\"tableitem1\" bgcolor=\"#f1f1f1\" valign=\"top\">"+
    "                        <td nowrap=\"nowrap\">"+
    "                          <a href=\"$rescue.url$\">"+
    "                            <font face=\"Geneva,Arial\" size=\"2\">"+
    "                              Release"+
    "                            </font>"+
    "                          </a>"+
    "                          &nbsp;|&nbsp;"+
    "                          <a href=\"$purge.url$\">"+
    "                            <font face=\"Geneva,Arial\" size=\"2\">"+
    "                              Delete"+
    "                            </font>"+
    "                          </a>"+
    "                        </td>"+
    "                        <td><font face=\"Geneva,Arial\" size=\"2\">$sender$</font></td>"+
    "                        <td><font face=\"Geneva,Arial\" size=\"2\">$subject$</font></td>"+
    "                      </tr>";
    
  private static final String ODD_ROW_TEMPLATE =
    "                      <tr class=\"tableitem0\" bgcolor=\"#ffffff\" valign=\"top\">"+
    "                        <td nowrap=\"nowrap\">"+
    "                          <a href=\"$rescue.url$\">"+
    "                            <font face=\"Geneva,Arial\" size=\"2\">"+
    "                              Release"+
    "                            </font>"+
    "                          </a>"+
    "                          &nbsp;|&nbsp;"+
    "                          <a href=\"$purge.url$\">"+
    "                            <font face=\"Geneva,Arial\" size=\"2\">"+
    "                              Delete"+
    "                            </font>"+
    "                          </a>"+
    "                        </td>"+
    "                        <td><font face=\"Geneva,Arial\" size=\"2\">$sender$</font></td>"+
    "                        <td><font face=\"Geneva,Arial\" size=\"2\">$subject$</font></td>"+
    "                      </tr>";
    
  private static final String END_HTML_TEMPLATE =
    "                    </tbody>"+
    "                  </table>"+
    "                  </td>"+
    "                </tr>"+
    "              </tbody>"+
    "            </table>"+
    "            </td>"+
    "          </tr>"+
    "        </tbody>"+
    "      </table>"+
    "      </td>"+
    "    </tr>"+
    "    <tr>"+
    "      <td>&nbsp;</td>"+
    "    </tr>"+
    "    <tr>"+
    "      <td class=\"infotext\">"+
    "        <font face=\"Geneva,Arial\" size=\"2\">"+
    "          For more information contact your System Administrator."+
    "        </font>"+
    "      </td>"+
    "    </tr>"+
    "    <tr>"+
    "      <td><br>"+
    "      <br>"+
    "      </td>"+
    "    </tr>"+
    "    <tr>"+
    "      <td class=\"copyright\">"+
    "        <font face=\"Geneva,Arial\" size=\"1\">"+
    "          Powered by<br>"+
    "      Metavize EdgeGuard"+
    "        </font>"+
    "      </td>"+
    "    </tr>"+
    "  </tbody>"+
    "</table>"+
    "</body>"+
    "</html>";




  
}