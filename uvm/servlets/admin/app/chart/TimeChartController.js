Ext.define('Ung.chart.TimeChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.timechart',

    onAfterRender: function (view) {
        var me = this, vm = this.getViewModel();

        // fetch data first
        // vm.bind('{entry}', me.fetchData, me);
        vm.bind('{entry.timeStyle}', me.setStyles, me);
        vm.bind('{entry.colors}', me.setStyles, me);
        vm.bind('{entry.timeDataInterval}', me.fetchData, me);
    },

    setChartType: function (timeStyle) {
        var type = 'areaspline';
        switch (timeStyle) {
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
        return type;
    },

    /**
     * fetches report data via rpc
     */
    fetchData: function () {
        var me = this, vm = this.getViewModel();
        if (!vm.get('entry')) { return; }
        if (me.chart) {
            me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>');
        } else {
            me.getView().setLoading(true);
        }
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                        vm.get('entry').getData(),
                        vm.get('startDate'),
                        vm.get('tillNow') ? null : vm.get('endDate'), -1)
            .then(function(result) {
                if (me.chart) {
                    me.chart.hideLoading();
                } else {
                    me.getView().setLoading(false);
                }

                me.data = result.list;
                me.setSeries();

                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').getController().setCurrentData(result.list);
                }
            });
    },

    /**
     * creates (normalize) series definition based on entry and data
     */
    setSeries: function () {
        if (!this.data) { return; }

        var me = this, vm = this.getViewModel(),
            timeDataColumns = Ext.clone(vm.get('entry.timeDataColumns')),
            i, j, newData, series = [];

        if (!timeDataColumns) {
            timeDataColumns = [];
            for (j = 0; j < me.data.length; j += 1) {
                for (var _column in me.data[j]) {
                    if (me.data[j].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                        timeDataColumns.push(_column);
                    }
                }
            }
        } else {
            for (i = 0; i < timeDataColumns.length; i += 1) {
                timeDataColumns[i] = timeDataColumns[i].split(' ').splice(-1)[0];
            }
        }

        for (i = 0; i < timeDataColumns.length; i += 1) {
            newData = [];
            for (j = 0; j < me.data.length; j += 1) {
                newData.push([
                    me.data[j].time_trunc.time,
                    me.data[j][timeDataColumns[i]] || 0
                ]);
            }
            series.push({
                name: timeDataColumns[i],
                data: newData
            });
        }

        if (!this.chart) {
            this.buildChart(series);
        } else {
            while(this.chart.series.length > 0) {
                this.chart.series[0].remove(false);
            }
            series.forEach(function (serie) {
                me.chart.addSeries(serie, false, false);
            });
            me.setStyles();
            me.chart.redraw();
        }

    },

    /**
     * updates the styles of the chart based on report entry conditions
     */
    setStyles: function () {
        if (!this.chart) { return ;}

        var me = this, entry = this.getViewModel().get('entry'),
            isStacked = entry.get('timeStyle').indexOf('STACKED') >= 0,
            isOverlapped = entry.get('timeStyle').indexOf('OVERLAPPED') >= 0,
            colors = Ext.clone(entry.get('colors')) || Ext.clone(Util.defaultColors);

        if (colors) {
            for (var i = 0; i < colors.length; i += 1) {
                colors[i] = isOverlapped ? new Highcharts.Color(colors[i]).setOpacity(0.5).get('rgba') : colors[i];
            }
        }

        me.chart.update({
            chart: {
                type: me.setChartType(entry.get('timeStyle'))
            },
            colors: colors,
            plotOptions: {
                series: {
                    stacking: isStacked ? 'normal' : undefined,
                    dataGrouping: {
                        approximation: entry.get('approximation') || 'sum'
                    }
                },
                spline: {
                    dataGrouping: {
                        groupPixelWidth: 16
                    },
                },
                areaspline: {
                    fillOpacity: 0.3,
                    dataGrouping: {
                        groupPixelWidth: 16
                    },
                },
                column: {
                    shadow: !isOverlapped,
                    grouping: !isOverlapped,
                    groupPadding: 0.15,
                    dataGrouping: {
                        groupPixelWidth: isStacked ? 50 : 80
                    },
                    tooltip: {
                        enabled: false
                    }
                }
            }
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
    },

    buildChart: function (series) {
        var me = this, entry = me.getViewModel().get('entry'), isWidget = me.getView().getIsWidget();

        me.chart = new Highcharts.StockChart({
            chart: {
                type: me.setChartType(entry.get('timeStyle')),
                zoomType: 'x',
                renderTo: me.getView().lookupReference('timechart').getEl().dom,
                animation: false,
                // marginBottom: isWidget ? 0 : 10,
                marginTop: 10,
                //marginRight: 0,
                //marginLeft: 0,
                // padding: [0, 0, 0, 0],
                // backgroundColor: 'transparent',
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            title: null,
            lang: {
                noData: 'some text'
            },
            noData: {
                style: {
                    padding: 0,
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#999'
                },
                useHTML: true
            },
            // colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,
            navigator: {
                enabled: false
            },
            rangeSelector : {
                enabled: false,
                inputEnabled: false
            },
            scrollbar: {
                enabled: false
                // enabled: !isWidget // disabled if is widget
            },
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            xAxis: [{
                type: 'datetime',
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                crosshair: {
                    width: 1,
                    dashStyle: 'ShortDot',
                    color: 'rgba(100, 100, 100, 0.3)'
                },
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
                minPadding: 0
            }],
            yAxis: {
                allowDecimals: true,
                min: 0,
                minRange: entry.get('units') === 'percent' ? 100 : 0.4,
                maxRange: entry.get('units') === 'percent' ? 100 : undefined,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                //tickPixelInterval: 50,
                tickLength: 5,
                tickWidth: 1,
                //tickPosition: 'inside',
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: entry.get('units') !== 'percent',
                tickInterval: entry.get('units') === 'percent' ? 20 : undefined,
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
                    y: 4,
                    formatter: function() {
                        var finalVal = this.value;

                        if (entry.get('units') === 'bytes/s') {
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
                    align: 'high',
                    offset: -10,
                    y: 3,
                    //rotation: 0,
                    //text: entry.units,
                    text: entry.get('units'),
                    //textAlign: 'left',
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 600
                    }
                }
            },
            legend: {
                enabled: true,
                padding: 5,
                margin: 0,
                y: isWidget ? 15 : 0,
                lineHeight: 12,
                itemDistance: 10,
                itemStyle: {
                    fontFamily: 'Source Sans Pro',
                    color: '#555',
                    fontSize: '12px',
                    fontWeight: 600
                },
                symbolHeight: 7,
                symbolWidth: 7,
                symbolRadius: 3
            },
            tooltip: {
                // enabled: true,
                split: true,
                distance: 30,
                padding: 5,
                // shared: false,
                hideDelay: 0,
                pointFormat: '<span style="color: {point.color}">\u25CF</span> <strong>{series.name}</strong>: {point.y}<br/>',
                // useHTML: true,
                // headerFormat: '<span style="font-size: 11px; line-height: 1.5; font-weight: bold;">{point.key}</span><br/>',
                // pointFormatter: function () {
                //     var str = '<span>' + this.series.name + '</span>';
                //     if (entry.get('units') === 'bytes' || entry.get('units') === 'bytes/s') {
                //         str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span>';
                //     } else {
                //         str += ': <spanstyle="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span> ' + entry.get('units');
                //     }
                //     return str + '<br/>';
                // }
            },
            plotOptions: {
                column: {
                    // edgeWidth: 0,
                    // borderWidth: 0,
                    // pointPadding: 0,
                    // groupPadding: 0.2,
                    borderWidth: 1,
                    pointPlacement: 'on',
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
                    lineWidth: 1,
                    tooltip: {
                        split: true
                    }
                },
                spline: {
                    lineWidth: 2,
                    // softThreshold: false
                },
                series: {
                    animation: false,
                    marker: {
                        enabled: true,
                        radius: 0,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 2,
                                radius: 4,
                                radiusPlus: 2
                            }
                        }
                    },
                    states: {
                        hover: {
                            enabled: true,
                            lineWidthPlus: 0,
                            halo: {
                                size: 2
                            }
                        }
                    }
                }
            },
            series: series
        });

        me.setStyles();
    },

    onResize: function () {
        if (this.chart) {
            this.chart.reflow();
        }
    }
});
