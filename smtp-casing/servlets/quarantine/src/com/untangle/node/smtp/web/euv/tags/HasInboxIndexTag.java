/**
 * $Id: HasInboxIndexTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;


/**
 * Includes/excludes body chunks if there
 * is an index
 */
@SuppressWarnings("serial")
public final class HasInboxIndexTag extends IfElseTag {

    @Override
    protected boolean isConditionTrue()
    {
        return QuarantineFunctions.hasCurrentIndex(pageContext.getRequest());
    }
}
