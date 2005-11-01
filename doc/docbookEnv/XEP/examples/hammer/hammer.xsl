<?xml version='1.0' encoding='ISO-8859-1'?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:rx="http://www.renderx.com/XSL/Extensions"
                version="1.0">

<xsl:output method="xml"
            version="1.0"
            encoding="ISO-8859-1"/>

<!-- This file makes part of an XSL Test Suite            -->
<!-- Hammer instruction: Test for complex layout patterns -->
<!-- Version: 1.3 of 22/10/2000                           -->
<!-- "Rich" stylesheet: generates a two-column text.      -->
<!-- Footnotes at the bottom of the page, figures at the  -->
<!-- top of the page, table of contents, keywords index.  -->


<!-- **************************************************** -->
<!-- Auxiliary templates: figure and footnote numbering   -->

<xsl:template name="insert-figure-ref">
  <xsl:number level="any" count="//figure" format="1"/>
</xsl:template>

<xsl:template name="insert-footnote-ref">
  <xsl:number level="any" count="//footnote" format="1"/>
</xsl:template>

<xsl:template name="copyright">
  <fo:block text-align="start" 
            font="10pt Times">&#169; RenderX, 1999&#8211;2001 </fo:block>
</xsl:template>


<!-- **************************************************** -->
<!-- Auxiliary template: insert figures after the         -->
<!-- embracing block element                              -->
<xsl:template name="draw-figures">
  <xsl:for-each select="figure">
    <fo:block margin="3pt"
              border="thin ridge silver"
              padding="3pt"
              space-before.optimum="6pt"
              space-after.optimum="6pt"
              text-align="center"
              font-style="italic"
              font-family="Helvetica"
              keep-together.within-column="always">
    <fo:block text-align="center">
     <fo:external-graphic src="url('{@src}')"/>
    </fo:block>
      Fig. <xsl:call-template name="insert-figure-ref"/>.
      <xsl:value-of select="@figure-name"/>
    </fo:block>
  </xsl:for-each>
</xsl:template>


<!-- **************************************************** -->
<!-- Topmost template                                     -->

<xsl:template match="manual">
  <fo:root>
    <fo:layout-master-set>

      <!-- First page: used for the title -->       
      <fo:simple-page-master master-name="FirstPage">
        <fo:region-body display-align="center"
            border="medium ridge silver"
            margin="1in"
            padding="0.4in"/>
        <fo:region-after  reference-orientation="0"
            extent="0.8in"
            display-align="before"
            padding="0in 1in"/>
      </fo:simple-page-master>

      <!-- Blank page: used for the annotation -->
      <fo:simple-page-master master-name="BlankPage">
        <fo:region-body display-align="center" margin="1in"/>
        <fo:region-after
              extent="0.8in"
              display-align="before"
              padding="0in 1in"/>
      </fo:simple-page-master>

      <fo:simple-page-master   
            master-name="TwoColumns">
        <fo:region-body  
            column-count="2"
            column-gap="0.2in"
            border-top="thin solid silver"
            border-bottom="thin solid silver"
            margin="0.9in 0.4in"/>
        <fo:region-before
            extent="0.8in"
            display-align="after"
            padding="0in 0.4in 3pt"/>
        <fo:region-after 
            extent="0.8in"
            display-align="before"
            padding="0.1in 0.4in 0in"/>
      </fo:simple-page-master>

      <fo:page-sequence-master master-name="Repeat-TwoColumns">
        <fo:repeatable-page-master-reference master-reference="TwoColumns"/>
      </fo:page-sequence-master>

    </fo:layout-master-set>

    <!-- If the document has a header, print it on a separate page -->
    <xsl:apply-templates select="header"/>

    <!-- Same for the annotation -->
    <xsl:apply-templates select="annotation"/>

  <!-- Main pages contain chapters -->

  <fo:page-sequence master-reference="Repeat-TwoColumns">
    <!-- Header -->
    <fo:static-content flow-name="xsl-region-before">
      <fo:list-block provisional-distance-between-starts="4.5in"
                     provisional-label-separation="0pt"
                     font="11pt Times">
        <fo:list-item>
        <fo:list-item-label end-indent="label-end()">
          <fo:block text-align="start" 
                    font-weight="bold">
            <xsl:value-of select="@header"/>
          </fo:block>
        </fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block text-align="end" 
                    font-weight="bold">
             Page <fo:page-number/>
          </fo:block>
        </fo:list-item-body>

        </fo:list-item>
      </fo:list-block>
    </fo:static-content>

    <fo:static-content flow-name="xsl-region-after">
      <xsl:call-template name="copyright"/>
    </fo:static-content>

    <fo:static-content flow-name="xsl-footnote-separator">
      <fo:block>
        <fo:leader leader-pattern="rule"
                   rule-style="solid"
                   rule-thickness="0.5pt"
                   leader-length="100%"/>
      </fo:block>
    </fo:static-content>

    <!-- Chapters, table of contents, and keyword index-->
    <fo:flow flow-name="xsl-region-body">
      <xsl:apply-templates select="toc|chapter|index"/>
    </fo:flow>
  </fo:page-sequence>

  </fo:root>
