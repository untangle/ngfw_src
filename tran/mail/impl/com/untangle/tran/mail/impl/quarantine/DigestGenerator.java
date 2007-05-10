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

package com.untangle.tran.mail.impl.quarantine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;

import com.untangle.tran.mail.papi.quarantine.InboxIndex;
import com.untangle.tran.mail.papi.quarantine.InboxRecord;
import com.untangle.tran.mail.papi.quarantine.InboxRecordComparator;
import com.untangle.tran.mime.ByteArrayMIMESource;
import com.untangle.tran.mime.ContentTypeHeaderField;
import com.untangle.tran.mime.ContentXFerEncodingHeaderField;
import com.untangle.tran.mime.EmailAddress;
import com.untangle.tran.mime.HeaderNames;
import com.untangle.tran.mime.MIMEMessage;
import com.untangle.tran.mime.MIMEMessageHeaders;
import com.untangle.tran.mime.MIMEPart;
import com.untangle.tran.mime.MIMESourceRecord;
import com.untangle.tran.mime.MIMEUtil;
import com.untangle.tran.mime.RcptType;
import com.untangle.tran.util.IOUtil;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 *
 */
class DigestGenerator {

    //Template name stuff
    private static final String RESOURCE_ROOT =
        "com/untangle/tran/mail/impl/quarantine/";
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
    private static final String TOTAL_NUM_RECORDS_VV = "totalNumRecords";
    private static final String TOTAL_SIZE_RECORDS_VV = "totalSizeRecords";
    private static final String MAX_DAYS_TO_INTERN_VV = "daysToIntern";
    private static final String MAX_DAYS_IDLE_INBOX_VV = "daysIdleInbox";
    private static final String COMPANY_NAME_VV = "company_name";
    private static final String JS_ESCAPER = "jsEscaper";

    private static final String MAIL_BLAST = "http://www.untangle.com/mail_blast/quarantine/images";

    private static final Integer MAX_RECORDS_PER_EMAIL = new Integer(25);

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
    private long maxMailInternInDays;
    private long maxIdleInboxInDays;

    //Silly little Object needed because Velocity seems to get grumpy with static methods.  It
    //simply acts as an object to wrap the JS escaping calls.
    private QuarJSEscaper s_escaper = new QuarJSEscaper();

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

            //RE Bug 1247.  Create a blank "VM_global_library.vm"
            //file to supress lame warning from Velocity.
            fOut = new FileOutputStream(new File(templateRoot, "VM_global_library.vm"));
            fOut.write("\n".getBytes());
            IOUtil.close(fOut);
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
        props.put("resource.loader", "file");
        //Turn-on caching (recommended for production environments)
        props.put("file.resource.loader.cache", "true");
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

    void setMaxMailInternInDays(long maxDays) {
        maxMailInternInDays = maxDays;
    }
    void setMaxIdleInboxInDays(long maxDays) {
        maxIdleInboxInDays = maxDays;
    }

    MIMEMessage generateMsg(InboxIndex index,
                            String serverHost,
                            String to,
                            String from,
                            AuthTokenManager atm) {

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
            /*
              SpamAssassin gives 1.4 "points" when there is an email
              address in the subject.  We observed that Mac clients (which
              have some anti-spam built into the email program) were flagging
              our digests as junk.  Running through SpamAssassin (we do not
              know why the Mac client declared the digest junk) lead to the
              following outcome:


              0.2 NO_REAL_NAME
              1.4 ADDRESS_IN_SUBJECT
              0.1 NORMAL_HTTP_TO_IP
              0.0 HTML_MESSAGE
              0.2 HTML_TAG_EXIST_TBODY
              0.2 HTML_90_100
              1.1 NO_DNS_FOR_FROM

              The only one we can really change is the "address in subject" - hence why
              the line below is commented-out
            */
            //      headers.setSubject("Quarantine Digest for " + to);
            headers.setSubject("Quarantine Digest");

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
            String authToken = atm.createAuthToken(to.trim());

            //Create the Velocity context, for template generation
            VelocityContext context = new VelocityContext();
            context.put(JS_ESCAPER, s_escaper);
            context.put(USER_EMAIL_VV, to);
            context.put(IMAGE_ROOT_VV, MAIL_BLAST);
            context.put(LINK_GENERATOR_VV, new LinkGenerator(serverHost, authToken));

            context.put(MAX_DAYS_TO_INTERN_VV, maxMailInternInDays);
            context.put(MAX_DAYS_IDLE_INBOX_VV, maxIdleInboxInDays);


            String companyName = MvvmContextFactory.context().brandingManager()
                .getBrandingSettings().getCompanyName();
            context.put(COMPANY_NAME_VV, companyName);


            InboxRecord[] allRecords = index.getAllRecords();
            //Sort records by date, with "newest" (i.e. greatest numerical value for time)
            //at the top
            Arrays.sort(allRecords, InboxRecordComparator.getComparator(
                                                                        InboxRecordComparator.SortBy.INTERN_DATE, false));

            if(allRecords == null || allRecords.length == 0) {
                context.put(HAS_RECORDS_VV, Boolean.FALSE);
                context.put(HAS_RECORDS_NOT_SHOWN_VV, Boolean.FALSE);
                context.put(NUM_RECS_NOT_SHOWN_VV, new Integer(0));
                context.put(TOTAL_NUM_RECORDS_VV, new String("0 mail"));
                context.put(TOTAL_SIZE_RECORDS_VV, new String("(0.0 KB)"));
            }
            else {
                context.put(HAS_RECORDS_VV, Boolean.TRUE);

                InboxRecord[] recsToDisplay = null;
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
                context.put(TOTAL_NUM_RECORDS_VV, String.valueOf(index.inboxCount()) + " mails");
                context.put(TOTAL_SIZE_RECORDS_VV, String.valueOf("(" + String.format("%01.1f", new Float(index.inboxSize() / 1024.0)) + " KB)"));
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
}
