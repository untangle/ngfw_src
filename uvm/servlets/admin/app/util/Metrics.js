Ext.define('Ung.util.Metrics', {
    singleton: true,
    frequency: 3000,
    interval: null,
    running: false,

    start: function () {
        var me = this;
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
        rpc.metricManager.getMetricsAndStats(Ext.bind(function(result, exception) {
            if (exception) { console.log(exception); }

            //console.log(result.metrics);
            data = [];

            Ext.getStore('stats').first().set(result.systemStats);

            for (var nodeId in result.metrics) {
                if (result.metrics.hasOwnProperty(nodeId)) {
                    data.push({
                        nodeId: nodeId,
                        metrics: result.metrics[nodeId]
                    });
                }
            }

            Ext.getStore('metrics').loadData(data);

            //Ext.getStore('metrics').loadData([result.metrics]);
        }));
    }

});