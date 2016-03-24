/*global
 Ext, Ung, i18n, console, Highcharts
 */

/**
 * Highcharts implementation
 */

Ext.define('Ung.charts', {
    singleton: true,

    /**
     * Creates the Line chart for the CPU Load widget from Dashboard
     * @param {Object} container - the DOM element where the chart is rendered
     * @returns {Object}         - the HighCharts chart object
     */
    cpuLineChart: function (container) {
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
                visible: true
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.25,
                    lineWidth: 3
                },
                series: {
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

    /**
     * Creates the Gauge chart for the CPU Load widget from Dashboard
     * @param {Object} container - DOM element where the chart is rendered
     * @returns {Object}         - the HighCharts chart object
     */
    cpuGaugeChart: function (container) {
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

    /**
     * Creates a TimeSeries based chart
     * It renders a Line (spline), Area (areaspline) or a Column chart with data grouping possibilities
     * @param {Object} entry         - the Report entry object
     * @param {Object} data          - the data used upon chart creation
     * @param {Object} container     - the DOM element where the chart is rendered
     * @param {Boolean} forDashboard - apply specific chart options for Dashboard/Reports charts
     * @returns {Object}             - the HighStock chart object
     */
    timeSeriesChart: function (entry, data, container, forDashboard) {
        /*
        if (!Ext.isEmpty(widget.entry.seriesRenderer)) {
            seriesRenderer = Ung.panel.Reports.getColumnRenderer(widget.entry.seriesRenderer);
        } else {
            console.log(widget.entry);
            seriesRenderer = widget.entry.seriesRenderer;
        }
        */

        var chartType, i;

        switch (entry.timeStyle) {
        case 'LINE':
            chartType = 'spline';
            break;
        case 'AREA':
            chartType = 'areaspline';
            break;
        case 'BAR':
        case 'BAR_3D':
        case 'BAR_OVERLAPPED':
        case 'BAR_3D_OVERLAPPED':
            chartType = 'column';
            break;
        default:
            chartType = 'areaspline';
        }

        /*
        if (!entry.timeDataColumns) {
            entry.timeDataColumns = [];
        }
        */
        return new Highcharts.StockChart({
            chart: {
                type: chartType,
                zoomType: 'x',
                renderTo: container,
                marginBottom: 50,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                }
            },
            title: !forDashboard ? {
                text: entry.description,
                align: 'center',
                margin: 20,
                y: 20,
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    color: '#777',
                    fontSize: '14px',
                    fontWeight: 'bold'
                }
            } : null,
            colors: (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
            navigator: {
                enabled: !forDashboard
            },
            rangeSelector : {
                enabled: !forDashboard,
                inputEnabled: false,
                buttons: this.setRangeButtons(data)
            },
            scrollbar: {
                enabled: false
            },
            credits: {
                enabled: false
            },
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
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
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
                minPadding: 0
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
                    x: -10,
                    y: 3
                },
                title: {
                    align: 'high',
                    offset: -10,
                    y: 3,
                    text: entry.units,
                    style: {
                        fontSize: !forDashboard ? '14px' : '10px',
                        color: 'green'
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
                    edgeWidth: 0,
                    borderWidth: 0,
                    pointPadding: 0,
                    groupPadding: 0.2,
                    dataGrouping: {
                        groupPixelWidth: 50
                    },
                    dataLabels: {
                        enabled: false,
                        align: 'left',
                        rotation: -45,
                        x: 0,
                        y: -2,
                        style: {
                            fontSize: '9px',
                            color: '#999',
                            textShadow: false
                        }
                    }
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
            series: this.setSeries(entry, data, null)
        });
    },

    /**
     * Creates a Category based chart
     * It renders a Pie (spline), Donut (areaspline) or a Column chart with 3D variants
     * @param {Object} entry         - the Report entry object
     * @param {Object} data          - the data used upon chart creation
     * @param {Object} container     - the DOM element where the chart is rendered
     * @param {Boolean} forDashboard - apply specific chart options for Dashboard/Reports charts
     * @returns {Object}             - the HighCharts chart object
     */
    categoriesChart: function (entry, data, container, forDashboard) {
        var seriesName = entry.seriesRenderer || entry.pieGroupColumn,
            colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];

        // apply gradient colors for the Pie chart
        /*
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
        */

        return new Highcharts.Chart({
            chart: {
                type: entry.chartType,
                renderTo: container,
                margin: (entry.chartType === 'pie' && !forDashboard) ? [80, 20, 50, 20] : undefined,
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: entry.is3d,
                    alpha: entry.chartType === 'pie' ? 45 : 20,
                    beta: 0,
                    depth: entry.chartType === 'pie' ? 0 : 50
                }
            },
            title: !forDashboard ? {
                text: entry.description,
                align: 'center',
                margin: 20,
                y: 20,
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    color: '#777',
                    fontSize: '14px',
                    fontWeight: 'bold'
                }
            } : null,
            colors: colors,
            credits: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            xAxis: {
                type: 'category',
                labels: {
                    style: {
                        fontSize: '11px'
                    }
                },
                title: !forDashboard ? {
                    align: 'middle',
                    text: seriesName,
                    style: {
                        fontSize: '16px'
                    }
                } : {
                    align: 'high',
                    offset: 20,
                    text: seriesName,
                    style: {
                        color: '#C0D0E0'
                    }
                }
            },
            tooltip: {
                headerFormat: '<span style="font-size: 16px; font-weight: bold;">' + seriesName + ' {point.key}</span><br/>',
                pointFormat: '{series.name}: <b>{point.y}</b>' + (entry.chartType === 'pie' ? '({point.percentage:.1f}%)' : '')
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
                },
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    depth: 50,
                    dataLabels: {
                        enabled: true,
                        align: 'center'
                    }
                }
            },
            yAxis: {
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                title: !forDashboard ? {
                    align: 'middle',
                    text: entry.units,
                    style: {
                        fontSize: '16px'
                    }
                } : {
                    align: 'high',
                    offset: -10,
                    text: entry.units,
                    style: {
                        color: 'green'
                    }
                }
            },
            legend: {
                enabled: entry.chartType === 'pie',
                title: {
                    text: seriesName + '<br/><span style="font-size: 9px; color: #555; font-weight: normal">(Click to hide)</span>',
                    style: {
                        fontStyle: 'italic'
                    }
                },
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'top',
                y: !forDashboard ? 50 : 0,
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            series: this.setSeries(entry, data, null)
        });
    },

    /**
     * Creates or updates the chart series with data
     * @param {Object} entry - the Report entry object
     * @param {Object} data  - the data used for the series
     * @param {Object} chart - the chart for which data is updated; is null when creating the chart
     * @returns {Array}      - the series array
     */
    setSeries: function (entry, data, chart) {
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
                        name: entry.seriesRenderer + ' [' + entry.timeDataColumns[i] + '] ',
                        grouping: entry.columnOverlapped ? false : true,
                        pointPadding: entry.columnOverlapped ? 0.15 * i : 0.1
                    };
                } else {
                    _seriesOptions[i] = {
                        id: entry.timeDataColumns[i].split(' ').splice(-1)[0],
                        name: entry.timeDataColumns[i].split(' ').splice(-1)[0],
                        grouping: entry.columnOverlapped ? false : true,
                        pointPadding: entry.columnOverlapped ? 0.15 * i : 0.1
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

    /**
     * Updates the Series type for the TimeSeries charts
     * @param {Object} entry   - the Report entry object
     * @param {Object} chart   - the chart for which series are updated
     * @param {string} newType - the new type of the Series: 'spline', 'areaspline' or 'column'
     */
    updateSeriesType: function (entry, chart, newType) {
        var i;
        for (i = 0; i < chart.series.length; i += 1) {
            chart.series[i].update({
                grouping: entry.columnOverlapped ? false : true,
                pointPadding: entry.columnOverlapped ? 0.15 * i : 0.1,
                type: newType
            }, false);
        }
        chart.redraw();
    },

    /**
     * Set the zooming range buttons for Time Series based on datetime extremes
     * @param {Object} data - the series data
     * @returns {Object}    - the range buttons definition
     * TODO: WIP on setting correct range buttons
     */
    setRangeButtons: function (data) {
        var buttons = [],
            range = (data[data.length - 1].time_trunc.time - data[0].time_trunc.time) / 1000; // seconds

        buttons.push({
            type: 'all',
            text: 'All'
        });

        if (range / (3600 * 24 * 31) > 1) { // more than a month
            buttons.unshift({
                type: 'week',
                count: 1,
                text: '1wk'
            });
        }

        if (range / (3600 * 24 * 7) >= 1) { // more than a week
            buttons.unshift({
                type: 'day',
                count: 1,
                text: '1d'
            });
        }

        if (range / (3600 * 24) >= 1 && range / (3600 * 24 * 7) < 1) { // more than a day
            buttons.unshift({
                type: 'minute',
                count: 6 * 60,
                text: '6h'
            });
            buttons.unshift({
                type: 'minute',
                count: 3 * 60,
                text: '3h'
            });
            buttons.unshift({
                type: 'minute',
                count: 60,
                text: '1h'
            });
        }

        if (range / 3600 >= 1 && range / (3600 * 24) < 1) { // less than an hour
            buttons.unshift({
                type: 'minute',
                count: 30,
                text: '30m'
            });
            buttons.unshift({
                type: 'minute',
                count: 10,
                text: '10m'
            });
            buttons.unshift({
                type: 'minute',
                count: 1,
                text: '1m'
            });
        }
        return buttons;
    }

});
