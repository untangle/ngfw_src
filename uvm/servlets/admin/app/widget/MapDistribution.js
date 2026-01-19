Ext.define('Ung.widget.MapDistribution', {
    extend: 'Ext.container.Container',
    alias: 'widget.mapdistributionwidget',

    controller: 'widget',

    border: false,
    baseCls: 'widget',
    visible: false,
    layout: 'fit',

    refreshIntervalSec: 10,

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
        reference: 'mapCmp'
    }],

    // Function to convert bytes to the appropriate unit
    bytesToDynamicSize : function(bytes) {
        if (bytes < 1000) {
            return 10;
        } else if (bytes < 1000 * 1000) {
            return 30;
        } else if (bytes < 1000 * 1000 * 1000) {
            return 50;
        } else if (bytes < 1000 * 1000 * 1000 * 1000) {
            return 70;
        } else {
            return 90;
        }
    },

    fetchData: function (cb) {
        var me = this;
        if (me.chart) {
            me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>');
            Rpc.asyncData('rpc.UvmContext.geographyManager.getGeoSessionStats')
            .then( function(result){
                if(Util.isDestroyed(me.chart)){
                    return;
                }
                var data = [], i;
                me.chart.hideLoading();
                cb();
                for (i = 0; i < result.length; i += 1) {
                    var bubbleSize = 0, numberPart = 0, unitPart = "", convertedValue="";
                    if (result[i].bps){
                        convertedValue = Util.bytesRenderer(result[i].bps, true);
                        numberPart = parseFloat(convertedValue);
                        unitPart = convertedValue.replace(/[\d\s.]+/g, '');
                        bubbleSize = result[i].bps;
                    }
                    data.push({
                        country: result[i].country,
                        z: bubbleSize == 0 ? 0 : me.bytesToDynamicSize(bubbleSize),
                        numberPart: numberPart,
                        unitPart: unitPart,
                        sessionCount: result[i].sessionCount
                    });
                }
                me.chart.series[1].setData(data, true, false);
            },function(ex){
                Util.handleException(ex);
            });
        } else {
            cb();
        }
    },

    listeners: {
        afterrender: function (view) {
            if (!Highcharts.map['custom/world']) {
                Ext.Loader.loadScript({
                    url: '/highcharts-6.0.2/world.js',
                    onLoad: function () {
                        if(Util.isDestroyed(view)){
                            return;
                        }
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
                joinBy: ['iso-a2', 'country'],
                minSize: 10,
                maxSize: 50,
                zMax: 500
            }],
            tooltip: {
                headerFormat: '',
                pointFormatter: function () {
                    var str = '<strong>' + this.country + '</strong><br/><strong>' + this.sessionCount + '</strong> ' + 'sessions'.t() + '<br/>';
                    str += '<strong>' + this.numberPart + '</strong> ' + this.unitPart ;
                    return str;
                },
                hideDelay: 0
            }
        });
    }
});
