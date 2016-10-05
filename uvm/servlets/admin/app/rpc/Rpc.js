Ext.define('Ung.rpc.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    loadWebUi: function() {
        var deferred = new Ext.Deferred(), me = this;
        this.getWebuiStartupInfo(function (result, exception) {
            if (exception) { deferred.reject(exception); }
            Ext.apply(me, result);
            console.log('hahaha');


            String.prototype.translate = function() {
                var record = Rpc.rpc.translations[this.valueOf()] ;
                if(record === null) {
                    alert('Missing translation for: ' + this.valueOf()); // Key is not found in the corresponding messages_<locale>.properties file.
                    return this.valueOf(); // Return key name as placeholder
                } else {
                    var value = record;
                }
                return value;
            };

            //console.log('Dashboard'.translate());

            if (me.nodeManager.node('untangle-node-reports')) {
                me.reportsManager = me.nodeManager.node('untangle-node-reports').getReportsManager();
            }
            deferred.resolve();
        });
        return deferred.promise;
    },

    loadReports: function () {
        var deferred = new Ext.Deferred();
        if (this.rpc.nodeManager.node('untangle-node-reports')) {
            this.rpc.reportsManager = this.rpc.nodeManager.node('untangle-node-reports').getReportsManager();
            this.rpc.reportsManager.getReportEntries(function (result, exception) {
                if (exception) { deferred.reject(exception); }
                deferred.resolve(result);
            });

        } else {
            deferred.reject('Reports not installed!');
        }
        return deferred.promise;
    },

    loadDashboardSettings: function () {
        var deferred = new Ext.Deferred();
        this.rpc.dashboardManager.getSettings(function (settings, exception) {
            if (exception) { deferred.reject(exception); }
            //Ung.app.dashboardSettings = result;

            //Ung.app.getStore('Widgets').loadRawData(result.widgets);
            //Ext.get('app-loader-text').setHtml('Loading widgets ...');
            deferred.resolve(settings);
        });
        return deferred.promise;
    },

    getReportData: function (entry, timeframe) {
        var deferred = new Ext.Deferred();
        this.rpc.reportsManager.getDataForReportEntry(function(result, exception) {
            if (exception) { deferred.reject(exception); }
            deferred.resolve(result);
        }, entry, timeframe, -1);
        return deferred.promise;
    },

    getRpcNode: function (nodeId) {
        var deferred = new Ext.Deferred();
        this.rpc.nodeManager.node(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, nodeId);
        return deferred.promise;
    },

    getNodeSettings: function (node) {
        var deferred = new Ext.Deferred();
        node.getSettings(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },

    setNodeSettings: function (node, settings) {
        console.log(settings);
        var deferred = new Ext.Deferred();
        node.setSettings(function (result, ex) {
            console.log(result);
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, settings);
        return deferred.promise;
    },




    readRecords: function () {
        console.log('read');
        return {};
    }


});