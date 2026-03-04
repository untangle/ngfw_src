/**
 * $Id$
 */
package com.untangle.app.dynamic_blocklists;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DynamicBlockListsManager
 */
@SuppressWarnings("serial")
public class DynamicBlockListsManager {

    private static final String DBL_SETUP_SCRIPT = System.getProperty("uvm.home") + "/bin/dbl-setup.sh";
    private static final String DBL_CLEAN_UP_SCRIPT = System.getProperty("uvm.home") + "/bin/dbl-cleanup.sh";

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final DynamicBlockListsApp app;

    /**
     * Constructor
     *
     * @param app The Dynamic Lists application
     */
    public DynamicBlockListsManager(DynamicBlockListsApp app) { this.app = app; }

    /**
     * Start the dbl setup
     */
    protected void start() {
        this.app.start();
        logger.info("Staring the Dynamic Blocklist Setup Process");
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(DBL_SETUP_SCRIPT); 
        logger.info("DBL setup script result: {}", result.getOutput());
    }

    /**
     * Stop the filter chain
     */
    protected void stop() {
        this.app.stop();
        logger.info("Staring the Dynamic Blocklist Cleanup Process");
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(DBL_CLEAN_UP_SCRIPT );
        logger.info("DBL cleanup script result: {}", result.getOutput());
    }

    /**
     * Configure dynamic-block-lists iptables
     */
    protected void configure() {
        UvmContextFactory.context().syncSettings().run(
                app.getSettingsFilename()
            );
    }
}
