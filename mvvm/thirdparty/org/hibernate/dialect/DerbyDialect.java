//$Id: DerbyDialect.java,v 1.1 2004/12/15 01:53:37 amread Exp $
package org.hibernate.dialect;

import net.sf.hibernate.dialect.DB2Dialect;
import net.sf.hibernate.id.TableHiLoGenerator;
import net.sf.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;

/**
 * @author Simon Johnston
 *
 * Hibernate Dialect for Cloudscape 10 - aka Derby. This implements both an
 * override for the identity column generator as well as for the case statement
 * issue documented at:
 * http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
 */
public class DerbyDialect extends DB2Dialect {

    /**
     * This is different in Cloudscape to DB2.
     */
    public String getIdentityColumnString() {
        return "not null generated always as identity"; //$NON-NLS-1
    }

    /**
     * Return the case statement modified for Cloudscape.
     */
    public CaseFragment createCaseFragment() {
        return new DerbyCaseFragment();
    }

    public boolean dropConstraints() {
        return true;
    }

    public Class getNativeIdentifierGeneratorClass() {
        return TableHiLoGenerator.class;
    }

    public boolean supportsSequences() {
        return false;
    }

    public boolean supportsLimit() {
        return false;
    }

    public boolean supportsLimitOffset() {
        return false;
    }

}
