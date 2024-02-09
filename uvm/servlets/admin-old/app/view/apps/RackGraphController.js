Ext.define('Ung.view.apps.RackGraphController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.rackgraph',

    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
    },

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },
    listen: {
        store: {
            '#metrics': {
                datachanged: 'onAddPoint'
            }
        }
    },
    updateMetricsCount: 0,

    onAfterRender: function (view) {
        // when instance changes reset metrics
        var me = this;
        me.getViewModel().bind('{instanceId}', function() {
            me.setMetrics();
        });


        // create chart
        this.chart = new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                //zoomType: 'x',
                renderTo: view.down('#rackgraph').getEl().dom,
                spacing: [2, 0, 2, 0],
                margin: [10, 2, 2, 2],
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            lang: { noData: '' },
            exporting: {
                enabled: false
            },
            title: null,
            credits: {
                enabled: false
            },
            colors: ['#8EFB6C'],
            xAxis: [{
                type: 'datetime',
                labels: {
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#999',
                        fontSize: '10px'
                    },
                    y: 12
                },
                visible: false
            }],
            yAxis: {
                allowDecimals: false,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                min: 0,
                minRange: 10,
                visible: false
            },
            legend: {
                enabled: false
            },
            tooltip: {
                backgroundColor: 'rgba(255, 255, 255, 0.7)',
                borderWidth: 1,
                borderColor: 'rgba(0, 0, 0, 0.1)',
                style: {
                    fontFamily: 'Source Sans Pro',
                    padding: '5px',
                    fontSize: '10px',
                },
                useHTML: true,
                hideDelay: 0,
                shadow: false,
                headerFormat: '',
                pointFormat: '{point.y} ' + 'sessions'.t()
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.2,
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
                            lineWidthPlus: 1,
                            halo: {
                                size: 1
                            }
                        }
                    }
                }
            },
            series: [{
                name: 'Sessions'.t(),
                data: []
            }]
        });
    },

    setMetrics: function () {
        var me = this, metricsCmps = [], stats = [], i,
            metrics = me.getViewModel().get('metrics');

        if (!metrics || metrics.length == 0){
            return;
        }

        // add metrics labels if not added
        if (me.getView().down('#metrics').items.length === 0) {
            for (i = 0; i < metrics.length; i += 1) {
                metricsCmps.push({
                    bind: {
                        html: '<p>' + metrics[i].displayName + ': <span>{stats.'+ i +'}</span></p>'
                    }
                });
            }
            me.getView().down('#metrics').add(metricsCmps);
        }

        // just update the stats values
        for (i = 0; i < metrics.length; i += 1) {
            stats.push(metrics[i].value < 0 ? 0 : metrics[i].value);
        }
        me.getViewModel().set('stats', stats);


        var data = [], 
            time = Util.getMilliseconds();
        time = Math.round(time/1000) * 1000;

        for (i = -17; i <= -1; i += 1) {
            data.push({
                x: time + i * 10000,
                y: 0
            });
        }

        var graphValArr = metrics.filter(function (metric) {
            return metric.name === 'live-sessions';
        });

        if( graphValArr == false ){
            this.chart = null;
            return;
        }

        if (graphValArr && graphValArr[0]) {
            data.push({
                x: time,
                y: graphValArr[0].value < 0 ? 0 : graphValArr[0].value
            });
        }
        me.chart.series[0].setData(data);

    },

    // update metrics/graph when metrics changed
    onAddPoint: function () {
        var me = this, vm = me.getViewModel(),
            appInstanceMetrics = Ext.getStore('metrics').findRecord('appId', vm.get('instanceId'));

        if (!me.chart || !appInstanceMetrics) {
            return;
        }
        var metrics = appInstanceMetrics.get('metrics').list, stats = [];
        if (!metrics || metrics.length == 0) {
            return;
        }

        for (var i = 0; i < metrics.length; i += 1) {
            stats.push(metrics[i].value < 0 ? 0 : metrics[i].value);
        }
        vm.set('stats', stats);

        var graphValArr = metrics.filter(function (metric) {
            return metric.name === 'live-sessions';
        });

        if( graphValArr == false ){
            this.chart = null;
            return;
        }

        if (graphValArr && graphValArr[0]) {
            me.chart.series[0].addPoint({
                x: Date.now(),
                y: graphValArr[0].value < 0 ? 0 : graphValArr[0].value
            }, true, true);
        }

        // random for testing
        // newVal = Math.floor(Math.random() * 20) + 15;

    }
});
