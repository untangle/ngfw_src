/*global
 Ext, Ung, i18n, console, Highcharts, window, rpc
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
                marginTop: 35,
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
                lineColor: "#C0D0E0",
                lineWidth: 1,
                tickLength: 5,
                tickPixelInterval: 70,
                labels: {
                    style: {
                        color: '#999',
                        fontSize: '9px'
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
                    lineWidth: 1,
                    animation: false
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
                positioner: function () {
                    return { x: 5, y: 0 };
                },
                formatter: function () {
                    return Highcharts.dateFormat('%H:%M:%S', new Date(this.x)) + ' - ' + this.y + ' load';
                },
                backgroundColor: 'transparent',
                borderWidth: 0,
                shadow: false,
                style: {
                    padding: '3px',
                    color: '#999',
                    fontSize: '9px'
                }
            },
            series: [{
                lineWidth: 2,
                data: (function () {
                    // generate an array of random data
                    var data = [], time = Date.now(), i;
                    try {
                        time = rpc.systemManager.getMilliseconds();
                    } catch (e) {
                        console.log('Unable to get current millis.');
                    }

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
                max: 50,
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
                    color: 'rgba(112, 173, 112, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 3,
                    to: 6,
                    color: 'rgba(255, 255, 0, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 6,
                    to: 7,
                    color: 'rgba(255, 0, 0, 1)',
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
                        radius: '99%',
                        backgroundColor: '#999'
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
     * @param {Object} container     - the ExtJS component containing the chart
     * @param {Boolean} forDashboard - apply specific chart options for Dashboard/Reports charts
     * @param {Boolean} forFixed     - apply specific chart options for fixed/non-interactive charts
     * @returns {Object}             - the HighStock chart object
     */
    timeSeriesChart: function (entry, data, container, forDashboard, forFixed) {
        var chartType,
            colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : this.baseColors;

        switch (entry.timeStyle) {
        case 'LINE':
            chartType = 'spline';
            break;
        case 'AREA':
        case 'AREA_STACKED':
            chartType = 'areaspline';
            break;
        case 'BAR':
        case 'BAR_3D':
        case 'BAR_OVERLAPPED':
        case 'BAR_3D_OVERLAPPED':
        case 'BAR_STACKED':
            chartType = 'column';
            break;
        default:
            chartType = 'areaspline';
        }

        return new Highcharts.StockChart({
            chart: {
                type: chartType,
                zoomType: 'x',
                renderTo: !forDashboard ? container.dom : container.getEl().query('.chart')[0],
                //marginBottom: !forDashboard ? 40 : 50,
                marginTop: !forDashboard ? 20 : 10,
                //padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                animation: false,
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                }
            },
            title: null,
            lang: {
                noData: i18n._("No data available yet!")
            },
            noData: {
                style: {
                    fontSize: '16px',
                    fontWeight: 'normal',
                    color: '#999'
                }
            },
            colors: colors,
            navigator: {
                enabled: false
            },
            rangeSelector : {
                enabled: false,
                inputEnabled: true,
                buttons: this.setRangeButtons(data)
            },
            scrollbar: {
                enabled: !forDashboard && !forFixed
            },
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            exporting: {
                chartOptions: {
                    title: {
                        text: entry.category + ' - ' + entry.title,
                        style: {
                            fontSize: '12px'
                        }
                    }
                },
                type: 'image/jpeg'
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
                /*
                events: {
                    setExtremes: function (event) {
                        if (!forDashboard) {

                            var panelReports = container.up('panel[name=panelReports]');
                            panelReports.timeFrame = {
                                start: new Date(event.min),
                                end: new Date(event.max)
                            };

                        }
                    }
                }
                */
            },
            yAxis: {
                allowDecimals: false,
                min: 0,
                minRange: 1,
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
                shared: true,
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                style: {
                    padding: '5px',
                    fontSize: '11px'
                },
                headerFormat: '<div style="font-size: 12px; font-weight: bold; line-height: 1.5;">{point.key}</div><br/>',
                pointFormatter: function () {
                    var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                    if (entry.units === "bytes" || entry.units === "bytes/s") {
                        str += ': <b>' + Ung.Util.bytesRenderer(this.y) + '</b>';
                    } else {
                        str += ': <b>' + this.y + '</b> ' + entry.units;
                    }
                    return str + '<br/>';
                }
            },
            plotOptions: {
                column: {
                    stacking: (entry.timeStyle === 'AREA_STACKED' || entry.timeStyle === 'BAR_STACKED') ? 'normal' : undefined,
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
                    stacking: (entry.timeStyle === 'AREA_STACKED' || entry.timeStyle === 'BAR_STACKED') ? 'normal' : undefined,
                    lineWidth: 0,
                    fillOpacity: (entry.timeStyle === 'AREA_STACKED' || entry.timeStyle === 'BAR_STACKED') ? 0.75 : 0.5
                    //shadow: true
                },
                spline: {
                    lineWidth: 2
                    //shadow: true
                },
                series: {
                    //shadow: true,
                    animation: false,
                    dataGrouping: {
                        approximation: entry.approximation || 'sum'
                    },
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
            series: this.setTimeSeries(entry, data, colors)
        });
    },

    /**
     * Creates or updates the Time Series chart data
     * @param {Object} entry - the Report entry object
     * @param {Object} data  - the data used for the series
     * @param {Array} colors - the colors used for the series
     * @returns {Array}      - the series array
     */
    setTimeSeries: function (entry, data, colors) {
        var i, j, _data, _seriesOptions = [], _seriesRenderer, _column, _color,
            columnOverlapped = entry.timeStyle === 'BAR_OVERLAPPED';

        if (entry.type === 'TIME_GRAPH_DYNAMIC') {
            entry.timeDataColumns = [];
            for (i = 0; i < data.length; i += 1) {
                for (_column in data[i]) {
                    if (data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && entry.timeDataColumns.indexOf(_column) < 0) {
                        entry.timeDataColumns.push(_column);
                    }
                }
            }
        }

        if (entry.timeDataColumns.length === 0) {
            _seriesOptions[0] = {
                data: []
            };
        }

        for (i = 0; i < entry.timeDataColumns.length; i += 1) {
            if (!Ext.isEmpty(entry.seriesRenderer)) {
                _seriesRenderer =  Ung.panel.Reports.getColumnRenderer(entry.seriesRenderer);
            }
            _column = entry.timeDataColumns[i].split(" ").splice(-1)[0];

            if (columnOverlapped && colors) {
                _color = new Highcharts.Color(colors[i]).setOpacity(0.75).get('rgba');
            } else {
                _color = colors[i];
            }

            _seriesOptions[i] = {
                id: _column,
                name: _seriesRenderer ? _seriesRenderer(_column) + ' [' + _column + ']' : _column,
                grouping: !columnOverlapped,
                pointPadding: columnOverlapped ? (entry.timeDataColumns.length <= 3 ? 0.15 : 0.075) * i : 0.1,
                color: _color
            };

            _data = [];
            console.log(data);
            for (j = 0; j < data.length; j += 1) {
                _data.push([
                    data[j].time || data[j].time_trunc, // for sqlite db, time_trunc represents the actual timestamp
                    data[j][_seriesOptions[i].id] || 0
                ]);
            }

            _seriesOptions[i].data = _data;
        }
        return _seriesOptions;
    },

    /**
     * Creates a Category based chart
     * It renders a Pie (spline), Donut (areaspline) or a Column chart with 3D variants
     * @param {Object} entry         - the Report entry object
     * @param {Object} data          - the data used upon chart creation
     * @param {Object} container     - the ExtJS component containing the chart
     * @param {Boolean} forDashboard - apply specific chart options for Dashboard/Reports charts
     * @param {Boolean} forFixed     - apply specific chart options for fixed/non-interactive charts
     * @returns {Object}             - the HighCharts chart object
     */
    categoriesChart: function (entry, data, container, forDashboard, forFixed) {
        var colors = (entry.colors !== null && entry.colors.length > 0) ? entry.colors : this.baseColors,
            tableConfig = Ext.clone(Ung.TableConfig.getConfig(entry.table)),
            seriesName,
            column,
            i;

        for (i = 0; i < tableConfig.columns.length; i += 1) {
            column = tableConfig.columns[i];
            if (column.dataIndex === entry.pieGroupColumn) {
                seriesName = column.header;
            }
        }

        return new Highcharts.Chart({
            chart: {
                type: entry.pieStyle.indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                renderTo: !forDashboard ? container.dom : container.getEl().query('.chart')[0],
                margin: (entry.chartType === 'pie' && !forDashboard) ? [80, 20, 50, 20] : undefined,
                spacing: [10, 10, 20, 10],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: '"PT Sans", "Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: entry.pieStyle.indexOf('3D') >= 0,
                    alpha: (entry.pieStyle === 'PIE_3D' || entry.pieStyle === 'DONUT_3D') ? 45 : 20,
                    beta: entry.pieStyle === 'COLUMN_3D' ? 5 : 0
                }
            },
            title: null,
            lang: {
                noData: i18n._('No data available yet!'),
                drillUpText: '< ' + i18n._('Back')
            },
            noData: {
                style: {
                    fontSize: '16px',
                    fontWeight: 'normal',
                    color: '#999'
                }
            },
            colors: colors,
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            exporting: {
                chartOptions: {
                    title: {
                        text: entry.category + ' - ' + entry.title,
                        style: {
                            fontSize: '12px'
                        }
                    }
                },
                type: 'image/jpeg'
            },
            xAxis: {
                type: 'category',
                labels: {
                    style: {
                        fontSize: '11px'
                    }
                },
                title: {
                    align: 'middle',
                    text: seriesName,
                    style: {
                        fontSize: !forDashboard ? '14px' : '12px',
                        fontWeight: 'bold'
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
                    y: 3,
                    text: entry.units,
                    style: {
                        fontSize: !forDashboard ? '14px' : '10px',
                        color: 'green'
                    }
                }
            },
            tooltip: {
                headerFormat: '<span style="font-size: 14px; font-weight: bold;">' + seriesName + ': {point.key}</span><br/>',
                pointFormat: '{series.name}: <b>{point.y}</b>' + (entry.pieStyle.indexOf('COLUMN') < 0 ? ' ({point.percentage:.1f}%)' : '')
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    center: ['50%', '50%'],
                    showInLegend: true,
                    colorByPoint: true,
                    innerSize: entry.pieStyle.indexOf('DONUT') >= 0 ? '50%' : 0,
                    depth: !forDashboard ? 50 : 25,
                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#EEE',
                    dataLabels: {
                        enabled: true,
                        distance: !forDashboard ? 15 : 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: !forDashboard ? '12px' : '11px'
                        },
                        formatter: function () {
                            if (this.point.percentage < 5) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.name;
                        },
                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || '#555'
                    }
                },
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    depth: !forDashboard ? 50 : 25,
                    dataLabels: {
                        enabled: false,
                        align: 'center'
                    }
                },
                series: {
                    animation: false
                }
            },
            legend: {
                enabled: entry.pieStyle.indexOf('COLUMN') < 0 && !forDashboard,
                backgroundColor: '#EEE',
                borderRadius: 3,
                padding: 15,
                style: {
                    overflow: 'hidden'
                },
                title: {
                    text: seriesName,
                    style: {
                        fontSize: '14px'
                    }
                },
                itemStyle: {
                    fontSize: !forDashboard ? '12px' : '11px',
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                //itemWidth: 120,
                useHTML: true,
                layout: 'vertical',
                align: 'left',
                verticalAlign: 'top',
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            series: this.setCategoriesSeries(entry, data, null)
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
        var _mainData = [], _otherCumulateVal = 0, i,
            _mainSeries = [{
                name: entry.units
            }];



        for (i = 0; i < data.length; i += 1) {
            if (i < entry.pieNumSlices) {
                _mainData.push({
                    name: data[i][entry.pieGroupColumn] !== undefined ? data[i][entry.pieGroupColumn] : i18n._('None'),
                    y: data[i].value
                });
            } else {
                _otherCumulateVal += data[i].value;
            }
        }

        if (_otherCumulateVal > 0) {
            _mainData.push({
                name: 'Other',
                y: _otherCumulateVal
            });
        }

        if (!chart) {
            _mainSeries[0].data = _mainData;
            return _mainSeries;
        }

        chart.series[0].setData(_mainData, true, true);
    },


    /**
     * Updates the Series type for the TimeSeries charts
     * @param {Object} entry   - the Report entry object
     * @param {Object} chart   - the chart for which series are updated
     */
    updateSeriesType: function (entry, chart) {
        var i, _newOptions, chartType, columnOverlapped = false;

        switch (entry.timeStyle) {
        case 'LINE':
            chartType = 'spline';
            break;
        case 'AREA':
        case 'AREA_STACKED':
            chartType = 'areaspline';
            break;
        case 'BAR':
        case 'BAR_3D':
        case 'BAR_STACKED':
            chartType = 'column';
            break;
        case 'BAR_OVERLAPPED':
        case 'BAR_3D_OVERLAPPED':
            columnOverlapped = true;
            chartType = 'column';
            break;
        default:
            chartType = 'areaspline';
        }

        for (i = 0; i < chart.series.length; i += 1) {
            _newOptions = {
                grouping: !columnOverlapped,
                pointPadding: columnOverlapped ? (chart.series.length <= 3 ? 0.15 : 0.075) * i : 0.1,
                type: chartType,
                stacking: (entry.timeStyle === 'AREA_STACKED' || entry.timeStyle === 'BAR_STACKED') ? 'normal' : undefined,
                fillOpacity: (entry.timeStyle === 'AREA_STACKED' || entry.timeStyle === 'BAR_STACKED') ? 0.75 : 0.5
            };

            if (columnOverlapped) {
                _newOptions.color = new Highcharts.Color(chart.options.colors[i]).setOpacity(0.75).get('rgba');
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
     * Set the chart display for the node in AppsView
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
                //margin: [2, 2, 2, 4],
                spacing: [2, 2, 2, 2],
                backgroundColor: '#FFF',
                plotShadow: true,
                plotBorderColor: '#ADADAD',
                plotBorderWidth: 1
            },
            credits: {
                enabled: false
            },
            colors: ['green'],
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
                //minRange: 3,
                min: 0,
                //tickAmount: 4,
                title: null,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    align: 'left',
                    style: {
                        color: '#999',
                        fontSize: '9px'
                    },
                    x: 0,
                    y: -3
                },
                visible: false
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
                                radius: 2,
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
                enabled: true,
                animation: false,
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                useHTML: true,
                style: {
                    padding: '5px',
                    fontSize: '10px'
                },
                headerFormat: '',
                pointFormat: '{point.y} ' + i18n._('sessions')
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
    },

    /**
     * Set the sessions chart displayed in App Status view
     * @param {Object} container - the DOM element where the chart is rendered
     * @returns {Object}         - the HighCharts chart object
     */
    appStatusChart: function (container) {
        return new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: container,
                margin: [0, 0, 20, 0],
                backgroundColor: 'transparent'
            },
            credits: {
                enabled: false
            },
            //colors: ['#FFF'],
            title: null,
            xAxis: {
                type: 'datetime',
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    enabled: true,
                    style: {
                        color: '#999',
                        fontSize: '10px'
                    }
                },
                crosshair: {
                    width: 1,
                    color: 'rgba(0,0,0,0.1)'
                }
            },
            yAxis: {
                minRange: 1,
                min: 0,
                tickPixelInterval: 50,
                title: null,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
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
                enabled: true,
                animation: false,
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                useHTML: true,
                style: {
                    padding: '5px',
                    fontSize: '10px'
                },
                formatter: function () {
                    return '<b>' + this.y + ' ' + this.series.name + '</b><br/>' +
                        Highcharts.dateFormat('%H:%M:%S', this.x);
                }
            },
            series: [{
                name: 'Sessions',
                data: (function () {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;

                    for (i = -29; i <= 0; i += 1) {
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
     * Creates a Map based chart
     * @param {Object} container     - the ExtJS component containing the chart
     * @returns {Object}             - the HighCharts chart object
     */
    mapChart: function (container) {
        return new Highcharts.Map({
            chart : {
                type: 'map',
                renderTo: container,
                margin: [0, 0, 5, 0],
                spacing: [0, 0, 0, 0],
                backgroundColor: 'transparent'
            },
            title: null,
            legend: {
                enabled: false
            },
            credits: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            mapNavigation: {
                enabled: true,
                enableMouseWheelZoom: false,
                enableTouchZoom: false,
                buttonOptions: {
                    verticalAlign: 'bottom',
                    x: 5
                }
            },
            xAxis: {
                crosshair: {
                    zIndex: 5,
                    dashStyle: 'dot',
                    snap: false,
                    color: 'gray'
                }
            },

            yAxis: {
                crosshair: {
                    zIndex: 5,
                    dashStyle: 'dot',
                    snap: false,
                    color: 'gray'
                }
            },
            series : [{
                name: 'Countries',
                mapData: Highcharts.maps['custom/world-highres'],
                color: '#E0E0E0',
                enableMouseTracking: false
            }, {
                type: 'mapbubble',
                minSize: 10,
                maxSize: 50,
                tooltip: {
                    headerFormat: '',
                    pointFormat: '<strong>{point.country}</strong><br/><strong>{point.sessionCount}</strong> ' + i18n._('sessions') + '<br/><strong>{point.z}</strong> kB/s',
                    hideDelay: 0
                }
            }]
        });
    }
});
