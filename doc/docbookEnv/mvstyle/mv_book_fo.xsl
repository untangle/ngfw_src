<?xml version="1.0" encoding="iso-8859-1"?>

<!-- >e-novative> DocBook Environment (eDE)                                  -->
<!-- (c) 2002 e-novative GmbH, Munich, Germany                               -->
<!-- http://www.e-novative.de                                                -->

<!-- Single HTML File Generation                                             -->

<!-- This file is part of eDE                                                -->

<!-- eDE is free software; you can redistribute it and/or modify             -->
<!-- it under the terms of the GNU General Public License as published by    -->
<!-- the Free Software Foundation; either version 2 of the License, or       -->
<!-- (at your option) any later version.                                     -->

<!-- eDE is distributed in the hope that it will be useful,                  -->
<!-- but WITHOUT ANY WARRANTY; without even the implied warranty of          -->
<!-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           -->
<!-- GNU General Public License for more details.                            -->

<!-- You should have received a copy of the GNU General Public License       -->
<!-- along with eDe; if not, write to the Free Software                      -->
<!-- Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
>

<xsl:import href="../docbook-xsl-1.69.1/fo/profile-docbook.xsl" />
<xsl:import href="mv_book.xsl" />
<xsl:import href="mv.xsl" />

<xsl:param name="body.fontset">Helvetica</xsl:param>
<xsl:param name="body.font.family">Helvetica</xsl:param>
<xsl:param name="title.fontset">Helvetica</xsl:param>
<xsl:param name="title.font.family">Helvetica</xsl:param>


<xsl:param name="admon.graphics">0</xsl:param>
<!--
<xsl:param name="hyphenate">0</xsl:param>
-->

<!-- title indentation/left margin -->
<!-- amf: This appears to control left margin of entire document -->
<!-- <xsl:param name="title.margin.left">1.25in</xsl:param> -->

<xsl:param name="page.margin.inner">
  <xsl:choose>
    <xsl:when test="$double.sided = 1">1in</xsl:when>
    <xsl:otherwise>0.75in</xsl:otherwise>
  </xsl:choose>
</xsl:param>

<!-- title indentation/left margin -->
<!-- <xsl:param name="title.margin.left">0px</xsl:param> -->

<!-- <xsl:import href="metavize-afolmsbee-fo.xsl" /> -->



<!--
<xsl:param name="body.font.family">Times</xsl:param>
<xsl:param name="body.font.master">12</xsl:param>
<xsl:param name="body.font.size">12</xsl:param>
-->
<!--
<xsl:param name="body.font.family">Verdana</xsl:param>
<xsl:param name="body.font.master">9</xsl:param>
<xsl:param name="body.font.size">9</xsl:param>
-->


<!-- 
      try to kill the blank page after the title page 
--><!--
<xsl:template name="book.titlepage.separator">
</xsl:template>
-->

<!--
<xsl:template name="header.content">
 <xsl:text>A Book</xsl:text>
</xsl:template>
-->

<xsl:param name="double.sided">1</xsl:param>
<xsl:param name="fop.extensions">1</xsl:param>
<!-- <xsl:param name="use.extensions">1</xsl:param> -->
<xsl:param name="use.extensions" select="'1'"></xsl:param>
<xsl:param name="tablecolumns.extension">1</xsl:param>

<!-- ==================================== -->
<!-- testing table of contents formatting -->
<!-- ==================================== -->

<xsl:template name="toc.line">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="label">
    <xsl:apply-templates select="." mode="label.markup"/>
  </xsl:variable>

  <fo:block text-align-last="justify"
            end-indent="{$toc.indent.width}pt"
            last-line-end-indent="-{$toc.indent.width}pt">
    <xsl:attribute name="font-family">Times</xsl:attribute>
    <fo:inline keep-with-next.within-line="always">
       <xsl:choose>
        <xsl:when test="local-name(.) = 'chapter'">
          <xsl:attribute name="font-weight">bold</xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <fo:basic-link internal-destination="{$id}">
        <xsl:if test="$label != ''">
          <xsl:copy-of select="$label"/>
          <xsl:value-of select="$autotoc.label.separator"/>
        </xsl:if>
        <xsl:apply-templates select="." mode="title.markup"/>
      </fo:basic-link>
    </fo:inline>
    <fo:inline keep-together.within-line="always">
      <xsl:text> </xsl:text>
      <fo:leader leader-pattern="dots"
                 leader-pattern-width="3pt"
                 leader-alignment="reference-area"
                 keep-with-next.within-line="always"/>
      <xsl:text> </xsl:text>
      <fo:basic-link internal-destination="{$id}">
        <fo:page-number-citation ref-id="{$id}"/>
      </fo:basic-link>
    </fo:inline>
  </fo:block>
