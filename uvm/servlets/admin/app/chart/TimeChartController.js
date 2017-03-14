Ext.define('Ung.chart.TimeChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.timechart',

    control: {
        '#': {
            resize: 'onResize'
        }
    },

    styles: {
        'LINE': { styleType: 'spline' },
        'AREA': { styleType: 'areaspline' },
        'AREA_STACKED': {styleType: 'areaspline', stacking: true },
        'BAR': {styleType: 'column', grouping: true },
        'BAR': {styleType: 'column', grouping: true },
        'BAR_OVERLAPPED': {styleType: 'column', overlapped: true },
        'BAR_OVERLAPPED': {styleType: 'column', overlapped: true },
        'BAR_STACKED': {styleType: 'column', stacking: true }
    },
    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
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
        case 'BAR':
        case 'BAR_OVERLAPPED':
        case 'BAR_OVERLAPPED':
        case 'BAR_STACKED':
            console.log('here');
            type = 'column';
            break;
        default:
            type = 'areaspline';
        }
        return type;
    },

    onAfterRender: function (view) {
        var me = this;
        this.entry = view.getViewModel().get('entry') || this.getView().getEntry();

        console.log(me.entry.get('timeStyle'));

        this.chart = new Highcharts.StockChart({
            chart: {
                type: me.setChartType(me.entry.get('timeStyle')),
                zoomType: 'x',
                renderTo: view.lookupReference('timechart').getEl().dom,
                //marginBottom: !forDashboard ? 40 : 50,
                marginTop: 10,
                //marginRight: 0,
                //marginLeft: 0,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                animation: false,
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            title: null,
            lang: {
                noData: ''
            },
            noData: {
                style: {
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#CCC'
                }
            },
            colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,
            navigator: {
                enabled: false
            },
            rangeSelector : {
                enabled: false,
                inputEnabled: false
            },
            scrollbar: {
                enabled: false
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
                        fontFamily: 'Roboto Condensed',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 700
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0
            }],
            yAxis: {
                allowDecimals: true,
                min: 0,
                minRange: me.entry.get('units') === 'percent' ? 100 : 0.4,
                maxRange: me.entry.get('units') === 'percent' ? 100 : undefined,
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
                endOnTick: me.entry.get('units') !== 'percent',
                tickInterval: me.entry.get('units') === 'percent' ? 20 : undefined,
                maxPadding: 0,
                opposite: false,
                labels: {
                    align: 'right',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Roboto Condensed',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: -10,
                    y: 5,
                    formatter: function() {
                        var finalVal = this.value;

                        if (me.entry.get('units') === 'bytes/s') {
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
                    text: me.entry.get('units'),
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
                y: 10,
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
                shared: true,
                animation: true,
                followPointer: true,
                backgroundColor: 'rgba(255, 255, 255, 0.7)',
                borderWidth: 1,
                borderColor: 'rgba(0, 0, 0, 0.1)',
                style: {
                    textAlign: 'right',
                    fontFamily: 'Source Sans Pro',
                    padding: '5px',
                    fontSize: '10px',
                    marginBottom: '40px'
                },
                //useHTML: true,
                hideDelay: 0,
                shadow: false,
                headerFormat: '<span style="font-size: 11px; line-height: 1.5; font-weight: bold;">{point.key}</span><br/>',
                pointFormatter: function () {
                    var str = '<span>' + this.series.name + '</span>';
                    if (me.entry.get('units') === 'bytes' || me.entry.get('units') === 'bytes/s') {
                        str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span>';
                    } else {
                        str += ': <spanstyle="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span> ' + me.entry.get('units');
                    }
                    return str + '<br/>';
                }

            },
            plotOptions: {
                column: {
                    edgeWidth: 0,
                    borderWidth: 0,
                    pointPadding: 0,
                    groupPadding: 0.2,
                    dataGrouping: {
                        groupPixelWidth: 40
                    },
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
                    lineWidth: 0
                },
                spline: {
                    lineWidth: 2,
                    softThreshold: false
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
            }
        });
    },

    onResize: function () {
        this.chart.reflow();
    },

    onSetSeries: function (data) {
        //var newData = [], me = this;
        var me = this, newData = [], i, j, _column,
            timeDataColumns = Ext.clone(this.entry.get('timeDataColumns')),
            style = this.entry.get('timeStyle'),
            colors = this.entry.get('colors') || this.defaultColors;

            /*
            timeDataColumns = Ext.clone(me.getViewModel().get('entry.timeDataColumns')),
            style = me.getViewModel().get('entry.timeStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
            */

        this.getView().setLoading(false);
        // this.getView().lookupReference('loader').hide(); // hide chart loader


        if (!timeDataColumns) {
            timeDataColumns = [];
            for (j = 0; j < data.length; j += 1) {
                for (_column in data[j]) {
                    if (data[j].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
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
            for (j = 0; j < data.length; j += 1) {
                newData.push([
                    data[j].time_trunc.time,
                    data[j][timeDataColumns[i]] || 0
                ]);
            }
            if (this.chart.get('series-' + timeDataColumns[i])) {
                this.chart.get('series-' + timeDataColumns[i]).update({
                    data: newData,
                    type: me.styles[style].styleType,
                    color: colors[i] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPadding: me.styles[style].overlapped ? (timeDataColumns.length <= 3 ? 0.10 : 0.075) * i : 0.1,
                    visible: !(timeDataColumns[i] === 'total' && me.styles[style].stacking)
                });
            } else {
                this.chart.addSeries({
                    id: 'series-' + timeDataColumns[i],
                    name: timeDataColumns[i],
                    data: newData,
                    type: me.styles[style].styleType,
                    color: colors[i] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPlacement: 0,
                    pointPadding: me.styles[style].overlapped ? (timeDataColumns.length <= 3 ? 0.12 : 0.075) * (i + 1) : 0.1,
                    visible: !(timeDataColumns[i] === 'total' && me.styles[style].stacking)
                }, false, false);
            }
        }
        this.chart.redraw();

    },

    onSetStyle: function () {
        var me = this,
            style = me.getViewModel().get('entry.timeStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
        if (this.chart) {
            var seriesLength = this.chart.series.length;
            this.chart.series.forEach( function (series, idx) {
                series.update({
                    type: me.styles[style].styleType,
                    color: colors[idx] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPadding: me.styles[style].overlapped ? (seriesLength <= 3 ? 0.15 : 0.075) * idx : 0.1,
                    visible: !(series.name === 'total' && me.styles[style].stacking)
                });
            });
        }
    },

    onBeginFetchData: function() {
        // this.getView().lookupReference('loader').show();
        this.getView().setLoading(true);
    }

});
