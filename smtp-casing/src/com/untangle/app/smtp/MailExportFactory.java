/**
 * $Id$
 */
package com.untangle.app.smtp;

import org.apache.log4j.Logger;

/**
 * Factory for the exported MailApp interface.
 * 
 */
public class MailExportFactory
{
    private static final MailExportFactory FACTORY = new MailExportFactory();

    private final Logger logger = Logger.getLogger(MailExportFactory.class);

    private MailExport export;

    /**
     * Initialize MailExportFactory instance.
     * @return MailExportFactory instance.
     */
    private MailExportFactory() {
    }

    /**
     * Initialize MailExportFactory.
     * @return FACTORY
     */
    public static MailExportFactory factory()
    {
        return FACTORY;
    }

    /**
     * Allows the casing to export its interface for its policy.
     * 
     * @param export
     *            exported interface.
     */
    public void registerExport(MailExport export)
    {
        synchronized (this) {
            if (this.export != null) {
                logger.warn("replacing export");
            }

            this.export = export;
        }
    }

    /**
     * Gets the exported interface for a given policy.
     * 
     * @return the exported interface.
     */
    public MailExport getExport()
    {
        synchronized (this) {
            return export;
        }
    }
}