</xsl:template>


<!-- **************************************************** -->
<!-- Chapters, subchapters, paragraphs                    -->

<xsl:template match="chapter">
  <fo:block id="{generate-id()}"
            text-align="justify" 
            font="11pt Times"
            line-height="1.3"
            space-before.minimum="18pt"
            space-before.conditionality="retain">

    <xsl:apply-templates/>
  </fo:block>
  <xsl:call-template name="draw-figures"/>
</xsl:template>


<xsl:template match="subchapter">
  <fo:block id="{generate-id()}"
            space-before.minimum="12pt" 
            space-before.conditionality="discard">
    <xsl:apply-templates/>
  </fo:block>
  <xsl:call-template name="draw-figures"/>
</xsl:template>


<xsl:template match="p">
  <fo:block space-before.optimum="6pt">
    <xsl:apply-templates/>
  </fo:block>
  <xsl:call-template name="draw-figures"/>
</xsl:template>

<xsl:template match="line">
  <fo:block>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>


<!-- **************************************************** -->
<!-- Special paragraph types                              -->

<!-- Annotation makes a separate page sequence -->
<xsl:template match="annotation">
  <fo:page-sequence master-reference="BlankPage">

    <fo:static-content flow-name="xsl-region-after">
      <xsl:call-template name="copyright"/>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body">
      <fo:block font="italic 11pt Times"
            border="thin solid gray"
            padding="0.1in"
            text-align="justify">
       <xsl:apply-templates/>
      </fo:block>
    </fo:flow>
  </fo:page-sequence>
</xsl:template>

<xsl:template match="disclaimer">
  <fo:block font-weight="bold" 
            space-before.optimum="12pt" 
            space-after.optimum="12pt"
            padding="0.1in"
            border="thin solid black">
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<!-- **************************************************** -->
<!-- Font properties: bold, italic, underlined, etc.      -->

<xsl:template match="b">
  <fo:wrapper font-weight="bold">
    <xsl:apply-templates/>
  </fo:wrapper>
</xsl:template>

<xsl:template match="i">
  <fo:wrapper font-style="italic">
    <xsl:apply-templates/>
  </fo:wrapper>
</xsl:template>

<xsl:template match="u">
  <fo:inline text-decoration="underline">
    <xsl:apply-templates/>
  </fo:inline>
</xsl:template>


<!-- **************************************************** -->
<!-- Figures are numbered through the whole text. This    -->
<!-- template gives only a reference to the figure; the   -->
<!-- figure itself will be drawn by an explicit call to   -->
<!-- draw-figures template                                -->

<xsl:template match="figure">
  (see Fig. <xsl:call-template name="insert-figure-ref"/>)
</xsl:template>


<!-- **************************************************** -->
<!-- Templates for titles                                 -->

<!-- Document title forms a separate page sequence -->
<xsl:template match="manual/header">
  <fo:page-sequence master-reference="FirstPage" text-align="center">
    <fo:static-content flow-name="xsl-region-after">
      <xsl:call-template name="copyright"/>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body">
      <fo:block font="16pt Helvetica">
        <xsl:apply-templates select="manufacturer"/>
      </fo:block>
      <fo:block font="bold 30pt Helvetica"
                space-before="1in">
        <xsl:apply-templates select="title"/>
      </fo:block>
      <fo:block font="italic 18pt Helvetica"
                space-before="24pt">
        <xsl:apply-templates select="subtitle"/>
      </fo:block>
    </fo:flow>
  </fo:page-sequence>
</xsl:template>

<xsl:template match="chapter/title">
  <fo:block font="bold 14pt Helvetica" 
            keep-together.within-column="always"
            keep-with-next.within-column="always"
            space-before.minimum="6pt"
            space-before.optimum="12pt"
            space-before.conditionality="retain"
            space-after.optimum="3pt"           
            background-color="silver"
            padding="3pt"
            border-top="thin solid black"
            border-bottom="thin solid black">
      <xsl:number level="multiple" 
                  count="chapter" 
                  from="manual" 
                  format="A. "/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="subchapter/title">
  <fo:block font="bold 12pt Helvetica" 
            keep-together.within-column="always"
            keep-with-next.within-column="always"
            space-before.minimum="3pt"
            space-before.optimum="6pt"
            space-before.conditionality="retain"
            space-after.optimum="3pt"           
            padding="3pt"
            border-top="thin solid black"
            border-bottom="thin solid black">
    <xsl:number level="multiple" 
                count="chapter|subchapter" 
                from="manual" 
                format="A.1. "/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>



