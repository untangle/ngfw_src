/**
 * $Id$
 */
package com.untangle.app.dynamic_lists;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DynamicListsManager
 */
@SuppressWarnings("serial")
public class DynamicListsManager {

    private static final String DBL_SETUP_SCRIPT = System.getProperty("uvm.home") + "/bin/dbl-setup.sh";
    private static final String DBL_CLEAN_UP_SCRIPT = System.getProperty("uvm.home") + "/bin/dbl-cleanup.sh";

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final DynamicListsApp app;

    /**
     * Constructor
     *
     * @param app The Dynamic Lists application
     */
    public DynamicListsManager(DynamicListsApp app) { this.app = app; }

    /**
     * Start the dbl setup
     */
    protected void start() {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(DBL_SETUP_SCRIPT);
    }

    /**
     * Stop the filter chain
     */
    protected void stop() {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(DBL_CLEAN_UP_SCRIPT);
    }

    /**
     * Configure dynamic-block-lists-routes
     */
    protected void configure() {

    }
}
