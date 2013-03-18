/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.virus;

import java.io.File;
import java.util.Date;

import com.untangle.uvm.node.Scanner;

public interface VirusScanner extends Scanner
{
    /**
     * Scans the file for viruses, producing a virus report.  Note that the contract for this
     * requires that a report always be generated, for any problems or exceptions an
     * "clean" report is generated (and the error/warning should be logged).
     *
     * @param msgFile a <code>File</code> value
     * @return a <code>VirusScannerResult</code> value
     */
    VirusScannerResult scanFile(File msgFile);

    Date getLastSignatureUpdate();
}
