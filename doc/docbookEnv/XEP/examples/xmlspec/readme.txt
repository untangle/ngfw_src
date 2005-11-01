This directory contains an XSL stylesheet designed to transform 
W3C documents into XSL Formatting Objects, and three W3C documents
to test it.
 
FILES
=====

   readme.txt - this README file
   xmlspec20.xsl - the stylesheet for W3C specifications
   w3c.gif - W3C logo

XML Sources of some W3C specs (verbatim from W3C site):

   xml2e.xml - XML 1.0 Second Edition (W3C Recommendation)  
   xpath.xml - XPath 1.0 (W3C Recommendation), XML source
   xslt.xml - XSL Transformations 1.0 (W3C Recommendation)


THE STYLESHEET
==============

The stylesheet xmlspec20.xsl covers the majority of elements/constructs 
found in the XMLSpec DTD version 2.0, with one important exception: 
IDL-related markup from DOM Spec is omitted as it is left undocumented 
in the DTD description. 

We have tried to build a style for real documents, rather than for an 
abstract DTD. Therefore, the style also comprises elements specific to 
single documents (e.g. element syntax descriptors in XSLT Specification).
Moreover, in cases where real usage of an element in documents contradicted 
the formatting intent as specified in the XMLSpec DTD docs, we have privileged 
the real-life usage (see e.g. treatment of <slist> elements). 


Adjustable parameters
=====================

The stylesheet has four global parameters: 

title-color 
 
  Specifies the color to be used for all headers and the left sidebar. 

attr-color 
  
  Specifies the color to be used for all hyperlinks. 

lhs-width 
rhs-width 
  Control the width of the left-hand and right-hand columns in BNF productions, 
  respectively (see the description of scrap element in the XMLSpec DTD docs). 
  These parameters are chosen for single documents individually; their values
  for documents included in the test suite are given below. 


Building PDF/PS versions of W3C documents
=========================================

The stylesheet has been tested on several W3C documents; three of them are 
included here with XML sources and XSL FO formatted versions. The XML sources
are identical to those published on the W3C site except for the DOCTYPE
declaration (external DTD reference is removed for manageability).

The following values for parameters 'lhs-width' and 'rhs-width' have
been used to format each of the documents:

XML 1.0 Second Edition [xml2e.xml]
  lhs-width="1in" rhs-width="3.25in"

XPath 1.0 [xpath.xml]
  lhs-width="2in" rhs-width="3.5in"

XSLT 1.0 [xslt.xml]
  lhs-width="1.75in" rhs-width="3.75in"


This same stylesheet can be used to format the XSL 1.0 Recommendation. 
[In fact, the official PDF version of the Recommendation has been produced 
using a previous version of this same stylesheet]. We don't include this 
in the test suite due to its size. Recommended values for parameters are: 

XSL 1.0:
  lhs-width="1.5in" rhs-width="4in"

These are the default values in the stylesheet, so you need not supply 
any additional parameter to format the XSL Rec.(if you venture to 
generate a PDF for XSL 1.0 Rec, choose a powerful machine: you will 
need at least 150 MB of RAM.