Ext.define('Ung.widget.CpuLoadController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.cpuload',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            resize: 'onResize'
        }
    },

    listen: {
        store: {
            '#stats': {
                datachanged: 'addPoint'
            }
        }
    },

    onAfterRender: function (view) {
        setTimeout(function () {
            view.removeCls('adding');
        }, 100);

        this.lineChart = new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: view.lookup('cpulinechart').getEl().dom,
                marginBottom: 15,
                marginTop: 20,
                //padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                animation: true,
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            title: null,
            credits: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            xAxis: [{
                type: 'datetime',
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
                        fontFamily: 'Source Sans Pro',
                        color: '#999',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0
            }],
            yAxis: {
                min: 0,
                minRange: 2,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickAmount: 4,
                tickLength: 5,
                tickWidth: 1,
                tickPosition: 'inside',
                opposite: false,
                labels: {
                    align: 'left',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#999',
                        fontSize: '11px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: 1,
                    y: -2
                },
                title: null
            },
            legend: {
                enabled: false
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
                    str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span>';
                    return str + '<br/>';
                }
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.15,
                    lineWidth: 2
                },
                series: {
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
            series: [{
                name: 'load',
                data: (function () {
                    var data = [], time = Date.now(), i;
                    time = Math.round(time/1000) * 1000;
                    for (i = -6; i <= 0; i += 1) {
                        data.push({
                            x: time + i * 10000,
                            y: 0
                        });
                    }
                    return data;
                }())
            }]
        });

        this.gaugeChart = new Highcharts.Chart({
            chart: {
                type: 'gauge',
                renderTo: view.lookupReference('cpugaugechart').getEl().dom,
                height: 140,
                margin: [-20, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    margin: '0 auto'
                }
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
                data: [0]
            }]
        });
        this.addPoint();
    },

    onResize: function () {
        this.lineChart.reflow();
        this.gaugeChart.reflow();
    },

    addPoint: function () {
        if (!this.gaugeChart || !this.lineChart) {
            return;
        }
        var store = Ext.getStore('stats');
        var vm = this.getViewModel(),
            stats = store.first().getData(),
            medLimit = stats.numCpus + 1,
            highLimit = stats.numCpus + 4;

        this.lineChart.yAxis[0].update({
            minRange: stats.numCpus
        });

        this.gaugeChart.yAxis[0].update({
            max: highLimit + 1,
            plotBands: [{
                from: 0,
                to: medLimit,
                color: 'rgba(112, 173, 112, 1)',
                innerRadius: '100%',
                outerRadius: '105%'
            }, {
                from: medLimit,
                to: highLimit,
                color: 'rgba(255, 255, 0, 1)',
                innerRadius: '100%',
                outerRadius: '105%'
            }, {
                from: highLimit,
                to: highLimit + 1,
                color: 'rgba(255, 0, 0, 1)',
                innerRadius: '100%',
                outerRadius: '105%'
            }]
        });

        this.lineChart.series[0].addPoint({
            x: Date.now(),
            y: store.first().getData().oneMinuteLoadAvg
        }, true, true);

        this.gaugeChart.series[0].points[0].update(stats.oneMinuteLoadAvg <= highLimit + 1 ? stats.oneMinuteLoadAvg : highLimit + 1, true);

        if (stats.oneMinuteLoadAvg < medLimit) {
            vm.set('loadLabel', 'low'.t());
        }
        if (stats.oneMinuteLoadAvg > medLimit) {
            vm.set('loadLabel', 'medium'.t());
        }
        if (stats.oneMinuteLoadAvg > highLimit) {
            vm.set('loadLabel', 'high'.t());
        }

    }
});
