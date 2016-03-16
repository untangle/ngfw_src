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

    timeChart: function (widget) {
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

        var chartSeries = [];

        /*
        switch (widget.chartType) {
        case 'spline':
            chartType = 'spline';
            break;
        case 'areaspline':
            chartType = 'areaspline';
            break;
        case 'column':
            chartType = 'column';
            break;
        case 'column-3d':
            chartType = 'column';
            chart3d = true;
            break;
        default:
            chartType = 'areaspline';
        }
        */

        var alphaColors = ['rgba(178, 178, 178, 0.7)', 'rgba(57, 108, 53, 0.7)', 'rgba(51, 153, 255, 0.7)'];
        for (i = 0; i < widget.entry.timeDataColumns.length; i += 1) {
            column = widget.entry.timeDataColumns[i].split(' ').splice(-1)[0];
            chartSeries.push({
                id: column,
                name: column,
                data: []
                //color: alphaColors[i]
                //pointPadding: 0.15 * i
            });
        }

        return new Highcharts.Chart({
            chart: {
                type: widget.chartType,
                zoomType: 'x',
                renderTo: widget.getEl().query('.chart')[0],
                colors: (widget.entry.colors !== null && widget.entry.colors.length > 0) ? widget.entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
                marginBottom: 50,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: widget.chart3d,
                    alpha: 0,
                    beta: 0,
                    depth: 20,
                    viewDistance: 10
                }
            },
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
                lineColor: "#C0D0E0",
                lineWidth: 1,
                tickLength: 5,
                tickPixelInterval: 70,
                //startOnTick: true,
                //endOnTick: true,
                //tickInterval: 60 * 1000,
                labels: {
                    style: {
                        color: '#999',
                        fontSize: '9px'
                    }
                },
                maxPadding: 0,
                minPadding: 0,
                title: {
                    align: 'high',
                    offset: 20,
                    text: (widget.timeframe / 3600 > 1 ? widget.timeframe / 3600 + " " + i18n._("hours") : widget.timeframe / 3600 + " " + i18n._("hour")),
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            yAxis: {
                allowDecimals: false,
                min: 0,
                minRange: 2,
                lineColor: "#C0D0E0",
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickPixelInterval: 50,
                tickLength: 5,
                tickWidth: 1,
                labels: {
                    align: 'right',
                    padding: 0,
                    style: {
                        color: '#999',
                        fontSize: '9px'
                    },
                    formatter: function () {
                        return Ung.Util.bytesRendererCompact(this.value).replace(/ /g, '');
                    },
                    x: -10
                },
                title: {
                    align: 'high',
                    offset: -10,
                    text: widget.entry.units,
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            legend: {
                enabled: true,
                padding: 0,
                y: 3,
                itemStyle: {
                    color: '#555'
                    //fontWeight: 300
                },
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            tooltip: {
                animation: false,
                shared: true,
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                useHTML: true,
                style: {
                    padding: '5px',
                    fontSize: '11px'
                },
                //valueSuffix: ' ' + widget.entry.units,
                headerFormat: '<div style="font-size: 12px; font-weight: bold; line-height: 1.5; border-bottom: 1px #EEE solid; margin-bottom: 5px; color: #777;">{point.key}</div>',
                pointFormatter: function () {
                    var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                    if (widget.entry.units === "bytes" || widget.entry.units === "bytes/s") {
                        str += ': <b>' + Ung.Util.bytesRenderer(this.y) + '</b>';
                    } else {
                        str += ': <b>' + this.y + '</b> ' + widget.entry.units;
                    }
                    return '<div>' + str + '</div>';
                }
            },
            plotOptions: {
                column: {
                    depth: 10,
                    //colorByPoint: true,
                    //pointWidth: 10,
                    //pointInterval: 1,
                    //pointIntervalUnit: 'minute',
                    //pointRange: 60 * 1000,
                    //pointPlacement: 'between',
                    grouping: false,
                    edgeWidth: 0,
                    edgeColor: '#FFF',
                    borderWidth: 1,
                    //borderColor: '#FFF',
                    pointPadding: 0.1,
                    groupPadding: 0
                },
                areaspline: {
                    lineWidth: 2,
                    fillOpacity: 0.2
                    //shadow: true
                },
                spline: {
                    lineWidth: 2
                    //shadow: true
                },
                series: {
                    //shadow: true,
                    marker: {
                        enabled: false,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 0,
                                radius: 4,
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
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
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
                    text: seriesName + '<br/><span style="font-size: 9px; color: #555; font-weight: normal">(Click to hide)</span>',
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
