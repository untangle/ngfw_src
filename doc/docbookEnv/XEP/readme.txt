XEP - XSL Formatting Engine for Paged Media

Version 4.4 build 20050926
September 26th, 2005


CONTENTS

1. About the program
2. Installation
3. Deinstallation
4. Support
5. Contact info
6. Copyright notices


1. About the program

XEP  converts  XSL  Formatting  Objects  documents  into Adobe PDF or
PostScript format. The program is written in Java and requires a Java 
Virtual Machine.  Sun  JRE or JDK version 1.3 or later is recommended 
for best performance.

XEP  also runs under JDK/JRE 1.2. The performance will not be as good
as  with  the latest  versions of Java VM.  The software will not run
on older Java machines.


2. Installation

 A. Ensure you have a Java VM  installed.  If you have multiple  VMs 
    on your  computer, pick  one of them.  The  installation  wizard 
    configures XEP to run  on  the  same  Java VM  that was used for 
    setup; you can change this later. 
    
 B. Run the setup from the jar file, using a Java VM of your choice.
    You  can run  the jar  by typing  the following  command  on the
    prompt:
    
      java -jar setup-4.4-20050926-personal.jar
      
    You will be prompted for the  target  directory name.  Type  the 
    path in the edit box, or choose a directory by pressing "Browse" 
    button.  You  can  also automatically  activate XEP  by pointing
    installer to your license file (you can perform activation later
    if you don't have your license file at hand during setup).
    You can also  run setup in console mode by adding '-c' switch to
    the command line:

      java -jar setup-4.4-20050926-personal.jar -c
    
    During the installation system copies all files to the specified
    location  and sets up necessary configuration files. 

 C. Activate your copy of the software. This  step is necessary only
    if you choose to skip activation  during XEP setup. In this case
    you  should  activate  XEP manually  by placing  appropriate XML
    license  file in  the root  directory of XEP  installation. This
    file  could be  included  in XEP  distribution  or  send  to you
    separately  via e-mail  to the address  provided  during initial
    registration.

After activation, your XEP copy is ready to use. Please refer to the
PDF documentation, contained in the doc/ subdirectory of the  target
installation directory:

- intro.pdf           	  A Gentle Introduction to XEP, by Dave Pawson
- reference.pdf           XEP Reference Manual
- tutorial.pdf            a brief introduction into XSL FO

3. Deinstallation

The program places all its files into the target directory, and does
not modify any system settings.  To deinstall it, simply  remove the 
installation directory with all its contents.
    

4. Support

RenderX provides support by email.  Please send your questions,
opinions and requests for enhancements to support@renderx.com.

When describing a problem, please take care to include the following 
data to help us better understand your case:

   - hardware and software platform used to run XEP  (processor type 
     and OS version);
   - Java VM vendor and version;
   - as much detail about XEP configuration as you can provide.


5. Contact info

Please write to info@renderx.com on  general  business  topics.  All 
correspondence  concerning  commercial  issues  should  be  sent  to
sales@renderx.com.


6. Copyright notices

Copyright (c) 1999-2004 RenderX Corporation.  All rights reserved.
Patents pending.

This product includes program code by Sun Microsystems, Inc. 
Copyright (c) 1999-2001 Sun Microsystems, Inc. All rights reserved.
URL: http://www.sun.com/

This product includes the SAXON XSLT Processor from Michael Kay. 
Copyright (c) 1998-2004 Michael Kay. All rights reserved.
URL: http://saxon.sourceforge.net/

This product includes the XT XSLT Processor from James Clark. 
Copyright (c) 1998, 1999 James Clark. All rights reserved.
URL: http://www.jclark.com/xml/xt.html

Documentation for this product is created in DocBook and formatted 
with DocBook XSL stylesheets from the DocBook Open Repository.
URL: http://docbook.sourceforge.net/

This product includes software developed by the Apache 
Software Foundation (http://www.apache.org/).
Copyright (c) 2000 The Apache Software Foundation.  All rights reserved.
URL: http://www.apache.org/