</xsl:template>
<!-- ==================================== -->
<!-- ==================================== -->
<!-- ==================================== -->


<!--
<xsl:template name="toc.line">
 <xsl:attribute name="font-family">Times</xsl:attribute>
</xsl:template>
-->

<!--
<xsl:template match="article">
  <xsl:variable name="column.count.body">
    <xsl:value-of select="2"/>
  </xsl:variable>
</xsl:template>
-->

<xsl:template name="user.footer.content"></xsl:template>

<xsl:template name="footer.content">
<xsl:param name="pageclass" select="''"/>
<xsl:param name="sequence" select="''"/>
<xsl:param name="position" select="''"/>
<xsl:param name="gentext-key" select="''"/>

<xsl:variable name="DistInfo">
 <xsl:choose>
  <xsl:when test="/book[@security='internal']">
    <xsl:text>Metavize Internal</xsl:text>
  </xsl:when>
  <xsl:when test="/article[@security='internal']">
    <xsl:text>Metavize Internal</xsl:text>
  </xsl:when>
  <xsl:otherwise>
     <xsl:text>Public</xsl:text>
  </xsl:otherwise>
 </xsl:choose>
</xsl:variable>

       <xsl:variable name="RevInfo">
         <xsl:choose>
            <xsl:when test="//revhistory/revision[1]/revnumber">
                <xsl:text>Rev </xsl:text>
                <xsl:value-of select="//revhistory/revision[1]/revnumber"/>
            </xsl:when>
            <xsl:otherwise>
      <!-- nop -->
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

       <xsl:choose>
         <xsl:when test="$sequence='blank'">
          <xsl:choose>
            <xsl:when test="$double.sided != 0 and $position = 'left'">
              <xsl:value-of select="$RevInfo"/>
            </xsl:when>

            <xsl:when test="$double.sided = 0 and $position = 'center'">
              <xsl:value-of select="$DistInfo"/>
            </xsl:when>

            <xsl:otherwise>
              <xsl:value-of select="$DistInfo"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>

        <xsl:when test="$double.sided = 1">
          <xsl:choose>
              <xsl:when test="$sequence='even'" >
              <xsl:choose>
                <xsl:when test="$position='left'">
                  <fo:page-number/>
                </xsl:when>
                <xsl:when test="$position='right'">
                   <xsl:value-of select="$RevInfo"/>
                </xsl:when>
                <xsl:otherwise>
             <xsl:value-of select="$DistInfo"/>
                      </xsl:otherwise>
              </xsl:choose>
              </xsl:when>
              <xsl:when test="$sequence='odd' or $sequence='first'" >
                <xsl:choose>
                  <xsl:when test="$position='left'">
                    <xsl:value-of select="$RevInfo"/>
                  </xsl:when>
                  <xsl:when test="$position='right'">
                    <fo:page-number/>
                  </xsl:when>
                  <xsl:otherwise>
        <xsl:value-of select="$DistInfo"/>
                  </xsl:otherwise>
               </xsl:choose>
              </xsl:when>      
              <xsl:otherwise>
                 <xsl:text>Double sided</xsl:text>
                 <xsl:value-of select="$sequence"/>
              </xsl:otherwise>
          </xsl:choose>          
        </xsl:when>

        <xsl:when test="$double.sided != 1">
           <xsl:choose>
         <xsl:when test="$position='left'">
            <xsl:value-of select="$RevInfo"/>
         </xsl:when>
         <xsl:when test="$position='right'">
            <fo:page-number/>
         </xsl:when>
           </xsl:choose>
        </xsl:when>


        <xsl:otherwise>
          <xsl:value-of select="$position"/>
          <xsl:value-of select="$sequence"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>




<xsl:param name="body.font.family">Helvetica</xsl:param>
<xsl:param name="body.font.master">8</xsl:param>

</xsl:stylesheet>