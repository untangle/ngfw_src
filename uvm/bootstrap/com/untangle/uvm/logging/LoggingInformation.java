/**
 * $Id: LoggingInformation.java,v 1.00 2012/04/12 15:49:50 dmorris Exp $
 */
package com.untangle.uvm.logging;

public class LoggingInformation
{
    String configName = "log4j-node.xml";
    String fileName = "null";

    public LoggingInformation( String configName, String fileName )
    {
        this.configName = configName;
        this.fileName = fileName;
    }
    
    /**
     * Name of the log4j configuration file for this context. The
     * configuration file should be in the classpath and the
     * configName should be suitable for {@link ClassLoader.getResource()}.
     *
     * @return configuration file name.
     */
    String getConfigName() { return configName; }
    void setConfigName( String configName )  { this.configName = configName; }

    /**
     * The name of the log file for this logging context. This file
     * will be created in the directory specified in the system
     * property: <code>uvm.log.dir</code>.
     *
     * @return log filename.
     */
    String getFileName() { return fileName; }
    void setFileName( String fileName )  { this.fileName = fileName; }
}
