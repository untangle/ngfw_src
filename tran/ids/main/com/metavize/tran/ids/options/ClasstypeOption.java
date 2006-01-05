package com.metavize.tran.ids.options;

import org.apache.log4j.Logger;

import com.metavize.tran.ids.IDSDetectionEngine;
import com.metavize.tran.ids.IDSRule;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSTransformImpl;
import com.metavize.tran.ids.RuleClassification;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.event.*;

public class ClasstypeOption extends IDSOption {
    private static final int HIGH_PRIORITY = 1;
    private static final int MEDIUM_PRIORITY = 2;
    private static final int LOW_PRIORITY = 3;
    private static final int INFORMATIONAL_PRIORITY = 4; // Super low priority

    private static final Logger logger = Logger.getLogger(ClasstypeOption.class);

    public ClasstypeOption(IDSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);

        IDSTransformImpl transform = (IDSTransformImpl)MvvmContextFactory.context().transformManager().threadContext().transform();
        IDSDetectionEngine engine = transform.getEngine();

        RuleClassification rc = engine.getClassification(params);
        if (rc == null) {
            logger.warn("Unable to find rule classification: " + params);
            // use default classification text for signature
        } else {
            signature.setClassification(rc.getDescription());

            if (true == initializeSettingsTime) {
                IDSRule rule = signature.rule();
                int priority = rc.getPriority();
                // logger.debug("Rule Priority for " + rule.getDescription() + " is " + priority);
                switch (priority) {
                case HIGH_PRIORITY:
                    rule.setLive(true);
                    rule.setLog(true);
                    break;
                case MEDIUM_PRIORITY:
                    rule.setLog(true);
                    break;
                default:
                    break;
                }
            }
        }
    }
}
