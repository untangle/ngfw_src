/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, window, document, Highcharts
 */

/**
 * Highcharts for dashboard widgets
 */

Ext.define('Ung.dashboard.Charts', {
    singleton: true,

    cpuLoad1: function (widget) {
        return new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: widget.getEl().query('.chart1')[0],
                animation: Highcharts.svg, // don't animate in old IE
                marginTop: 25,
                marginRight: 10,
                marginLeft: 10,
                marginBottom: 20,
                backgroundColor: 'transparent'
            },
            credits: {
                enabled: false
            },
            title: null,
            xAxis: {
                type: 'datetime',
                labels: {
                    style: {
                        color: '#999',
                        fontSize: '10px'
                    }
                },
                crosshair: {
                    width: 1,
                    color: 'rgba(0,0,0,0.1)'
                },
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                minRange: 2,
                min: 0,
                tickAmount: 4,
                title: null,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    align: 'left',
                    style: {
                        color: '#999',
                        fontSize: '10px'
                    },
                    x: 0,
                    y: -3
                },
                /*
                 plotLines: [{
                 value: 2,
                 width: 1,
                 color: 'green',
                 dashStyle: 'solid'
                 }, {
                 value: 3,
                 width: 1,
                 color: 'rgba(255, 255, 0, 1)',
                 dashStyle: 'solid'
                 }, {
                 value: 6,
                 width: 1,
                 color: 'rgba(255, 0, 0, 1)',
                 dashStyle: 'solid'
                 }],
                 */
                /*
                plotBands: [{
                    color: '#FCFFC5',
                    from: 3,
                    to: 6
                }, {
                    color: 'rgba(255, 0, 0, 0.1)',
                    from: 6,
                    to: 100
                }],
                */
                visible: true
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.25,
                    lineWidth: 3
                },
                series: {
                    /*
                    point: {
                        events: {
                            mouseOver: function () {
                                //widget.chart2.series[0].points[0].update(this.y <= 7 ? this.y : 7, true);
                            }
                        }
                    },
                    */
                    marker: {
                        enabled: false,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 0,
                                radius: 3,
                                radiusPlus: 0
                            }
                        }
                    },
                    states: {
                        hover: {
                            enabled: true,
                            lineWidthPlus: 0,
                            halo: {
                                size: 0
                            }
                        }
                    }
                }
            },
            legend: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            tooltip: {
                enabled: false
            },
            series: [{
                lineWidth: 2,
                data: (function () {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;

                    for (i = -19; i <= 0; i += 1) {
                        data.push({
                            x: time + i * 3000,
                            y: 0
                        });
                    }
                    return data;
                }())
            }]
        });
    },

    cpuLoad2: function (widget) {
        return new Highcharts.Chart({
            chart: {
                type: 'gauge',
                renderTo: widget.getEl().query('.chart2')[0],
                height: 140,
                margin: [-20, 0, 0, 0],
                backgroundColor: 'transparent'
            },
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false
            },
            pane: [{
                startAngle: -45,
                endAngle: 45,
                background: null,
                center: ['50%', '135%'],
                size: 280
            }],

            tooltip: {
                enabled: false
            },

            yAxis: [{
                min: 0,
                max: 7,
                minorTickPosition: 'outside',
                tickPosition: 'outside',
                tickColor: '#555',
                minorTickColor: '#999',
                labels: {
                    rotation: 'auto',
                    distance: 20,
                    step: 1
                },
                plotBands: [{
                    from: 0,
                    to: 3,
                    color: 'rgba(112, 173, 112, 0.5)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 3,
                    to: 6,
                    color: 'rgba(255, 255, 0, 0.5)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 6,
                    to: 7,
                    color: 'rgba(255, 0, 0, 0.5)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }],
                title: null
            }],

            plotOptions: {
                gauge: {
                    dataLabels: {
                        enabled: false
                    },
                    dial: {
                        radius: '95%',
                        backgroundColor: '#555'
                    }
                }
            },


            series: [{
                data: [0],
                yAxis: 0
            }]

        });
    },

    timeChart: function (widget, d3) {

        var seriesRenderer, column, i;

        if (!widget.entry.timeDataColumns) {
            widget.entry.timeDataColumns = [];
        }
        /*
        if (!Ext.isEmpty(widget.entry.seriesRenderer)) {
            seriesRenderer = Ung.panel.Reports.getColumnRenderer(widget.entry.seriesRenderer);
        } else {
            console.log(widget.entry);
            seriesRenderer = widget.entry.seriesRenderer;
        }
        */

        var chartType, chartSeries = [];

        for (i = 0; i < widget.entry.timeDataColumns.length; i += 1) {
            column = widget.entry.timeDataColumns[i].split(' ').splice(-1)[0];
            chartSeries.push({
                id: column,
                name: column,
                data: [],
                pointPadding: 0.15 * i
            });
        }

        switch (widget.entry.timeStyle) {
        case 'LINE':
            chartType = 'spline';
            break;
        case 'AREA':
            chartType = 'areaspline';
            break;
        case 'BAR_3D_OVERLAPPED':
            chartType = 'column';
            break;
        default:
            chartType = 'areaspline';
        }

        return new Highcharts.Chart({
            chart: {
                type: chartType,
                zoomType: 'x',
                renderTo: widget.getEl().query('.chart')[0],
                //animation: Highcharts.svg, // don't animate in old IE
                //marginTop: 25,
                //marginRight: 10,
                //marginRight: 0,
                marginBottom: 50,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: d3,
                    alpha: 15,
                    beta: 15,
                    depth: 50,
                    viewDistance: 25
                }
            },
            //colors: widget.entry.colors ? widget.entry.colors : Highcharts.theme,
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false,
                buttons: {
                    contextButton: {
                        enabled: false
                    },
                    areaButton: {
                        text: 'Area',
                        onclick: function () {
                            this.series[0].update({type: 'areaspline'}, true, true);
                        }
                    },
                    lineButton: {
                        text: 'Line',
                        onclick: function () {
                            this.series[0].update({type: 'spline'}, true, true);
                        }
                    },
                    columnButton: {
                        text: 'Column',
                        onclick: function () {
                            this.series[0].update({type: 'column'}, true, true);
                        }
                    }
                }
            },
            navigation: {
                buttonOptions: {
                    align: 'left'
                }
            },
            xAxis: {
                type: 'datetime',
                crosshair: true,
                labels: {
                    style: {
                        color: '#999',
                        fontSize: '10px'
                    }
                },
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                allowDecimals: false,
                min: 0,
                minRange: 2,
                title: null,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickPixelInterval: 50,
                labels: {
                    align: 'right',
                    padding: 0,
                    style: {
                        color: '#555',
                        fontSize: '10px'
                    },
                    x: -10
                },
                visible: true
            },
            legend: {
                enabled: true,
                padding: 0,
                y: 5,
                itemStyle: {
                    color: '#555'
                    //fontWeight: 300
                },
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            tooltip: {
                shared: true,
                valueSuffix: ' ' + widget.entry.units,
                shadow: false
                /*
                 positioner: function () {
                 return { x: 80, y: 50 };
                 },
                 shadow: false
                 */
            },
            plotOptions: {
                column: {
                    depth: 15,
                    //colorByPoint: true,
                    //colors: ['rgba(255, 0, 0, 0.45)', 'rgba(113, 255, 0, 0.45)', 'rgba(0, 105, 255, 0.45)'],
                    //pointWidth: 10,
                    //pointInterval: 6000,
                    //pointPlacement: 'between',
                    grouping: false,
                    edgeWidth: 0,
                    borderWidth: 0,
                    pointPadding: 0,
                    groupPadding: 0.1
                    //pointRange: 60 * 1000
                },
                areaspline: {
                    lineWidth: 1,
                    fillOpacity: 0.2
                },
                spline: {
                    lineWidth: 2
                },
                series: {
                    //shadow: true,
                    marker: {
                        enabled: false,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 0,
                                radius: 3,
                                radiusPlus: 0
                            }
                        }
                    },
                    states: {
                        hover: {
                            enabled: true,
                            lineWidthPlus: 0,
                            halo: {
                                size: 0
                            }
                        }
                    }
                }
            },
            series: chartSeries
        });
    },

    pieChart: function (widget) {
        var seriesName = widget.entry.seriesRenderer || widget.entry.pieGroupColumn;
        return new Highcharts.Chart({
            chart: {
                type: 'pie',
                renderTo: widget.getEl().query('.chart')[0],
                animation: Highcharts.svg,
                spacing: [20, 10, 20, 20],
                backgroundColor: 'transparent',
                style: {
                    //fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: false,
                    alpha: 45,
                    beta: 0
                }
            },
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false
            },
            legend: {
                title: {
                    text: seriesName + '<br/><span style="font-size: 9px; color: #666; font-weight: normal">(Click to hide)</span>',
                    style: {
                        fontStyle: 'italic'
                    }
                },
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'top',
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
                //x: -10,
                //y: 100                
            },
            tooltip: {
                headerFormat: '<span style="font-size: 16px; font-weight: bold;">' + seriesName + ' {point.key}</span><br/>',
                pointFormat: '{series.name}: <b>{point.y}</b> ({point.percentage:.1f}%)'
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    showInLegend: true,
                    depth: 15,
                    dataLabels: {
                        enabled: true,
                        distance: 3,
                        padding: 0,
                        format: '<b>{point.y}</b> ({point.percentage:.1f}%)',
                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || '#555'
                    }
                }
            },
            series: [{
                id: widget.entry.units,
                name: widget.entry.units,
                colorByPoint: true,
                data: []
            }]
        });
    }

});
