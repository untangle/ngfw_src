Ext.define('Ung.view.reports.GraphReport', {
    extend: 'Ext.Component',
    alias: 'widget.graphreport',

    viewModel: true,

    border: false,
    bodyBorder: false,

    margin: '0 0 0 5px',

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize',
        deactivate: 'reset'
    },

    config: {
        widget: null,
        chart: null
    },

    controller: {

        onAfterRender: function (view) {
            var me = this,
                vm = me.getViewModel();

            // initialize an empty chart (once)
            me.initChart();
            // find and set the widget component if report is rendered inside a widget
            view.setWidget(view.up('reportwidget'));
            // if it's a widget, than fetch data after the report entry is binded to it
            vm.bind('{entry}', function (entry) {
                if(Util.isDestroyed(me, view)){
                    return;
                }
                if (!entry ||
                    ( entry.get('type') !== 'PIE_GRAPH' &&
                      entry.get('type') !== 'TIME_GRAPH' &&
                      entry.get('type') !== 'TIME_GRAPH_DYNAMIC') ) {
                    return;
                }

                // if rendered as widget, add to dashboard queue
                // if (view.getWidget()) {
                //     // console.log('aaa');
                //     DashboardQueue.addFirst(view.getWidget());
                // }

                // if rendered in creating new widget dialog, fetch data
                if (view.up('new-widget') || view.itemId == 'fixedChart') {
                    me.fetchData(true);
                }
            });

            // when editing entry update graph styles on the fly
            vm.bind('{eEntry.pieStyle}', function (pieStyle) {
                if(Util.isDestroyed(me)){
                    return;
                }
                if (!pieStyle) { return; }
                me.setStyles();
            });

            vm.bind('{eEntry.timeStyle}', function (timeStyle) {
                if(Util.isDestroyed(me)){
                    return;
                }
                if (!timeStyle) { return; }
                me.setStyles();
            });
        },

        /**
         * initializes an empty chart (no data) and adds it to the container (this is done once)
         */
        initChart: function () {
            var me = this, isWidget = me.getView().isWidget;

            me.chart = new Highcharts.stockChart(me.getView().getEl().dom, {
                chart: {
                    // type: 'spline',
                    // animation: false,
                    marginRight: isWidget ? undefined : 20,
                    spacing: isWidget ? [5, 5, 10, 5] : [30, 10, 15, 10],
                    style: { fontFamily: 'Roboto Condensed', fontSize: '10px' },
                    backgroundColor: 'transparent',
                    selectedrange: null,
                    events: {
                        selection: function (event) {
                            if (isWidget) { return; } // applies only when viewing the report
                            if (event.resetSelection) {
                                me.chart.update({
                                    exporting: {
                                        buttons: {
                                            timerangeButton: {
                                                enabled: false
                                            }
                                        }
                                    }
                                });
                                me.chart.selectedrange = null;
                            } else {
                                me.chart.update({
                                    exporting: {
                                        buttons: {
                                            timerangeButton: {
                                                enabled: true
                                            }
                                        }
                                    }
                                });
                                me.chart.selectedrange = {
                                    min: event.xAxis[0].min,
                                    max: event.xAxis[0].max
                                };
                            }
                        }
                    }
                },
                exporting: {
                    enabled: true,
                    buttons: {
                        contextButton: {
                            enabled: false // disable default contextButton
                        },
                        timerangeButton: {
                            text: 'Apply this timerange'.t(),
                            align: 'center',
                            enabled: false, // this updates based on zoom selection
                            y: 10,
                            onclick: function() {
                                Ext.fireEvent('timerangechange', me.chart.selectedrange);
                            }
                        }
                    }
                },
                navigator: { enabled: false },
                rangeSelector : { enabled: false },
                scrollbar: { enabled: false },
                credits: { enabled: false },
                title: {
                    text: null,
                    useHtml: true,
                    align: 'left'
                },
                subtitle: {
                    align: 'left',
                    useHtml: true
                },
                noData: {
                    position: {
                        y: -20
                    },
                    style: {
                        fontSize: '16px',
                        fontWeight: 'normal',
                        color: '#555',
                        textAlign: 'center'
                    }
                },

                // colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,

                xAxis: {
                    // alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                    lineWidth: 1,
                    tickLength: 5,
                    // gridLineWidth: 1,
                    // gridLineDashStyle: 'dash',
                    // gridLineColor: '#EEE',
                    tickPixelInterval: isWidget ? 80 : 120,
                    labels: {
                        style: {
                            color: '#777',
                            fontSize: isWidget ? '11px' : '12px',
                            fontWeight: 600
                        },
                        // y: isWidget ? 15 : 20,
                        autoRotation: [-25]
                    },
                    maxPadding: 0,
                    minPadding: 0,
                    events: {
                        // afterSetExtremes: function () {
                        //     // filters the current data grid based on the zoom range
                        //     if (me.getView().up('entry')) {
                        //         me.getView().up('entry').getController().filterData(this.getExtremes().min, this.getExtremes().max);
                        //     }
                        // }
                    }
                },
                yAxis: {
                    allowDecimals: true,
                    min: 0,
                    lineWidth: 1,
                    // gridLineWidth: 1,
                    gridLineDashStyle: 'dash',
                    // gridLineColor: '#EEE',
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
                            color: '#777',
                            fontSize: isWidget ? '11px' : '12px',
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
                            fontSize: isWidget ? '12px' : '14px',
                            fontWeight: 600
                        }
                    }
                },
                tooltip: {
                    enabled: true,
                    animation: false,
                    shared: true,
                    followPointer: true,
                    split: false,
                    // distance: 30,
                    padding: 10,
                    hideDelay: 0,
                    backgroundColor: 'rgba(247, 247, 247, 0.95)',
                    useHTML: true,
                    style: {
                        fontSize: isWidget ? '12px' : '14px'
                    },
                    headerFormat: '<p style="margin: 0 0 5px 0; color: #555;">{point.key}</p>'
                },
                plotOptions: {
                    column: {
                        depth: 25,
                        edgeWidth: 1,
                        edgeColor: '#FFF'
                    },
                    areaspline: {
                        lineWidth: 1
                    },
                    spline: {
                        lineWidth: 2
                    },
                    pie: {
                        allowPointSelect: false,
                        cursor: 'pointer',
                        center: ['50%', '50%'],
                        showInLegend: true,
                        colorByPoint: true,

                        depth: isWidget ? 25 : 35,
                        minSize: 150,
                        borderWidth: 1,
                        edgeWidth: 1,
                        dataLabels: {
                            enabled: true,
                            distance: 5,
                            padding: 0,
                            reserveSpace: false,
                            formatter: function () {
                                if (this.point.percentage < 2) {
                                    return null;
                                }
                                if (this.point.name.length > 25) {
                                    return this.point.name.substring(0, 25) + '...';
                                }
                                return this.point.name + ' (' + this.point.percentage.toFixed(2) + '%)';
                            }
                        },
                        events: {
                            click: function(event) {
                                // call this way to be able to access viewmodel
                                me.onPointClick(event);
                            }
                        }
                    },
                    series: {
                        dataLabels: {
                            style: {
                                fontSize: isWidget ? '10px' : '12px'
                            }
                        },
                        animation: true,
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
                    y: isWidget ? 5 : 10,
                    // useHTML: true,
                    lineHeight: 12,
                    itemDistance: 10,
                    itemStyle: {
                        fontSize: '12px',
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
                loading: {
                    style: {
                        opacity: 1
                    }
                },
                series: []
            });
        },

        /**
         * returns the chart type (e.g. line, areaspline, column etc...) based on entry type and style
         */
        getChartType: function (entry) {
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
                if (entry.get('pieStyle') && entry.get('pieStyle').indexOf('COLUMN') >= 0) {
                    type = 'column';
                } else {
                    type = 'pie';
                }
            }
            return type;
        },

        /**
         * fetches the report data
         */
        fetchData: function (reset, cb) {
            var me = this,
                vm = this.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'),
                reps = me.getView().up('#reports'),
                startDate, endDate;

            if (!entry) { return; }

            // disable this reset because it fires second time and may cause browser freeze NGFW-11306
            // if (reset) { me.reset(); }

            vm.set('eError', false);

            if (reps) { reps.getViewModel().set('fetching', true); }

            // date range setup
            if (!me.getView().renderInReports) {
                // if not rendered in reports than treat as widget so from server startDate is extracted the timeframe
                startDate = new Date(Util.getMilliseconds() - (Ung.dashboardSettings.timeframe * 3600 || 3600) * 1000);
                endDate = null;
            } else {
                // if it's a report, convert UI client start date to server date
                startDate = Util.clientToServerDate(vm.get('time.range.since'));
                endDate = Util.clientToServerDate(vm.get('time.range.until'));
            }

            // if (reset) { me.reset(); }
            me.chart.hideNoData();
            me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-fw fa-lg"></i>');

            Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                entry.getData(), // entry
                startDate,
                endDate,
                vm.get('query.conditions'), -1) // sql filters
                .then(function (result) {
                    if(Util.isDestroyed(me)){
                        return;
                    }
                    me.data = result.list;

                    if (entry.get('type') === 'PIE_GRAPH') {
                        var fieldName = entry.get('pieGroupColumn');

                        var converters = [];
                        var renderers = [];
                        var fieldNames = fieldName.split(",");
                        fieldNames.forEach(function(field){
                            var tableField = TableConfig.getTableField(entry.get('table'), field);
                            if(tableField && tableField['convert']){
                                converters.push(tableField['convert']);
                            }
                            var tableColumn = TableConfig.getTableColumn(entry.get('table'), field);
                            if(tableColumn && tableColumn['renderer']){
                                renderers.push(tableColumn['renderer']);
                            }
                        });

                        me.data.forEach(function(row){
                            fieldNames.forEach(function(fieldName){
                                converters.forEach( function(conveter){
                                    row[fieldName] = conveter.call(this, row[fieldName], row);
                                });
                                renderers.forEach( function(renderer){
                                    row[fieldName] = renderer.call(this, row[fieldName], row);
                                });
                            });
                        });
                    }

                    me.setSeries();
                    if (cb) { cb(me.data); }
                }, function () {
                    if(Util.isDestroyed(vm)){
                        return;
                    }
                    if (cb) { cb(); }
                    vm.set('eError', true);
                })
                .always(function () {
                    if(Util.isDestroyed(me)){
                        return;
                    }
                    if (reps) { reps.getViewModel().set('fetching', false); }
                    if( me.chart.noDataLabel === undefined){
                        me.chart.zoomOut();
                    }
                    me.chart.hideLoading();
                });
        },

        /**
         * used to refresh the chart when it's container size changes
         */
        onResize: function (el, width, height) {
            var me = this;
            // explicitly set size of the chart to avoid cutoffs
            if (me.chart) {
                // me.chart.setSize(width, height, false);
                me.chart.reflow();
            }
        },


        reset: function () {
            var me = this;
            while(me.chart.series.length > 0) {
                me.chart.series[0].remove(true);
            }
            me.chart.update({
                xAxis: { visible: false },
                yAxis: { visible: false },
                legend: {
                    enabled: false
                }
            });
            me.chart.redraw();
            me.chart.zoomOut();
        },

        /**
         * sets the data series for graph reports
         */
        setSeries: function () {
            var me = this, vm = this.getViewModel(), entry = vm.get('eEntry') || vm.get('entry'),
                seriesRenderer = ( entry && entry.get('seriesRenderer') ) ? Renderer[entry.get('seriesRenderer')] : null,
                seriesData,
                seriesName;

            // remove any existing series
            while (me.chart.series.length > 0) {
                me.chart.series[0].remove(false);
            }

            if (!me.data || (Ext.isArray(me.data) && me.data.length === 0)) {
                me.chart.showNoData('No Data!');
                return;
            }

            if ( entry && ( entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') ){
                var dataColumns = [], units = entry.get('units');

                // get or generate series names based on timeDataColumns for TIME_GRAPH or data form TIME_GRAPH_DYNAMIC
                if (entry.get('type') === 'TIME_GRAPH') {
                    Ext.Array.each(entry.get('timeDataColumns'), function (column) {
                        dataColumns.push(column.split(' ').splice(-1)[0]);
                    });
                }


                if (entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
                    Ext.Array.each(me.data, function (row) {
                        Ext.Object.each(row, function (key) {
                            if (row.hasOwnProperty(key) && key !== 'time_trunc' && key !== 'time' && dataColumns.indexOf(key) < 0) {
                                dataColumns.push(key);
                            }
                        });
                    });
                }

                Ext.Array.each(dataColumns, function (column) {
                    // set series data
                    seriesData = [];
                    Ext.Array.each(me.data, function (row) {
                        seriesData.push([
                            ( row.time_trunc && row.time_trunc.time ) || row.time_trunc, // for sqlite is time_trunc, for postgres is time_trunc.time
                            row[column] || 0
                        ]);
                    });
                    // set series name
                    seriesName = column;
                    if (seriesRenderer) {
                        seriesName = seriesRenderer(parseInt(column, 10));
                        if (seriesName.substr(-1) != ']') {
                            seriesName += ' [' + column + ']';
                        }
                    }

                    me.chart.addSeries({
                        name: seriesName,
                        data: seriesData,
                        tooltip: {
                            pointFormatter: function () {
                                var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                                if (units === 'bytes' || units === 'bytes/s') {
                                    str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                                } else {
                                    str += ': <b>' + this.y + '</b> ' + units;
                                }
                                return str + '<br/>';
                            }
                        }
                    }, false, false);
                });
            }

            if (entry && ( entry.get('type') === 'PIE_GRAPH') ){
                var othersValue = 0, colVal; // needed for global conditions from pies which have seriesRendered defined (altered) e.g. protocol
                seriesData = [];

                Ext.Array.each(me.data, function (row, idx) {
                    colVal = row[entry.get('pieGroupColumn')];
                    if (!seriesRenderer) {
                        seriesName = colVal !== undefined ? colVal : 'None'.t();
                    } else {
                        seriesName = seriesRenderer(parseInt(colVal, 10));
                    }

                    if (idx < entry.get('pieNumSlices')) {
                        seriesData.push({
                            name: seriesName,
                            value: colVal,
                            y: row.value
                        });
                    } else {
                        othersValue += row.value;
                    }
                });

                // add the rest of the values as Others slice
                if (othersValue > 0) {
                    seriesData.push({
                        name: 'Others'.t(),
                        color: '#DDD',
                        y: othersValue
                    });
                }

                me.chart.addSeries({
                    name: entry.get('units').t(),
                    // name: seriesName,
                    data: seriesData,
                    tooltip: {
                        pointFormatter: function () {
                            var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                            if (entry.get('units') === 'bytes' || entry.get('units') === 'bytes/s') {
                                str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                            } else {
                                str += ': <b>' + this.y + '</b>';
                            }
                            return str + '<br/>';
                        }
                    }
                }, false, false);
            }
            // me.chart.redraw();
            // reload styles
            me.setStyles();
        },


        /**
         * sets/updates the chart styles based on entry and data
         */
        setStyles: function () {
            var me = this, vm = me.getViewModel(), entry = vm.get('eEntry') || vm.get('entry'), colors,
                isWidget = me.getView().isWidget, plotLines = [],

                isColumnStacked = false, isColumnOverlapped = false,
                isPieColumn = false, isDonut = false, isPie = false, is3d = false;

            if (!entry) { return; }

            // NGFW-11448 - apply 12/24 time format on graphs
            var timeLabelFormats;
            var timestampFormat = Rpc.directData('rpc.translations.timestamp_fmt');
            if (timestampFormat && timestampFormat.indexOf('h:i:s a') >= 0 ) {
                // 12 hours format
                timeLabelFormats = {
                    xAxis: {
                        second: '%l:%M:%S %p',
                        minute: '%l:%M %p',
                        hour: '%l:%M %p',
                        day: '%Y-%m-%d'
                    },
                    tooltip: {
                        second: '%Y-%m-%d, %l:%M:%S %p, %l:%M:%S %p',
                        minute: '%Y-%m-%d, %l:%M %p',
                        hour: '%Y-%m-%d, %l:%M %p',
                        day: '%Y-%m-%d'
                    },
                    dataGrouping: {
                        millisecond: ['%Y-%m-%d, %l:%M:%S %p', '%Y-%m-%d, %l:%M:%S %p', '-%l:%M:%S %p'],
                        second: ['%Y-%m-%d, %l:%M:%S %p', '%Y-%m-%d, %l:%M:%S %p', '-%l:%M:%S %p'],
                        minute: ['%Y-%m-%d, %l:%M %p', '%Y-%m-%d, %l:%M %p', '-%l:%M %p'],
                        hour: ['%Y-%m-%d, %l:%M %p', '%Y-%m-%d, %l:%M %p', '%Y-%m-%d, %l:%M %p'],
                        day: ['%Y-%m-%d']
                    }
                };
            } else {
                // 24 hours format
                timeLabelFormats = {
                    xAxis: {
                        second: '%H:%M:%S',
                        minute: '%H:%M',
                        hour: '%H:%M',
                        day: '%Y-%m-%d'
                    },
                    tooltip: {
                        second: '%Y-%m-%d, %H:%M:%S, %H:%M:%S',
                        minute: '%Y-%m-%d, %H:%M',
                        hour: '%Y-%m-%d, %H:%M',
                        day: '%Y-%m-%d'
                    },
                    dataGrouping: {
                        millisecond: ['%Y-%m-%d, %H:%M:%S', '%Y-%m-%d, %H:%M:%S', '-%H:%M:%S'],
                        second: ['%Y-%m-%d, %H:%M:%S', '%Y-%m-%d, %H:%M:%S', '-%H:%M:%S'],
                        minute: ['%Y-%m-%d, %H:%M', '%Y-%m-%d, %H:%M', '-%H:%M'],
                        hour: ['%Y-%m-%d, %H:%M', '%Y-%m-%d, %H:%M', '%Y-%m-%d, %H:%M'],
                        day: ['%Y-%m-%d']
                    }
                };
            }

            var isPieGraph = entry.get('type') === 'PIE_GRAPH';
            var isTimeGraph = entry.get('type').indexOf('TIME_GRAPH') >= 0;

            if (isTimeGraph) {
                isColumnStacked = entry.get('timeStyle').indexOf('STACKED') >= 0;
                isColumnOverlapped = entry.get('timeStyle').indexOf('OVERLAPPED') >= 0;
            }

            if (isPieGraph) {
                isPieColumn = entry.get('pieStyle').indexOf('COLUMN') >= 0;
                isPie = entry.get('pieStyle').indexOf('COLUMN') < 0;
                isDonut = entry.get('pieStyle').indexOf('DONUT') >= 0;
                is3d = entry.get('pieStyle').indexOf('3D') >= 0;
            }

            if (isWidget) {
                if (Ung.dashboardSettings.theme === 'DEFAULT' && Ext.isArray(entry.get('colors')) && entry.get('colors').length > 0) {
                    colors = Ext.clone(entry.get('colors'));
                } else {
                    colors = Ext.clone(Theme[Ung.dashboardSettings.theme].colors);
                }
            } else {
                if (!entry.get('colors') || entry.get('colors').length === 0) {
                    colors = Ext.clone(Theme.DEFAULT.colors);
                } else {
                    colors = Ext.clone(entry.get('colors'));
                }
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
                            [0, Highcharts.Color(color).setOpacity((isWidget && Ung.dashboardSettings.theme === 'DARK') ? 0.9 : 0.4).get('rgba')],
                            [1, Highcharts.Color(color).setOpacity((isWidget && Ung.dashboardSettings.theme === 'DARK') ? 0.3 : 0.8).get('rgba')]
                        ]
                    };
                });
            }

            if (isColumnOverlapped) {
                for (var i = 0; i < colors.length; i += 1) {
                    colors[i] = new Highcharts.Color(colors[i]).setOpacity(0.5).get('rgba');
                }
            }

            if (isTimeGraph) {
                Ext.Array.each(me.chart.series, function (serie, idx) {
                    serie.update({
                        color: colors[idx],
                        lineColor: colors[idx],
                        fillColor: {
                            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                            stops: [
                                [0, Highcharts.Color(colors[idx]).setOpacity(0.7).get('rgba')],
                                [1, Highcharts.Color(colors[idx]).setOpacity(0.1).get('rgba')]
                            ]
                        },
                        pointPadding: isColumnOverlapped ? 0.1 * idx : 0.1
                    }, false);
                });

                Ext.Array.each(me.data, function (row) {
                    var t;
                    if(row.time_trunc){
                        if(row.time_trunc.time){
                            t = row.time_trunc.time;
                        }else{
                            t = row.time_trunc;
                        }
                    }
                    if (t && t % (24 * 3600 * 1000) === 0) {
                        var d = Util.clientToServerDate(new Date(t));
                        d = Ext.Date.add(d, Ext.Date.MINUTE, d.getTimezoneOffset());
                        plotLines.push({
                            value: d,
                            width: 1,
                            dashStyle: 'Dash',
                            color: isWidget ? (Ung.dashboardSettings.theme !== 'DARK' ? '#EEE' : '#444') : '#EEE',
                            label: {
                                text: Ext.Date.format(d, 'Y-m-d'),
                                y: 15,
                                style: {
                                    fontSize: '12px',
                                    color: '#999'
                                }
                            }
                        });
                    }
                });
            }

            var settings = {
                exporting: {
                    chartOptions: {
                        title: {
                            text: '<span style="color: #999;">' + entry.get('category') + '/</span>' + entry.get('title')
                        },
                        subtitle: {
                            text: entry.get('description')
                        }
                    }
                },
                chart: {
                    type: me.getChartType(entry),
                    zoomType: isTimeGraph ? 'x' : undefined,
                    panning: isTimeGraph,
                    panKey: 'ctrl',
                    options3d: {
                        enabled: is3d,
                        alpha: isPieColumn ? 30 : 50,
                        beta: isPieColumn ? 5 : 0
                    }
                },
                subtitle: {
                    text: null
                },
                colors: colors,
                // scrollbar: {
                //     enabled: isTimeGraph
                // },
                plotOptions: {
                    series: {
                        stacking: isColumnStacked ? 'normal' : undefined
                    },
                    // pie graphs
                    pie: {
                        innerSize: isDonut ? '40%' : 0,
                        colors: colors
                        //borderColor: '#666666'
                    },
                    // time graphs
                    spline: {
                        shadow: true,
                        dataGrouping: {
                            groupPixelWidth: 8,
                            approximation: entry.get('approximation') || 'sum',
                            dateTimeLabelFormats: timeLabelFormats.dataGrouping
                        },
                    },
                    // time graphs
                    areaspline: {
                        // shadow: true,
                        // fillOpacity: 0.3,
                        dataGrouping: {
                            groupPixelWidth: 8,
                            approximation: entry.get('approximation') || 'sum',
                            dateTimeLabelFormats: timeLabelFormats.dataGrouping
                        },
                    },
                    column: {
                        borderWidth: isColumnOverlapped ? 1 : 0,
                        pointPlacement: isTimeGraph ? 'on' : null, // time
                        // pointPadding: 0.01,
                        colorByPoint: isPieColumn, // pie
                        grouping: !isColumnOverlapped,
                        groupPadding: isColumnOverlapped ? 0.1 : 0.15,
                        // shadow: !isColumnOverlapped,
                        dataGrouping: isTimeGraph ? 50 : undefined
                    }
                },
                xAxis: {
                    visible: !isPie,
                    minRange: !isPieColumn ? 10 * 60 * 1000 : undefined, // minzoom = 10 minutes
                    // tickPixelInterval: 50,
                    type: isTimeGraph ? 'datetime' : 'category',
                    crosshair: isTimeGraph ? {
                        width: 1,
                        dashStyle: 'ShortDot'
                    } : false,
                    plotLines: plotLines,
                    dateTimeLabelFormats: timeLabelFormats.xAxis
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
                    title: {
                        text: (!isWidget && isPie && isPieGraph) ? (TableConfig.getColumnHumanReadableName(entry.get('pieGroupColumn')) + '<br/> <span style="font-size: 12px;">[' + entry.get('pieGroupColumn') + '] by ' + entry.get('units') + '</span>') : null,
                        style: { fontSize: '18px', fontWeight: 400 }
                    },
                    enabled: !(isWidget && isPieGraph),
                    layout: (isPieGraph && isPie) ? 'vertical' : 'horizontal',
                    align: (isPieGraph && isPie) ? 'left' : 'center',
                    verticalAlign: (isPieGraph && isPie) ? 'top' : 'bottom'
                },
                tooltip: {
                    split: ((isWidget && me.chart.series.length <= 3) || !isWidget) && isTimeGraph,
                    dateTimeLabelFormats: timeLabelFormats.tooltip
                }
            };

            Highcharts.merge(true, settings, isWidget ? Theme[Ung.dashboardSettings.theme] : Theme.DEFAULT);
            me.chart.update(settings, true);

            // force redraw for column charts, NGFW-11349
            if (isPieColumn) { me.chart.redraw(); }
        },

        onPointClick: function (event) {
            var me = this, vm = me.getViewModel(),
                column = vm.get('entry.pieGroupColumn'),
                value = event.point.value;

            Ext.fireEvent('addglobalcondition', column, value);
        }
    }
});
