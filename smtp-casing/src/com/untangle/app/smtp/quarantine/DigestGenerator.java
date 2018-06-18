/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.util.I18nUtil;

/**
 *
 */
class DigestGenerator
{
    // Template name stuff
    private static final String RESOURCE_ROOT = "com/untangle/app/smtp/quarantine/";
    private static final String HTML_TEMPLATE_NAME = "DigestSimpleEmail_HTML.vm";

    // Variables within the Velocity templates. Note that these must align
    // with the contents of the Velocity ("*.vm") templates.
    private static final String USER_EMAIL_VV = "user_email";
    private static final String INBOX_LINK_VV = "inbox_link";
    private static final String I18N_UTIL_VV = "i18n_util";

    // We save the lengths of the templates, so we can make a guestimate
    // of how large the byte[] to accumulate them need be. Avoids
    // a lot of stupid copies.
    private int m_htmlTemplateLen = 0;

    private final Logger m_logger = Logger.getLogger(DigestGenerator.class);

    private VelocityEngine m_velocityEngine;
    private Template m_htmlTemplate;

    /**
     * Initialize instance of DigestGenerator.
     */
    DigestGenerator() {

        // We have to extract the template files
        // to a temp directory, then tell Velocity to
        // use that directory. This is a workaround
        // the fact that we cannot use their
        // "ClasspathResourceLoader" due to classloader
        // issues
        String templatedDirName = null;
        FileOutputStream fOut = null;
        InputStream in = null;
        try {
            File tempDir = new File("/tmp/");
            File templateRoot = new File(tempDir, "velocity" + File.separator + "quarantine");
            if (!templateRoot.exists()) {
                templateRoot.mkdirs();
            }
            templatedDirName = templateRoot.getAbsolutePath();

            in = getClass().getClassLoader().getResourceAsStream(RESOURCE_ROOT + HTML_TEMPLATE_NAME);
            fOut = new FileOutputStream(new File(templateRoot, HTML_TEMPLATE_NAME));
            IOUtil.pipe(in, fOut);
            fOut.flush();
            IOUtil.close(fOut);
            IOUtil.close(in);

            m_logger.debug("Created template files in \"" + templatedDirName + "\"");

            // RE Bug 1247. Create a blank "VM_global_library.vm"
            // file to supress lame warning from Velocity.
            fOut = new FileOutputStream(new File(templateRoot, "VM_global_library.vm"));
            fOut.write("\n".getBytes());
            IOUtil.close(fOut);
        } catch (Exception ex) {
            IOUtil.close(fOut);
            IOUtil.close(in);
            m_logger.error("Unable to copy velocity template files to \"" + templatedDirName + "\"", ex);
        }

        m_velocityEngine = new VelocityEngine();

        Properties props = new Properties();

        // Sets the $velocityCount variable to start at 1
        // instead of 0 when iterating through a "foreach"
        props.put("directive.foreach.counter.initial.value", "1");

        // ----Set how resources are loaded----
        props.put("resource.loader", "file");
        // Turn-on caching (recommended for production environments)
        props.put("file.resource.loader.cache", "true");
        // Turn-off looking for updates
        props.put("file.resource.loader.modificationCheckInterval", "1");// Integer.toString(Integer.MAX_VALUE));

        // Assign the "path" for the file loader
        props.put("file.resource.loader.path", templatedDirName);
        /*
         * 
         * //----Set how resources are loaded---- props.put("resource.loader", "ClasspathResourceLoader"); //Turn-on
         * caching (recommended for production environments) props.put("ClasspathResourceLoader.resource.loader.cache",
         * "true"); //Assign the "classpath" class loader's class
         * props.put("ClasspathResourceLoader.resource.loader.class",
         * "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader" );
         */
        // ---Logging---
        props.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        // I don't know why it doesn't just use its classnames like everyone
        // else...
        props.put("runtime.log.logsystem.log4j.category", "org.apache.velocity");

        try {
            m_velocityEngine.init(props);
        } catch (Exception ex) {
            m_logger.error("Unable to initialize Velocity engine", ex);
        }

        // Load the templates
        try {
            m_htmlTemplate = m_velocityEngine.getTemplate(HTML_TEMPLATE_NAME);
        } catch (Exception ex) {
            m_logger.error("Unable to load templates", ex);
        }
    }

    /**
     * Create message body.
     * @param  serverHost Servr address.
     * @param  to         To address.
     * @param  atm        AuthTokenManager.
     * @param  i18nUtil   I18nUtil for translation.
     * @return            Message body.
     */
    String generateMsgBody(String serverHost, String to, AuthTokenManager atm, I18nUtil i18nUtil)
    {

        try {
            // Create the auth token
            String authToken = atm.createAuthToken(to.trim());

            // Create the Velocity context, for template generation
            VelocityContext context = new VelocityContext();
            context.put(USER_EMAIL_VV, to);
            context.put(INBOX_LINK_VV, new LinkGenerator(serverHost, authToken).generateInboxLink());
            context.put(I18N_UTIL_VV, i18nUtil);

            byte[] htmlBytes = mergeTemplate(context, m_htmlTemplate, m_htmlTemplateLen);

            return new String(htmlBytes);
        } catch (Exception e) {
            m_logger.warn("Exception attempting to generate digest email", e);
            return null;
        }
    }

    /**
     * Merge template.
     * @param  context  VelocityContext.
     * @param  template Template to merge
     * @param  estSize  Estimated size.
     * @return          Array of bytes.
     */
    private byte[] mergeTemplate(VelocityContext context, Template template, int estSize)
    {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(estSize);
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            template.merge(context, writer);
            writer.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            m_logger.error("Unable to merge template", ex);
            return null;
        }
    }
}