<!-- **************************************************** -->
<!-- Lists. The ordered and unordered lists are treated   -->
<!-- the same, the difference will appear on item level.  -->

<xsl:template match="ul|ol">
  <fo:list-block provisional-distance-between-starts="18pt"
                 provisional-label-separation="3pt"
                 space-before.optimum="9pt"
                 space-after.optimum="6pt" >
    <xsl:apply-templates/>
  </fo:list-block>
</xsl:template>


<xsl:template match="imagelist">
  <fo:list-block provisional-distance-between-starts="1.9in"
                 provisional-label-separation="0in"
                 space-before.optimum="9pt"
                 space-after.optimum="6pt" >
    <xsl:apply-templates/>
  </fo:list-block>
</xsl:template>


<!-- List item for an unordered list: if no bullet is specified, uses '-' -->
<xsl:template match="ul/li">
  <fo:list-item space-before.optimum="6pt">
    <fo:list-item-label end-indent="label-end()">
      <fo:block>
        <xsl:choose>
          <xsl:when test="../@bullet">
            <xsl:value-of select="../@bullet"/>
          </xsl:when>
          <xsl:otherwise>-</xsl:otherwise>
        </xsl:choose>
      </fo:block>
    </fo:list-item-label>

    <fo:list-item-body start-indent="body-start()">
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>

<!-- List item for an ordered list: default format is 1. -->
<xsl:template match="ol/li">
  <fo:list-item space-before.optimum="6pt">
    <fo:list-item-label end-indent="label-end()">
      <fo:block>
        <xsl:choose>
          <xsl:when test="../@number-format">
            <xsl:number level='single' 
                        count="li" 
                        format="../@number-format"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:number level='single' 
                        count="li" 
                        format="1. "/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:block>
    </fo:list-item-label>

    <fo:list-item-body start-indent="body-start()">
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>

<!-- List item for an image list -->
<xsl:template match="imagelist/li">
  <fo:list-item keep-together.within-column="always"
                display-align="center">
    <fo:list-item-label end-indent="label-end()">
    <fo:block text-align="center">
     <fo:external-graphic src="url({@img})"/>
    </fo:block>
    </fo:list-item-label>

    <fo:list-item-body start-indent="body-start()">
      <fo:block space-before.optimum="12pt"
                space-after.optimum="12pt">
        <xsl:apply-templates/>
      </fo:block>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>



<!-- **************************************************** -->
<!-- Keyword -->

<xsl:template match="keyword">
  <fo:wrapper font-style="italic"
                      color="blue"
                      rx:key="{string()}">
    <xsl:apply-templates/>
  </fo:wrapper>
</xsl:template>


<!-- **************************************************** -->
<!-- Footnote. Footnote is placed to the bottom-of-page. -->
<!-- Footnotes are numbered throughout the whole document -->

<xsl:template match="footnote">
  <fo:footnote>
    <fo:inline baseline-shift="super" font-size="75%"
               keep-together.within-line="always">
      <xsl:number level="any" 
                  count="//footnote" 
                  format="(1)"/>
    </fo:inline>

    <fo:footnote-body>
      <fo:list-block provisional-distance-between-starts="15pt"
                     provisional-label-separation="2pt"
                     space-before="6pt"
                     space-before.conditionality="discard"
                     space-after="6pt"
                     space-after.conditionality="retain"
                     line-height="1.2"
                     font="9pt Times">
        <fo:list-item>

          <fo:list-item-label end-indent="label-end()">
            <fo:block>
              <fo:wrapper font-size="75%">
                <xsl:number level="any" 
                            count="//footnote" 
                            format="(1)"/>
              </fo:wrapper>
            </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
            <fo:block padding-top="4pt"><xsl:apply-templates/></fo:block>
          </fo:list-item-body>
        </fo:list-item>
      </fo:list-block>
    </fo:footnote-body>
  </fo:footnote>
</xsl:template>


<!-- **************************************************** -->
<!-- Troubleshooting. In this version, it is rendered by  -->
<!-- means of an fo:table.                                -->

