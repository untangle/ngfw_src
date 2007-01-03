/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.util;

import java.util.*;

public interface ExpNode
{
    /* by definition,
     * all variables declared in an interface are implicitly static and final
     */

    /* constants */
    /* value types */
    int UNKNOWN_VAL = 0x00;
    int BOOL_VAL = 0x01;
    int INT_VAL = 0x02;
    int STRLIT_VAL = 0x04;
    int VAR_VAL = 0x08; /* value may be bool, integer, or string literal */

    /* node types */
    int UNKNOWN_NODE = 0x00;
    int BINARYOPT_NODE = 0x11;
    int UNARYOPT_NODE = 0x12;
    int CONST_NODE = 0x14;
    int VAR_NODE = 0x18;

    /* print tree orientation codes */
    char TOPNODE_PR = 'T';
    char LEFTNODE_PR = 'L';
    char RIGHTNODE_PR = 'R';

    /* print request codes */
    int PREFIX_PR = 0;
    int INFIX_PR = 1;
    int POSTFIX_PR = 2;

    /* print object types */
    String UNKNOWN_STR = "null";
    String BOOL_STR = "bool";
    String INT_STR = "int";
    String STRLIT_STR = "str lit";

    /* class variables */

    /* instance variables */

    /* constructors */

    /* public methods */
    public Object eval(Hashtable zEnv) throws EvalException;

    public Object findConstValue(VarNode zVarNode);

    public int getValType();

    public int getNodeType();

    public String toValTypeString(Hashtable zEnv); /* utility */

    public void dump(Hashtable zEnv, int iIndent, char cNodePos); /* utility */

    public void dump(Hashtable zEnv, int iNodeFormat); /* utility */

    /* private methods */
}
