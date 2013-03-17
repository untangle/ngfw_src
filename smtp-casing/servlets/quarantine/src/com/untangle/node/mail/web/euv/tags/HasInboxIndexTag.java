/**
 * $Id$
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
