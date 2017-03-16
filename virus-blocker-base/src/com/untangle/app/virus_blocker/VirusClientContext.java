/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.io.File;

public final class VirusClientContext
{
    private InputSettings iSettings;

    private volatile VirusScannerResult virusReport;

    public VirusClientContext(File msgFile, String host, int port)
    {
        iSettings = new InputSettings(msgFile, host, port);
        virusReport = null;
    }

    public File getMsgFile()
    {
        return iSettings.getMsgFile();
    }

    public String getHost()
    {
        return iSettings.getHost();
    }

    public int getPort()
    {
        return iSettings.getPort();
    }

    public void setResultError()
    {
        this.virusReport = VirusScannerResult.ERROR;
        return;
    }

    public void setResult( boolean clean, String virusName )
    {
        if (true == clean) {
            this.virusReport = VirusScannerResult.CLEAN;
        } else {
            this.virusReport = new VirusScannerResult( clean, virusName );
        }
        return;
    }

    public VirusScannerResult getResult()
    {
        return virusReport;
    }

    class InputSettings
    {
        private final File msgFile;
        private final String host;
        private final int port;

        public InputSettings(File msgFile, String host, int port) {
            this.msgFile = msgFile;
            this.host = host;
            this.port = port;
        }

        public File getMsgFile() {
            return msgFile;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
