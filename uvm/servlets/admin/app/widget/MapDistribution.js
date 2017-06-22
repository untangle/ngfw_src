Ext.define('Ung.widget.MapDistribution', {
    extend: 'Ext.container.Container',
    alias: 'widget.mapdistributionwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',
    visible: false,

    layout: 'fit',

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 10,
    geographyManager: rpc.UvmContext.geographyManager(),

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        style: { height: '40px' },
        html: '<h1>' + 'Map Distribution'.t() + '</h1>' +
            '<div class="actions"><a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a></div>'
    }, {
        xtype: 'component',
        height: 260,
        // style: {
        //     right: 0,
        //     bottom: 0
        // },
        reference: 'mapCmp'
    }],

    fetchData: function (cb) {
        var me = this, data = [];

        if (me.chart && me.geographyManager) {
            me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>');
            me.geographyManager.getGeoSessionStats(function (result, ex) {
                me.chart.hideLoading();
                cb();
                if (ex) { Util.handleException(ex); return; }
                for (i = 0; i < result.length; i += 1) {
                    var bubbleSize = 0;
                    if (result[i].kbps)
                        bubbleSize = Math.round(result[i].kbps * 100) / 100;
                    data.push({
                        lat: result[i].latitude,
                        lon: result[i].longitude,
                        z: bubbleSize,
                        country: result[i].country,
                        sessionCount: result[i].sessionCount
                    });
                }
                me.chart.series[1].setData(data, true, false);
            });
        } else {
            cb();
        }
    },

    listeners: {
        afterrender: function (view) {
            // view.setLoading(true);
            if (!Highcharts.map['custom/world']) {
                Ext.Loader.loadScript({
                    url: '/highcharts-5.0.9/world.js',
                    onLoad: function () {
                        // view.setLoading(false);
                        view.renderMap(view);
                    }
                });
            } else {
                view.renderMap(view);
            }
        },
        resize: function (view) {
            if (view.chart) {
                view.chart.reflow();
            }
        }
    },

    renderMap: function (me) {
        me.chart = new Highcharts.Map({
            chart : {
                type: 'map',
                renderTo: me.lookup('mapCmp').getEl().dom,
                margin: [5, 5, 5, 5],
                spacing: [5, 5, 5, 5],
                // backgroundColor: 'transparent',
                map: 'custom/world'
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
            plotOptions: {
                series: {
                    allowPointSelect: true,
                    point: {
                        events: {
                            click: function () {
                                if (this.country) {
                                    Ung.app.redirectTo('#sessions/?serverCountry=' + this.country);
                                }
                            }
                        }
                    }
                }
            },
            series : [{
                name: 'Countries',
                color: '#E0E0E0',
                enableMouseTracking: false
            }, {
                type: 'mapbubble',
                minSize: 10,
                maxSize: 50,
                zMax: 500
            }],
            tooltip: {
                headerFormat: '',
                pointFormat: '<strong>{point.country}</strong><br/><strong>{point.sessionCount}</strong> ' + 'sessions'.t() + '<br/><strong>{point.z}</strong> kB/s',
                hideDelay: 0
            }
        });
    }
});
