/**
 * $Id$
 */
package com.untangle.node.ftp;

import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ForkedEventHandler;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * FTP node implementation.
 */
public class FtpNodeImpl extends NodeBase implements FtpNode
{
    private final Logger logger = Logger.getLogger(FtpNodeImpl.class);

    private SessionEventHandler clientSideCtlHandler = new ForkedEventHandler( new FtpClientParserEventHandler(), new FtpUnparserEventHandler(true) );
    private SessionEventHandler serverSideCtlHandler = new ForkedEventHandler( new FtpUnparserEventHandler(false), new FtpServerParserEventHandler() );

    private SessionEventHandler clientSideDataHandler = new ForkedEventHandler( new FtpClientParserEventHandler(), new FtpUnparserEventHandler(true) );
    private SessionEventHandler serverSideDataHandler = new ForkedEventHandler( new FtpUnparserEventHandler(false), new FtpServerParserEventHandler() );
    
    private final PipelineConnector controlClientSideConnector = UvmContextFactory.context().pipelineFoundry().create( "ftp-control-client-side", this, null, clientSideCtlHandler, Fitting.FTP_CTL_STREAM, Fitting.FTP_CTL_TOKENS, null, null );
    private final PipelineConnector controlServerSideConnector = UvmContextFactory.context().pipelineFoundry().create( "ftp-control-server-side", this, null, serverSideCtlHandler, Fitting.FTP_CTL_TOKENS, Fitting.FTP_CTL_STREAM, null, null );
    private final PipelineConnector dataClientSideConnector = UvmContextFactory.context().pipelineFoundry().create( "ftp-data-client-side", this, null, clientSideDataHandler, Fitting.FTP_DATA_STREAM, Fitting.FTP_DATA_TOKENS, null, null );
    private final PipelineConnector dataServerSideConnector = UvmContextFactory.context().pipelineFoundry().create( "ftp-data-server-side", this, null, serverSideDataHandler, Fitting.FTP_DATA_TOKENS, Fitting.FTP_DATA_STREAM, null, null );
    private final PipelineConnector[] connectors = new PipelineConnector[] { controlClientSideConnector, controlServerSideConnector, dataClientSideConnector, dataServerSideConnector };

    private FtpSettings settings;

    // constructors -----------------------------------------------------------

    public FtpNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    // FtpNode methods ---------------------------------------------------

    public FtpSettings getFtpSettings()
    {
        return settings;
    }

    public void setFtpSettings(final FtpSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID + ".js";

        try {
            UvmContextFactory.context().settingsManager().save( settingsFile, newSettings );
        } catch(Exception exn) {
            logger.error("setFtpSettings()",exn);
            return;
        }

        this.settings = newSettings;
        
        reconfigure();
    }

    // Node methods ------------------------------------------------------

    public void reconfigure()
    {
        if ( settings != null ) {
            for ( PipelineConnector connector : this.connectors ) 
                connector.setEnabled( settings.isEnabled() );
        }
    }

    public void initializeSettings() { }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID + ".js";

        FtpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try {
            // first we try to read our json settings
            readSettings = UvmContextFactory.context().settingsManager().load( FtpSettings.class, settingsFile );
        } catch (Exception exn) {
            logger.error("postInit()",exn);
        }

        try {
            // still no settings found so init with defaults
            if (readSettings == null)
            {
                logger.warn("No settings found... initializing with defaults");
                setFtpSettings(new FtpSettings());
            }

            // otherwise apply the loaded or imported settings from the file
            else
            {
                logger.info("Loaded settings from " + settingsFile);
                this.settings = readSettings;
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings",exn);
        }
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void connectPipelineConnectors()
    {
        UvmContextFactory.context().pipelineFoundry().registerCasing( controlClientSideConnector, controlServerSideConnector );
        UvmContextFactory.context().pipelineFoundry().registerCasing( dataClientSideConnector, dataServerSideConnector );
    }

    @Override
    protected void disconnectPipelineConnectors()
    {
        UvmContextFactory.context().pipelineFoundry().deregisterCasing( controlClientSideConnector );
        UvmContextFactory.context().pipelineFoundry().deregisterCasing( dataClientSideConnector );
    }
    

    public Object getSettings()
    {
        return getFtpSettings();
    }

    public void setSettings(Object settings)
    {
        setFtpSettings((FtpSettings)settings);
    }
}
