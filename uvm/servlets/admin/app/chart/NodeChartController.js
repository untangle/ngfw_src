Ext.define('Ung.chart.NodeChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.nodechart',

    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
    },

    onAfterRender: function (view) {
        this.chart = new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                //zoomType: 'x',
                renderTo: view.lookupReference('nodechart').getEl().dom,
                marginBottom: 15,
                marginRight: 0,
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
                        color: '#555',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0
            }],
            yAxis: {
                allowDecimals: false,
                min: 0,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                minRange: 1,
                tickPixelInterval: 30,
                tickLength: 5,
                tickWidth: 1,
                //tickPosition: 'inside',
                showFirstLabel: false,
                showLastLabel: true,
                maxPadding: 0,
                opposite: false,
                labels: {
                    align: 'right',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#555',
                        fontSize: '11px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: -10,
                    y: 5
                },
                title: {
                    align: 'high',
                    offset: -10,
                    y: 3,
                    //rotation: 0,
                    //text: entry.units,
                    text: 'sessions',
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
                    str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span> sessions';
                    return str + '<br/>';
                }
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.15,
                    lineWidth: 1
                },
                spline: {
                    lineWidth: 2,
                    softThreshold: false
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
                data: (function () {
                    var data = [], time = Date.now(), i;
                    try {
                        time = rpc.systemManager.getMilliseconds();
                    } catch (e) {
                        console.log('Unable to get current millis.');
                    }
                    time = Math.round(time/1000) * 1000;
                    for (i = -39; i <= 0; i += 1) {
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

    onResize: function () {
        this.chart.reflow();
    },

    onAddPoint: function () {
        var newVal = this.getViewModel().get('metrics').filter(function (metric) {
            return metric.name === 'live-sessions';
        })[0].value || 0;

        this.chart.series[0].addPoint({
            x: Date.now(),
            y: newVal
        }, true, true);

    }
});
