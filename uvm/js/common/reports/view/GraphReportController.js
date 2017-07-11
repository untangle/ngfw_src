Ext.define('Ung.view.reports.GraphReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.graphreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            resize: 'onResize'
        }
    },

    /**
     * initializes graph report
     */
    onAfterRender: function () {
        var me = this, vm = this.getViewModel(),
            modFields = { uniqueId: null }; // keeps some of the entry modified fields needed for dynamic styling

        // build the empty chart
        me.buildChart();


        if (!me.getView().up('reportwidget')) {
            // whatch when report entry is changed or modified

            vm.bind('{entry}', function (entry) {
                // if it's not a graph report, do nothing
                if (entry.get('type').indexOf('GRAPH') < 0) { return; }

                if (modFields.uniqueId !== entry.get('uniqueId')) {
                    modFields = {
                        uniqueId: entry.get('uniqueId'),
                        timeDataInterval: entry.get('timeDataInterval'),
                        pieNumSlices: entry.get('pieNumSlices'),
                        timeStyle: entry.get('timeStyle'),
                        pieStyle: entry.get('pieStyle'),
                        approximation: entry.get('approximation'),
                        colors: entry.get('colors')
                    };
                    // fetch report data first time
                    me.fetchData(true);
                    return;
                }

                // based on which fields are modified do some specific actions
                Ext.Object.each(modFields, function (key, value) {
                    if (key === 'uniqueId') { return; }
                    if (value !== entry.get(key)) {
                        modFields[key] = entry.get(key);
                        if (key === 'timeDataInterval') { me.fetchData(false); }
                        if (key === 'pieNumSlices') { me.setPieSeries(); }
                        if (Ext.Array.indexOf(['timeStyle', 'pieStyle', 'approximation'], key) >= 0) { me.setStyles(); }
                    }
                });
            }, me, {
                deep: true
            });
        } else {
            me.isWidget = true;
            // DashboardQueue.add(me.getView());
        }
    },

    /**
     * when container is resized the chart needs to adapt to the new size
     */
    onResize: function () {
        if (this.chart) {
            this.chart.reflow();
        }
    },

    /**
     * builds an empty chart (no data) and adds it to the container (this is done once)
     */
    buildChart: function () {
        var me = this, entry = me.getViewModel().get('entry'), widgetDisplay = me.getView().widgetDisplay;

        me.chart = new Highcharts.StockChart({
            chart: {
                type: 'spline',
                renderTo: me.getView().lookupReference('graph').getEl().dom,
                animation: false,
                spacing: widgetDisplay ? [5, 5, 10, 5] : [10, 10, 15, 10],
                style: { fontFamily: 'Source Sans Pro', fontSize: '12px' }
            },
            exporting: {
                enabled: false
            },
            navigator: { enabled: false },
            rangeSelector : { enabled: false },
            scrollbar: { enabled: false },
            credits: { enabled: false },
            title: {
                text: null
            },

            lang: { noData: '<i class="fa fa-info-circle fa-lg"></i><br/>No data!' },
            noData: {
                position: {
                    verticalAlign: 'top',
                    y: 20
                },
                style: {
                    fontFamily: 'Source Sans Pro',
                    padding: 0,
                    fontSize: '14px',
                    fontWeight: 'normal',
                    color: '#999',
                    textAlign: 'center'
                },
                useHTML: true
            },

            // colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,

            xAxis: {
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    style: {
                        color: '#333',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0,
                events: {
                    // afterSetExtremes: function () {
                    //     // filters the current data grid based on the zoom range
                    //     if (me.getView().up('reports-entry')) {
                    //         me.getView().up('reports-entry').getController().filterData(this.getExtremes().min, this.getExtremes().max);
                    //     }
                    // }
                }
            },
            yAxis: {
                allowDecimals: true,
                min: 0,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                //tickPixelInterval: 50,
                tickLength: 5,
                tickWidth: 1,
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: true,
                // tickInterval: entry.get('units') === 'percent' ? 20 : undefined,
                maxPadding: 0,
                opposite: false,
                labels: {
                    align: 'right',
                    useHTML: true,
                    padding: 0,
                    style: {
                        color: '#333',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    x: -10,
                    y: 4
                },
                title: {
                    align: 'high',
                    offset: -10,
                    y: 3,
                    rotation: 0,
                    textAlign: 'left',
                    style: {
                        color: '#555',
                        fontSize: '12px',
                        fontWeight: 600
                    }
                }
            },
            tooltip: {
                enabled: true,
                animation: false,
                shared: true,
                // distance: 30,
                padding: 5,
                hideDelay: 0,
            },
            plotOptions: {
                column: {
                    depth: 25,
                    edgeWidth: 1,
                    edgeColor: '#FFF'
                },
                areaspline: {
                    lineWidth: 1,
                    tooltip: {
                        // split: true
                    }
                },
                spline: {
                    lineWidth: 2
                },
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    center: ['50%', '50%'],
                    showInLegend: true,
                    colorByPoint: true,

                    depth: 35,
                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#FFF',
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: '12px',
                            color: '#333',
                            // fontFamily: 'Source Sans Pro',
                            fontWeight: 600
                        },
                        formatter: function () {
                            if (this.point.percentage < 2) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.name + ' (' + this.point.percentage.toFixed(2) + '%)';
                        }
                    }
                },
                series: {
                    animation: false,
                    states: {
                        hover: {
                            lineWidthPlus: 0
                        }
                    },
                    marker: {
                        radius: 2,
                    }
                }
            },
            legend: {
                margin: 0,
                y: widgetDisplay ? 5 : 0,
                useHTML: true,
                lineHeight: 12,
                itemDistance: 10,
                itemStyle: {
                    fontSize: '11px',
                    fontWeight: 600,
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            series: []
        });
    },

    /**
     * fetches the report data
     */
    fetchData: function (reset, cb) {
        var me = this,
            vm = this.getViewModel(),
            entryType = vm.get('entry.type');

        var treeNav;
        if (me.getView().up('#reports')) {
            treeNav = me.getView().up('#reports').down('treepanel');
        }

        if (reset) {
            // if report entry changed, reset the chart first
            while(me.chart.series.length > 0) {
                me.chart.series[0].remove(true);
            }
            me.chart.update({
                xAxis: { visible: false },
                yAxis: { visible: false }
            });
            me.chart.redraw();
            me.chart.zoomOut();
        }

        if (treeNav) { treeNav.setDisabled(true); } // disable reports tree while data is fetched
        me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>');

        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            vm.set('startDate', new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600) * 1000));
            vm.set('endDate', new Date(rpc.systemManager.getMilliseconds()));
        }

        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            vm.get('entry').getData(), // entry
            vm.get('startDate'), // start date
            vm.get('tillNow') ? null : vm.get('endDate'), // end date
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function (result) {
                if (treeNav) { treeNav.setDisabled(false); }
                me.chart.hideLoading();
                me.data = result.list;

                // after data is fetched, generate chart series based on it's type
                if (entryType === 'TIME_GRAPH' || entryType === 'TIME_GRAPH_DYNAMIC') {
                    me.setTimeSeries();
                }
                if (entryType === 'PIE_GRAPH') {
                    me.setPieSeries();
                }

                if (cb) { cb(); }
                // if graph rendered inside reports, format and add data in current data grid
                if (!me.isWidget && me.getView().up('reports-entry')) {
                    // vm.set('_currentData', []);
                    var ctrl = me.getView().up('reports-entry').getController();
                    switch (entryType) {
                        case 'TIME_GRAPH':         ctrl.formatTimeData(me.data); break;
                        case 'TIME_GRAPH_DYNAMIC': ctrl.formatTimeDynamicData(me.data); break;
                        case 'PIE_GRAPH':          ctrl.formatPieData(me.data); break;
                    }
                } else {
                    // is widget
                    // DashboardQueue.next();
                    // console.log(me);
                    // Ext.defer(function () {
                    //     DashboardQueue.add(me);
                    // }, me.refreshIntervalSec * 1000);

                }
            });
    },

    /**
     * set chart series for the timeseries
     */
    setTimeSeries: function () {
        var me = this, vm = this.getViewModel(),
            timeDataColumns = Ext.clone(vm.get('entry.timeDataColumns')),
            colors = (vm.get('entry.colors') && vm.get('entry.colors').length > 0) ? vm.get('entry.colors') : Util.defaultColors,
            i, j, seriesData, series = [], seriesRenderer = null, column,
            units = vm.get('entry.units');

        if (!me.data) { return; }

        // get or generate series names based on time data columns
        if (!timeDataColumns) {
            timeDataColumns = [];
            for (i = 0; i < me.data.length; i += 1) {
                for (var _column in me.data[i]) {
                    if (me.data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                        timeDataColumns.push(_column);
                    }
                }
            }

            if (!Ext.isEmpty(vm.get('entry.seriesRenderer'))) {
                seriesRenderer = Renderer[vm.get('entry.seriesRenderer')];
            }

        } else {
            for (i = 0; i < timeDataColumns.length; i += 1) {
                timeDataColumns[i] = timeDataColumns[i].split(' ').splice(-1)[0];
            }
        }

        // create series
        for (i = 0; i < timeDataColumns.length; i += 1) {
            column = timeDataColumns[i];
            seriesData = [];
            for (j = 0; j < me.data.length; j += 1) {
                seriesData.push([
                    me.data[j].time_trunc.time || me.data[j].time_trunc, // for sqlite is time_trunc, for postgres is time_trunc.time
                    me.data[j][column] || 0
                ]);
            }
            var renderedName = column;
            if( seriesRenderer ){
                renderedName = seriesRenderer(column);
                if(renderedName.substr(-1) != ']'){
                    renderedName += " [" + column + "]";
                }
            }
            series.push({
                name: renderedName,
                data: seriesData,
                fillColor: {
                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                    stops: [
                        [0, Highcharts.Color(colors[i]).setOpacity(0.7).get('rgba')],
                        [1, Highcharts.Color(colors[i]).setOpacity(0.1).get('rgba')]
                    ]
                },
                tooltip: {
                    pointFormatter: function () {
                        var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                        if (units === "bytes" || units === "bytes/s") {
                            str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                        } else {
                            str += ': <b>' + this.y + '</b> ' + units;
                        }
                        return str + '<br/>';
                    }
                }
            });
        }

        // remove existing series
        while (this.chart.series.length > 0) {
            this.chart.series[0].remove(false);
        }

        // add series
        series.forEach(function (serie) {
            me.chart.addSeries(serie, false, false);
        });
        me.setStyles();
        me.chart.redraw();
    },

    /**
     * set serie fro the pie chart
     */
    setPieSeries: function () {
        var me = this, vm = this.getViewModel(), seriesName,
            slicesData = [], restValue = 0, seriesRenderer = null, i;

        if (!me.data) { return; }

        if (!Ext.isEmpty(vm.get('entry.seriesRenderer'))) {
            seriesRenderer = Renderer[vm.get('entry.seriesRenderer')];
        }

        for (i = 0; i < me.data.length; i += 1) {
            if (!seriesRenderer) {
                seriesName = me.data[i][vm.get('entry.pieGroupColumn')] !== undefined ? me.data[i][vm.get('entry.pieGroupColumn')] : 'None'.t();
            } else {
                seriesName = seriesRenderer(me.data[i][vm.get('entry.seriesRenderer')]);
            }

            if (i < vm.get('entry.pieNumSlices')) {
                slicesData.push({
                    name: seriesName,
                    y: me.data[i].value,
                });
            } else {
                restValue += me.data[i].value;
            }
        }

        if (restValue > 0) {
            slicesData.push({
                name: 'Others'.t(),
                color: '#DDD',
                y: restValue
            });
        }

        while(this.chart.series.length > 0) {
            this.chart.series[0].remove(false);
        }

        me.chart.addSeries({
            name: vm.get('entry.units').t(),
            data: slicesData,
            tooltip: {
                pointFormatter: function () {
                    var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                    if (vm.get('entry.units') === "bytes" || vm.get('entry.units') === "bytes/s") {
                        str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                    } else {
                        str += ': <b>' + this.y + '</b>';
                    }
                    return str + '<br/>';
                }
            }
        }, false, false);

        me.setStyles();
        me.chart.redraw();
    },

    /**
     * returns the chart type (e.g. line, areaspline, column etc...) based on entry
     */
    setChartType: function (entry) {
        var type;

        if (entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
            switch (entry.get('timeStyle')) {
            case 'LINE':
                type = 'spline';
                break;
            case 'AREA':
            case 'AREA_STACKED':
                type = 'areaspline';
                break;
            case 'BAR':
            case 'BAR_3D':
            case 'BAR_OVERLAPPED':
            case 'BAR_3D_OVERLAPPED':
            case 'BAR_STACKED':
                type = 'column';
                break;
            default:
                type = 'areaspline';
            }
        }
        if (entry.get('type') === 'PIE_GRAPH') {
            if (entry.get('pieStyle').indexOf('COLUMN') >= 0) {
                type = 'column';
            } else {
                type = 'pie';
            }
        }
        return type;
    },

    /**
     * sets/updates the chart styles based on entry and data
     */
    setStyles: function () {
        var me = this, entry = this.getViewModel().get('entry'),
        widgetDisplay = me.getView().widgetDisplay,

        isTimeColumn = false, isColumnStacked = false, isColumnOverlapped = false,
        isPieColumn = false, isDonut = false, isPie = false, is3d = false;

        var isPieGraph = entry.get('type') === 'PIE_GRAPH';
        var isTimeGraph = entry.get('type').indexOf('TIME_GRAPH') >= 0;

        if (entry.get('timeStyle')) {
            isTimeColumn = entry.get('timeStyle').indexOf('BAR') >= 0;
            isColumnStacked = entry.get('timeStyle').indexOf('STACKED') >= 0;
            isColumnOverlapped = entry.get('timeStyle').indexOf('OVERLAPPED') >= 0;
        }

        if (entry.get('pieStyle')) {
            isPieColumn = entry.get('pieStyle').indexOf('COLUMN') >= 0;
            isPie = entry.get('pieStyle').indexOf('COLUMN') < 0;
            isDonut = entry.get('pieStyle').indexOf('DONUT') >= 0;
            is3d = entry.get('pieStyle').indexOf('3D') >= 0;
        }

        var colors = Ext.clone(entry.get('colors')) || Ext.clone(Util.defaultColors);

        if (colors) {
            for (var i = 0; i < colors.length; i += 1) {
                colors[i] = isTimeGraph ? ( isColumnOverlapped ? new Highcharts.Color(colors[i]).setOpacity(0.5).get('rgba') : new Highcharts.Color(colors[i]).setOpacity(0.7).get('rgba')) : colors[i];
            }
            // add gradient
            if ((isPie || isDonut) && !is3d) {
                colors = Highcharts.map( colors, function (color) {
                    return {
                        radialGradient: {
                            cx: 0.5,
                            cy: 0.5,
                            r: 0.7
                        },
                        stops: [
                            [0, Highcharts.Color(color).setOpacity(0.4).get('rgba')],
                            [1, Highcharts.Color(color).setOpacity(0.8).get('rgba')]
                        ]
                    };
                });
            }

        }

        me.chart.update({
            chart: {
                type: me.setChartType(entry),
                zoomType: isTimeGraph ? 'x' : undefined,
                panning: isTimeGraph,
                panKey: 'ctrl',
                options3d: {
                    enabled: is3d,
                    alpha: isPieColumn ? 30 : 50,
                    beta: isPieColumn ? 5 : 0
                }
            },
            colors: colors,
            // scrollbar: {
            //     enabled: isTimeGraph
            // },
            plotOptions: {
                series: {
                    stacking: isColumnStacked ? 'normal' : undefined,
                    dataGrouping: isTimeGraph ? { approximation: entry.get('approximation') || 'sum' } : undefined
                },
                // pie graphs
                pie: {
                    innerSize: isDonut ? '40%' : 0,
                    //borderColor: '#666666'
                },
                // time graphs
                spline: {
                    shadow: true,
                    dataGrouping: {
                        groupPixelWidth: 8
                    },
                },
                // time graphs
                areaspline: {
                    // shadow: true,
                    // fillOpacity: 0.3,
                    dataGrouping: {
                        groupPixelWidth: 8
                    },
                },
                column: {
                    borderWidth: isColumnOverlapped ? 1 : 0,
                    pointPlacement: isTimeGraph ? 'on' : undefined, // time
                    colorByPoint: isPieColumn, // pie
                    grouping: !isColumnOverlapped,
                    groupPadding: 0.20,
                    // shadow: !isColumnOverlapped,
                    shadow: false,
                    dataGrouping: isTimeGraph ? { groupPixelWidth: isColumnStacked ? 50 : 80 } : undefined
                }
            },
            xAxis: {
                visible: !isPie,
                type: isTimeGraph ? 'datetime' : 'category',
                crosshair: (isTimeGraph && !isTimeColumn) ? {
                    width: 1,
                    dashStyle: 'ShortDot',
                    color: 'rgba(100, 100, 100, 0.5)'
                } : false
                // crosshair: {
                //     width: 1,
                //     dashStyle: 'ShortDot',
                //     color: 'rgba(100, 100, 100, 0.5)'
                // },
            },
            yAxis: {
                visible: !isPie,
                minRange: entry.get('units') === 'percent' ? 100 : 1,
                maxRange: entry.get('units') === 'percent' ? 100 : undefined,
                labels: {
                    formatter: function() {
                        var finalVal = this.value;

                        if (entry.get('units') === 'bytes' || entry.get('units') === 'bytes/s') {
                            finalVal = Util.bytesToHumanReadable(this.value, true);
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + finalVal + '</strong> (per second)</span>';
                            }
                            */
                        } else {
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + this.value + '</strong> (' + entry.get('units') + ')</span>';
                            }
                            */
                        }
                        return finalVal;
                    }
                },
                title: {
                    text: entry.get('units')
                }
            },
            legend: {
                enabled: !(widgetDisplay && isPie),
                layout: isPie ? 'vertical' : 'horizontal',
                align: isPie ? 'left' : 'center',
                verticalAlign: isPie ? 'top' : 'bottom'
            }
            // tooltip: {
            //     split: isTimeGraph && !isTimeColumn
            // }
        });

        if (entry.get('timeStyle') === 'BAR_OVERLAPPED' || entry.get('timeStyle') === 'BAR_3D_OVERLAPPED') {
            Ext.Array.each(me.chart.series, function (serie, idx) {
                serie.update({
                    pointPadding: (me.chart.series.length <= 3 ? 0.1 : 0.075) * idx
                }, false);
            });
            me.chart.redraw();
        } else {
            Ext.Array.each(me.chart.series, function (serie, idx) {
                serie.update({
                    pointPadding: 0.1
                }, false);
            });
            me.chart.redraw();
        }
    }

});
