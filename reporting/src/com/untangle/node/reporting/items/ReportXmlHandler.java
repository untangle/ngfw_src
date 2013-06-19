/*
 * $Id: ReportXmlHandler.java,v 1.00 2011/12/17 12:31:52 dmorris Exp $
 */
package com.untangle.node.reporting.items;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler for uvm-node.xml files.
 */
public class ReportXmlHandler extends DefaultHandler
{
    private String reportName;
    private String reportTitle;

    private ApplicationData currentReport;
    private SummarySection currentSummary;
    private Chart currentChart;
    private Plot currentPlot;
    private Highlight currentHighlight;
    private DetailSection currentDetailSection;

    private StringBuilder sqlBuilder;

    private final List<Section> sections = new ArrayList<Section>();

    private final Logger logger = Logger.getLogger(getClass());

    public ReportXmlHandler() { }

    // public methods ---------------------------------------------------------

    public ApplicationData getReport()
    {
        return currentReport;
    }

    // DefaultHandler methods -------------------------------------------------

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attrs)
        throws SAXException
    {
        if (qName.equals("report")) {
            this.reportName = attrs.getValue("name");
            this.reportTitle = attrs.getValue("title");
        } else if (qName.equals("summary-section")) {
            currentSummary = new SummarySection(attrs.getValue("name"),
                                                attrs.getValue("title"));
            sections.add(currentSummary);
        } else if (qName.equals("graph")) {
            if (null == currentSummary) {
                logger.warn("no currentSummary for chart");
            } else {
                currentChart = new Chart(attrs.getValue("name"),
                                         attrs.getValue("title"),
                                         attrs.getValue("type"),
                                         attrs.getValue("image"),
                                         attrs.getValue("csv"));
                currentSummary.addSummaryItem(currentChart);
            }
        } else if (qName.equals("key-statistic")) {
            if (null == currentChart) {
                logger.warn("no currentChart for key-statistic");
            } else {
                KeyStatistic ks = new KeyStatistic(attrs.getValue("name"),
                                                   attrs.getValue("value"),
                                                   attrs.getValue("unit"),
                                                   attrs.getValue("link-type"));
                currentChart.addKeyStatistic(ks);
            }
        } else if (qName.equals("highlight")) {
            if (null == currentSummary) {
                logger.warn("no currentSummary for highlight");
            } else {
                currentHighlight = new Highlight(attrs.getValue("name"),
                         attrs.getValue("string-template"));
                currentSummary.addSummaryItem(currentHighlight);
            }
        } else if (qName.equals("highlight-value")) {
            if (null == currentHighlight) {
                logger.warn("no currentHighlight for highlight-value");
            } else {
                currentHighlight.addValue(attrs.getValue("name"),
                      attrs.getValue("value"));
//         logger.info(currentHighlight);
            }
        } else if (qName.equals("plot")) {
            currentPlot = null;

            String type = attrs.getValue("type");
            String title = attrs.getValue("title");
            String xLabel = attrs.getValue("x-label");
            String yLabel = attrs.getValue("y-label");
            String majorFormatter = attrs.getValue("major-formatter");
            String displayLimit = attrs.getValue("display-limit");
            String yAxisLowerBound = attrs.getValue("y-axis-lower-bound");

            if (null == type) {
                logger.warn("null plot type");
            }
            if (type.equals("time-series-chart")) {
                currentPlot = new TimeSeriesChart(title, xLabel, yLabel,
                                                  majorFormatter, 
                                                  yAxisLowerBound);
            } else if (type.equals("stacked-bar-chart")) {
                currentPlot = new StackedBarChart(title, xLabel, yLabel,
                                                  majorFormatter,
                                                  yAxisLowerBound);
            } else if (type.equals("pie-chart")) {
                int dl = -1;

                if (displayLimit != null) {
                    try {
                        dl = Integer.valueOf(displayLimit);
                    } catch (NumberFormatException exn) {
                        logger.warn("Ignoring bad display-limit: "
                                    + displayLimit, exn);
                    }
                }

                currentPlot = new PieChart(title, xLabel, yLabel,
                                           majorFormatter, dl);
            } else {
                logger.warn("unknown plot: " + type);
            }

            if (null != currentPlot) {
                currentChart.setPlot(currentPlot);
            }

        } else if (qName.equals("detail-section")) {
            currentDetailSection = new DetailSection(attrs.getValue("name"),
                                                     attrs.getValue("title"));
            sections.add(currentDetailSection);
        } else if (qName.equals("sql")) {
            sqlBuilder = new StringBuilder();
        } else if (qName.equals("column")) {
            if (null == currentDetailSection) {
                logger.warn("no currentDetailSection for column");
            } else {
                ColumnDesc c = new ColumnDesc(attrs.getValue("name"),
                                              attrs.getValue("title"),
                                              attrs.getValue("type"));
                currentDetailSection.addColumn(c);
            }
        } else if (qName.equals("color")) {
            currentPlot.setColor(attrs.getValue("title"),
                                 attrs.getValue("value"));
        } else {
            logger.warn("ignoring unknown element: " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if (qName.equals("report")) {
            currentReport = new ApplicationData(reportName, reportTitle,
                                                sections);
        } else if (qName.equals("sql")) {
            if (currentDetailSection == null) {
                logger.warn("no currentDetailSection for sql");
            } else {
                currentDetailSection.setSql(sqlBuilder.toString());
                sqlBuilder = null;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
    {
        if (null != sqlBuilder) {
            sqlBuilder.append(ch, start, length);
        }
    }
}
