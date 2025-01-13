package com.untangle.app.dynamic_lists;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DynamicListsManager
 */
@SuppressWarnings("serial")
public class DynamicListsManager {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final DynamicListsApp app;

    /**
     * Constructor
     *
     * @param app The Dynamic Lists application
     */
    public DynamicListsManager(DynamicListsApp app) { this.app = app; }
}
