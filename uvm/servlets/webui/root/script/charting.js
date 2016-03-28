/*global
 Ext, Ung, i18n, console, Highcharts
 */

/**
 * Highcharts implementation
 */

Ext.define('Ung.charts', {
    singleton: true,
    //generateRandomData: true,
    baseColors: ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],

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

        var chartType,
            colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : this.baseColors;

        // add transparency to colors
        if (entry.columnOverlapped) {
            colors = colors.map(function (color) {
                Highcharts.Color(color).setOpacity(0.5).get('rgba');
            });
            console.log(colors);
        }

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
            lang: {
                noData: i18n._("No data available yet!")
            },
            noData: {
                style: {
                    fontSize: '16px',
                    color: '#999'
                }
            },
            colors: colors,
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
                enabled: false
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
                areaspline: {
                    lineWidth: 0,
                    fillOpacity: 0.5
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
            series: this.setTimeSeries(entry, data, null)
        });
    },

    /**
     * Creates or updates the Time Series chart data
     * @param {Object} entry - the Report entry object
     * @param {Object} data  - the data used for the series
     * @param {Object} chart - the chart for which data is updated; is null when creating the chart
     * @returns {Array}      - the series array
     */
    setTimeSeries: function (entry, data, chart) {
        var i, j, _data, _seriesOptions = [];

        if (entry.type === 'TIME_GRAPH_DYNAMIC') {
            entry.timeDataColumns = [];
            var column;
            for (column in data[data.length - 1]) {
                if (data[data.length - 1].hasOwnProperty(column) && column !== 'time_trunc') {
                    entry.timeDataColumns.push(column);
                }
            }
        }

        if (entry.timeDataColumns.length === 0) {
            _seriesOptions[0] = {
                data: []
            };
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
                    pointPadding: entry.columnOverlapped ? 0.2 * i : 0.1
                };
            } else {
                _seriesOptions[i] = {
                    id: entry.timeDataColumns[i].split(' ').splice(-1)[0],
                    name: entry.timeDataColumns[i].split(' ').splice(-1)[0],
                    grouping: entry.columnOverlapped ? false : true,
                    pointPadding: entry.columnOverlapped ? 0.2 * i : 0.1
                };
            }

            _data = [];
            for (j = 0; j < data.length; j += 1) {
                _data.push([
                    data[j].time_trunc.time,
                    data[j][_seriesOptions[i].id] ? Math.floor(data[j][_seriesOptions[i].id]) : (this.generateRandomData ? Math.floor(Math.random() * 120) : 0)
                ]);
            }
            if (!chart) {
                _seriesOptions[i].data = _data;
                //_seriesOptions[i].data = []; // test for no data
            } else {
                chart.series[i].setData(_data, false, true);
            }
        }

        if (!chart) {
            return _seriesOptions;
        }

        chart.redraw();

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
            that = this,
            colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : this.baseColors;

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
                animation: false,
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: entry.is3d,
                    alpha: entry.chartType === 'pie' ? 45 : 20,
                    beta: 0,
                    depth: entry.chartType === 'pie' ? 0 : 50
                },
                events: {
                    drilldown: function (e) {
                        this.isDrillDown = true;

                        var i, _ddData = [];
                        for (i = entry.ddBreakPoint; i < entry.data.length; i += 1) {
                            _ddData.push({
                                name: entry.data[i][entry.pieGroupColumn],
                                y: entry.data[i].value
                            });
                        }
                        this.addSeriesAsDrilldown(e.point, {name: 'sessions', data: _ddData});
                        //that.setCategoriesSeries(entry, data, this);
                    },
                    drillup: function (e) {
                        this.isDrillDown = false;
                        that.setCategoriesSeries(entry, data, this);
                    }
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
            lang: {
                noData: i18n._("No data available yet!")
            },
            noData: {
                style: {
                    fontSize: '16px',
                    color: '#999'
                }
            },
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
                pointFormat: '{series.name}: <b>{point.y}</b>' + (entry.chartType === 'pie' ? ' ({point.percentage:.1f}%)' : '')
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
                        //format: '<b>{point.y}</b> ({point.percentage:.1f}%)',
                        formatter: function () {
                            return '<b>' + this.point.y + '</b> (' + this.point.percentage.toFixed(1) + '%)';
                        },
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
            series: this.setCategoriesSeries(entry, data, null),
            drilldown: {
                drillUpButton: {
                    position: {
                        align: 'left'
                    },
                    relativeTo: 'spacingBox'
                }
            }

        });
    },

    /**
     * Creates or updates the Categories chart data
     * @param {Object} entry - the Report entry object
     * @param {Object} data  - the data used for the series
     * @param {Object} chart - the chart for which data is updated; is null when creating the chart
     * @returns {Array}      - the series array
     */
    setCategoriesSeries: function (entry, data, chart) {
        // TODO: Pie percentage not correct inside DrillDown

        var i, _otherCumulateVal = 0, _mainData = [], _drillDownData = [], _total = 0;

        if (this.generateRandomData) {
            data = [];
            for (i = 0; i < Math.floor(Math.random() * 12) + 5; i += 1) {
                data.push({
                    name: 'random',
                    value: Math.floor(Math.random() * 100) + 1
                });
            }

            // sort descending by value
            data.sort(function (d1, d2) {
                return d2.value - d1.value;
            });
        }

        // calculate total
        for (i = 0; i < data.length; i += 1) {
            _total += data[i].value;
        }

        // calculate percentages to use in drilldown
        for (i = 0; i < data.length; i += 1) {
            data[i].percentage = parseFloat((data[i].value * 100 / _total).toFixed(1));
        }

        // store data inside entry for drilldown usage
        entry.data = data;

        entry.ddBreakPoint = null;
        // find drilldown breakpoint under 15%
        for (i = 0; i < data.length; i += 1) {
            if (data[i].percentage < 15 && !entry.ddBreakPoint) {
                entry.ddBreakPoint = i;
            }
        }

        var _mainSeries = [{
            name: entry.units
        }];

        for (i = 0; i < data.length; i += 1) {
            if (i < entry.ddBreakPoint) {
                _mainData.push({
                    name: data[i][entry.pieGroupColumn],
                    y: data[i].value
                });
            } else {
                _otherCumulateVal += data[i].value;
                _drillDownData.push({
                    name: data[i][entry.pieGroupColumn],
                    y: data[i].value
                });
            }
        }

        _mainData.push({
            name: 'other',
            y: _otherCumulateVal,
            drilldown: true
        });

        if (!chart) {
            _mainSeries[0].data = _mainData;
            return _mainSeries;
        }

        if (!chart.isDrillDown) {
            chart.series[0].setData(_mainData, true, true);
        } else {
            chart.series[0].setData(_drillDownData, true, true);
        }
    },


    /**
     * Updates the Series type for the TimeSeries charts
     * @param {Object} entry   - the Report entry object
     * @param {Object} chart   - the chart for which series are updated
     * @param {string} newType - the new type of the Series: 'spline', 'areaspline' or 'column'
     */
    updateSeriesType: function (entry, chart, newType) {
        var i, _newOptions;
        for (i = 0; i < chart.series.length; i += 1) {
            _newOptions = {
                grouping: entry.columnOverlapped ? false : true,
                pointPadding: entry.columnOverlapped ? 0.15 * i : 0.1,
                type: newType
            };
            if (entry.columnOverlapped) {
                _newOptions.color = Highcharts.Color(chart.options.colors[i]).setOpacity(0.75).get('rgba');
            } else {
                _newOptions.color = chart.options.colors[i];
            }
            chart.series[i].update(_newOptions, false);
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
        if (!data || !data[data.length - 1].time_trunc) {
            return false;
        }

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
    },

    /**
     * Set the chart display for the node in RackView
     * @param {Object} container - the DOM element where the chart is rendered
     * @param {Object} data      - the series data
     * @returns {Object}         - the HighCharts chart object
     */
    nodeChart: function (container, data) {
        var i, _data = [];
        for (i = 0; i < data.length; i += 1) {
            _data.push({
                x: data[i].time,
                y: data[i].sessions
            });
        }

        return new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: container,
                margin: [0, 0, 0, 0],
                backgroundColor: 'transparent'
            },
            credits: {
                enabled: false
            },
            colors: ['#FFF'],
            title: null,
            xAxis: {
                type: 'datetime',
                labels: {
                    enabled: false,
                    style: {
                        color: '#999',
                        fontSize: '10px'
                    }
                },
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                minRange: 1,
                min: 0,
                tickAmount: 4,
                title: null,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#777',
                labels: {
                    align: 'left',
                    style: {
                        color: '#999',
                        fontSize: '9px'
                    },
                    x: 0,
                    y: -3
                },
                visible: true
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.15,
                    lineWidth: 1
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
                            enabled: false,
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
                enabled: false,
                animation: false,
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                useHTML: true,
                style: {
                    padding: '5px',
                    fontSize: '11px'
                },
                headerFormat: '',
                pointFormatter: function () {
                    var str = '<span>' + this.series.name + '</span>';
                    return '<div>' + str + '</div>';
                }
            },
            series: [{
                data: _data
            }]
        });
    },

    /**
     * Updates the Rack node chart
     * @param {Object} chart - the chart object
     * @param {Object} data  - new data to update
     */
    nodeChartUpdate: function (chart, data) {
        var i, _data = [];
        for (i = 0; i < data.length; i += 1) {
            _data.push({
                x: data[i].time,
                y: data[i].sessions
            });
        }
        chart.series[0].setData(_data, true, false);
    }

});
