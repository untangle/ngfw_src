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

package com.untangle.buildutil;

import java.io.File;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JRException;

public class JRCompiler
{
    private static final String SUFFIX_JRXML  = ".jrxml";
    private static final String SUFFIX_JASPER = ".jasper";

    private final String destinationDirectory;
    private final List <String>files;

    public static void main( String args[] ) throws JRException, InvalidSuffixException
    {
        String destination = args[1];

        JRCompiler jr = parseArgs( args );

        /* Iterate all of the files and start compiling */
        jr.compileAllFiles();
    }

    /**
     * sourcePrefix: A prefix to strip of the source files in order to calculate the destination
     * destination: Directory where the source files should end up, the directory structure after
     *              sourcePrefix is maintained */
    public JRCompiler( String destinationDirectory, List<String>files )
    {
        this.destinationDirectory = destinationDirectory.trim();
        this.files = Collections.unmodifiableList( new LinkedList<String>( files ));
    }

    public void compileAllFiles() throws JRException, InvalidSuffixException
    {
        for ( String file : files ) compile( file );
    }
    
    public void compile( String src ) throws JRException, InvalidSuffixException
    {
        /* Make sure to use the correct compiler */
        System.setProperty( "jasper.reports.compiler.class",
                            "net.sf.jasperreports.engine.design.JRJdk13Compiler" );
        src = src.trim();
        
        File destination = getDestination( src );
        File parent = new File( destination.getParent());
        
        /* Make all of the parent directories */
        if ( parent != null && !parent.exists()) {
            System.out.println( "Making the directory: '" + parent + "'" );
            parent.mkdirs();
        }
        
        System.out.printf( "[JRXML->JASPER]: %s -> %s\n", src, destination );

        JasperCompileManager.compileReportToFile( src, destination.toString());
    }

    private File getDestination( String src ) throws InvalidSuffixException
    {
        String name = new File( src ).getName();
        /* Strip off the JRXML suffix and replace if with the JASPER suffix */
        if ( name.endsWith( SUFFIX_JRXML )) {
            name = name.replace( SUFFIX_JRXML, SUFFIX_JASPER );
        } else {
            throw new InvalidSuffixException( src );
        }
        
        return new File( this.destinationDirectory + "/" + name );
    }

    private static JRCompiler parseArgs( String args[] )
    {
        String destination = "";
        
        int i;
        for ( i = 0 ;  i < args.length ; i++ ) {
            String arg = args[i];
            /* Ignore all non-prefixed arguments that are not preceeded by a flag */
            if ( !arg.startsWith( "-" )) break;

            if ( arg.equals( "-o")) {
                destination = args[++i];
            } else {
                dieUsage();
            }
        }
        
        int numFiles = args.length - i;

        if ( numFiles == 0 ) dieUsage();

        String files[] = new String[numFiles];
        System.arraycopy( args, i, files, 0, numFiles );
        
        return new JRCompiler( destination, Arrays.asList( files ));
    }

    private static void dieUsage()
    {
        System.out.println( "USAGE: JRCompiler [-o <destination directory>] <file> [<file>]*" );
        System.exit( 1 );
    }

    static class InvalidSuffixException extends Exception
    {
        InvalidSuffixException( String file )
        {
            super( "The filename: " + file + " needs the JRXML(" + SUFFIX_JRXML + ") suffix " );
        }
    }
}
