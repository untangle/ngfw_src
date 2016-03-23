/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, window, document, Highcharts
 */

/**
 * Highcharts for dashboard widgets
 */

Ext.define('Ung.dashboard.Charts', {
    singleton: true,

    cpuLoad1: function (container) {
        return new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: container,
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

    cpuLoad2: function (container) {
        return new Highcharts.Chart({
            chart: {
                type: 'gauge',
                renderTo: container,
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

    timeChart: function (entry, data, container, forDashboard) {
        var seriesRenderer, column, i;

        if (!entry.timeDataColumns) {
            entry.timeDataColumns = [];
        }
        /*
        if (!Ext.isEmpty(widget.entry.seriesRenderer)) {
            seriesRenderer = Ung.panel.Reports.getColumnRenderer(widget.entry.seriesRenderer);
        } else {
            console.log(widget.entry);
            seriesRenderer = widget.entry.seriesRenderer;
        }
        */

        var chartSeries = [], chartType, chart3d;

        //var alphaColors = ['rgba(178, 178, 178, 0.7)', 'rgba(57, 108, 53, 0.7)', 'rgba(51, 153, 255, 0.7)'];

        return new Highcharts.StockChart({
            chart: {
                type: entry.chartType,
                zoomType: 'x',
                renderTo: container,
                marginBottom: 50,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: chart3d,
                    alpha: 0,
                    beta: 0,
                    depth: 20,
                    viewDistance: 10
                }
            },
            colors: (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
            navigator: {
                enabled: !forDashboard
            },
            rangeSelector : {
                enabled: !forDashboard,
                inputEnabled: false,
                buttons: [{
                    type: 'minute',
                    count: 60,
                    text: '1h'
                }, {
                    type: 'minute',
                    count: 180,
                    text: '3h'
                }, {
                    type: 'minute',
                    count: 360,
                    text: '6h'
                }, {
                    type: 'all',
                    text: '24h'
                }],
                selected : 3
            },
            scrollbar: {
                enabled: false
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
                    areaSplineButton: {
                        text: 'Areaspline',
                        onclick: function () {
                            for (i = 0; i < this.series.length; i += 1) {
                                this.series[i].update({type: 'areaspline'}, true, true);
                            }
                        }
                    },
                    areaButton: {
                        text: 'Area',
                        onclick: function () {
                            for (i = 0; i < this.series.length; i += 1) {
                                this.series[i].update({type: 'area'}, true, true);
                            }
                        }
                    },
                    lineButton: {
                        text: 'Line',
                        onclick: function () {
                            for (i = 0; i < this.series.length; i += 1) {
                                this.series[i].update({type: 'line'}, true, true);
                            }
                        }
                    },
                    splineButton: {
                        text: 'Spline',
                        onclick: function () {
                            for (i = 0; i < this.series.length; i += 1) {
                                this.series[i].update({type: 'spline'}, true, true);
                            }
                        }
                    },
                    columnButton: {
                        text: 'Column',
                        onclick: function () {
                            for (i = 0; i < this.series.length; i += 1) {
                                this.series[i].update({type: 'column'}, true, true);
                            }
                        }
                    }
                }
            },
            navigation: {
                buttonOptions: {
                    align: 'right'
                }
            },
            xAxis: {
                type: 'datetime',
                //crosshair: true,
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
                    //text: (widget.timeframe / 3600 > 1 ? widget.timeframe / 3600 + " " + i18n._("hours") : widget.timeframe / 3600 + " " + i18n._("hour")),
                    text: '1 hour',
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
                opposite: false,
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
                    text: entry.units,
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
                    if (entry.units === "bytes" || entry.units === "bytes/s") {
                        str += ': <b>' + Ung.Util.bytesRenderer(this.y) + '</b>';
                    } else {
                        str += ': <b>' + this.y + '</b> ' + entry.units;
                    }
                    return '<div>' + str + '</div>';
                }
            },
            plotOptions: {
                column: {
                    //colorByPoint: true,
                    //pointWidth: 5,
                    //pointInterval: 1,
                    //pointIntervalUnit: 'minute',
                    //pointRange: 60 * 1000,
                    //pointPlacement: 'on',
                    //groupPadding: 0.2,
                    //pointRange: 60 * 1000,
                    edgeWidth: 0,
                    edgeColor: '#FFF',
                    borderWidth: 0,
                    //borderColor: '#FFF',
                    pointPadding: 0,
                    dataGrouping: {
                        groupPixelWidth: 50
                    }
                    //shadow: true
                },
                area: {
                    lineWidth: 1,
                    fillOpacity: 0.5
                },
                areaspline: {
                    lineWidth: 1,
                    fillOpacity: 0.5
                    //shadow: true
                },
                line: {
                    lineWidth: 2
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
            series: this.generateSeries(entry, data, null)
        });
    },

    pieChart: function (entry, data, container, forDashboard) {
        var seriesName = entry.seriesRenderer || entry.pieGroupColumn;
        var colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];

        // create gradients from colors
        colors = colors.map(function (color) {
            return {
                radialGradient: {
                    cx: 0.5,
                    cy: 0.3,
                    r: 0.7
                },
                stops: [
                    [0, color],
                    [1, Highcharts.Color(color).brighten(-0.2).get('rgb')] // darken
                ]
            };
        });

        return new Highcharts.Chart({
            chart: {
                type: 'pie',
                renderTo: container,
                //animation: false,
                spacing: [50, 10, 50, 50],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: entry.is3d,
                    alpha: 45,
                    beta: 0
                }
            },
            colors: colors,
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false
            },
            xAxis: {
                type: 'category',
                //crosshair: true,
                labels: {
                    //rotation: -45,
                    style: {
                        fontSize: '11px'
                    }
                },
                title: {
                    align: 'high',
                    offset: 20,
                    text: seriesName,
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            yAxis: {
                title: {
                    text: entry.units,
                    style: {
                        color: '#C0D0E0'
                    }
                }
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
                    colorByPoint: true,
                    innerSize: entry.isDonut ? '50%' : 0,
                    depth: 45,
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        format: '<b>{point.y}</b> ({point.percentage:.1f}%)',
                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || '#555'
                    }
                }
            },
            series: this.generateSeries(entry, data, null)
        });
    },

    columnChart: function (entry, data, container, forDashboard) {
        var seriesName = entry.seriesRenderer || entry.pieGroupColumn;

        return new Highcharts.Chart({
            chart: {
                type: 'column',
                renderTo: container,
                //margin: [0, 0, 0, 0],
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
            colors: (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false
            },
            legend: {
                enabled: false
            },
            tooltip: {
                enabled: false
            },
            plotOptions: {
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    dataLabels: {
                        enabled: true,
                        align: 'center'
                    }
                }
            },
            xAxis: {
                type: 'category',
                crosshair: true,
                labels: {
                    //rotation: -45,
                    style: {
                        fontSize: '11px'
                    }
                },
                title: {
                    align: 'high',
                    offset: 20,
                    text: seriesName,
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            yAxis: {
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                title: {
                    text: entry.units,
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            series: this.generateSeries(entry, data, null)
        });
    },

    generateSeries: function (entry, data, chart) {
        var i, j, _data, _seriesOptions = [];

        if (entry.type === 'TIME_GRAPH' || entry.type === 'TIME_GRAPH_DYNAMIC') {
            // build time data columns for TIME_GRAPH_DYNAMIC case
            if (entry.type === 'TIME_GRAPH_DYNAMIC') {
                entry.timeDataColumns = [];
                var column;
                for (column in data[0]) {
                    if (data[0].hasOwnProperty(column) && column !== 'time_trunc') {
                        entry.timeDataColumns.push(column);
                    }
                }
            }

            for (i = 0; i < entry.timeDataColumns.length; i += 1) {
                // special case series naming for interface usage
                // adding 'id' to refeer to the actual data properties
                // TODO: check other special cases
                if (entry.seriesRenderer === 'interface') {
                    _seriesOptions[i] = {
                        id: entry.timeDataColumns[i],
                        name: entry.seriesRenderer + ' [' + entry.timeDataColumns[i] + '] '
                    };
                } else {
                    _seriesOptions[i] = {
                        id: entry.timeDataColumns[i].split(' ').splice(-1)[0],
                        name: entry.timeDataColumns[i].split(' ').splice(-1)[0]
                    };
                }

                _data = [];
                for (j = 0; j < data.length; j += 1) {
                    _data.push([
                        data[j].time_trunc.time,
                        data[j][_seriesOptions[i].id] ? Math.floor(data[j][_seriesOptions[i].id]) : 0
                    ]);
                }
                if (!chart) {
                    _seriesOptions[i].data = _data;
                } else {
                    chart.series[i].setData(_data);
                }
            }
        }

        if (entry.type === 'PIE_GRAPH') {
            _data = [];
            for (i = 0; i < data.length; i += 1) {
                _data.push({
                    name: data[i][entry.pieGroupColumn],
                    y: data[i].value
                });
            }
            if (!chart) {
                _seriesOptions[0] = {
                    name: entry.units,
                    data: _data
                };
            } else {
                chart.series[0].setData(_data);
            }
        }

        return _seriesOptions;
    },

    updateType: function (chart, newType) {
        var i;
        for (i = 0; i < chart.series.length; i += 1) {
            chart.series[i].update({type: newType}, false);
        }
        chart.redraw();
    }
});
