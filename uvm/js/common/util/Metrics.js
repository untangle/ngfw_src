Ext.define('Ung.util.Metrics', {
    alternateClassName: 'Metrics',
    singleton: true,
    frequency: 10000,
    interval: null,
    running: false,

    start: function () {
        var me = this;
        Ext.getStore('stats').loadRawData({});
        Ext.getStore('metrics').loadData({});
        me.stop();
        me.run();
        me.interval = window.setInterval(function () {
            me.run();
        }, me.frequency);
    },

    stop: function () {
        if (this.interval !== null) {
            window.clearInterval(this.interval);
        }
    },

    run: function () {
        var data = [];
        Rpc.asyncData('rpc.metricManager.getMetricsAndStats')
        .then( function(result){
            if(Util.isDestroyed(data)){
                return;
            }
            data = [];

            Ext.getStore('stats').loadRawData(result.systemStats);

            for (var appId in result.metrics) {
                if (result.metrics.hasOwnProperty(appId)) {
                    data.push({
                        appId: appId,
                        metrics: result.metrics[appId]
                    });
                }
            }

            Ext.getStore('metrics').loadData(data);
        },function(ex){
            Util.handleException(ex);
        });
    }

});