<xsl:template match="troubleshooting">
  <fo:table space-before.optimum="6pt"
            space-after.optimum="6pt"
            border="1pt solid black"
            border-collapse="collapse"
            padding="2pt"
            font-size="9pt">

    <fo:table-header text-align="center" 
                     font-weight="bold"
                     font-style="italic">
      <fo:table-cell border="0.5pt solid silver"
                     padding="3pt">
        <fo:block>Problem</fo:block>
      </fo:table-cell>
      <fo:table-cell border="0.5pt solid silver"
                     padding="3pt">
        <fo:block>Cause  </fo:block>
      </fo:table-cell>
      <fo:table-cell border="0.5pt solid silver"
                     padding="3pt" 
                     ends-row="true">
        <fo:block>Remedy </fo:block>
      </fo:table-cell>
    </fo:table-header>

    <fo:table-body>
      <xsl:apply-templates/>
    </fo:table-body>
  </fo:table>
</xsl:template>

<!-- A series of possible causes and suggested remedies -->

<xsl:template match="problem">
  <fo:table-cell padding="3pt" 
                 border="0.5pt solid silver"
                 number-rows-spanned="{count(..//remedy)}">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </fo:table-cell>
</xsl:template>


<xsl:template match="cause">
  <fo:table-cell padding="3pt" 
                 border="0.5pt solid silver"
                 number-rows-spanned="{count(../remedy)}">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </fo:table-cell>
</xsl:template>


<xsl:template match="remedy">
  <fo:table-cell padding="3pt" 
                 border="0.5pt solid silver"
                 ends-row="true">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </fo:table-cell>
</xsl:template>



<!-- **************************************************** -->
<!-- Specification. In this version, it is rendered by    -->
<!-- means of an fo:table.                                -->
<xsl:template match="specifications">
  <fo:table space-before.optimum="6pt"
            space-after.optimum="6pt"
            border="1pt solid black"
            font-size="9pt">
    <fo:table-column column-width="0.8in"/>
    <fo:table-column/>
    <fo:table-body>
      <xsl:apply-templates/>
    </fo:table-body>
  </fo:table>
</xsl:template>


<xsl:template match="specifications/item">
  <fo:table-cell text-align="end" 
                 font-weight="bold"
                 padding="3pt">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </fo:table-cell>
</xsl:template>

<xsl:template match="specifications/value">
  <fo:table-cell text-align="start"
                 padding="3pt"
                 ends-row="true">
    <fo:block>
       <xsl:apply-templates/>
    </fo:block>
  </fo:table-cell>
</xsl:template>


<!-- **************************************************** -->
<!-- TOC and index -->

<xsl:template match="toc">
  <fo:block font="bold 14pt Helvetica" 
            keep-together.within-column="always"
            keep-with-next.within-column="always"
            space-before.minimum="6pt"
            space-before.optimum="12pt"
            space-before.conditionality="retain"
            space-after.optimum="3pt"           
            background-color="silver"
            padding="3pt"
            border-top="thin solid black"
            border-bottom="thin solid black">
    Table of Contents
  </fo:block>

  <xsl:for-each select="//chapter">
    <fo:block font="11pt Times"
              space-before.optimum="6pt"
              text-align-last="justify">
      Chapter
      <xsl:number level="any" format="A. " count="//chapter"/>

      <fo:wrapper text-transform="uppercase">
        <xsl:value-of select="title"/>
      </fo:wrapper>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>

    <xsl:for-each select="subchapter">
      <fo:block margin-left="0.5in"
                font="11pt Times"
                text-align-last="justify">
        Subchapter
        <xsl:number level="multiple" 
                    count="chapter|subchapter" 
                    from="manual" 
                    format="A.1. "/>
        <xsl:value-of select="./title"/>
        <fo:leader leader-pattern="dots"/>
        <fo:page-number-citation ref-id="{generate-id()}"/>
      </fo:block>
    </xsl:for-each>
  </xsl:for-each>

  <xsl:for-each select="//index">
    <fo:block font="11pt Times" space-before.optimum="6pt">
      <fo:wrapper text-transform="uppercase">
        Keyword Index
      </fo:wrapper>
    </fo:block>
  </xsl:for-each>

</xsl:template>


<xsl:template match="index">
  <fo:block font="bold 14pt Helvetica" 
            keep-together.within-column="always"
            keep-with-next.within-column="always"
            space-before.minimum="6pt"
            space-before.optimum="12pt"
            space-before.conditionality="retain"
            space-after.optimum="3pt"           
            background-color="silver"
            padding="3pt"
            border-top="thin solid black"
            border-bottom="thin solid black">
    Keyword Index
  </fo:block>

  <xsl:for-each select="//keyword">
    <xsl:sort select="." data-type="text" order="ascending"/>
    <fo:block font="11pt Times">
      <fo:wrapper text-transform="lowercase">
        <xsl:value-of select="."/>
      </fo:wrapper>
      <xsl:text> </xsl:text>
      <fo:wrapper font-style="italic" font-weight="bold">
      	<rx:page-index ref-key="{string()}"/>
      </fo:wrapper>
    </fo:block>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
