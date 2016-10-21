// test
Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    autoCreateViewport: false,
    name: 'Ung',

    rpc: null,

    requires: [
        'Ung.rpc.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.form.field.VTypes'
    ],


    stores: [
        'Policies', 'Metrics', 'Stats', 'Reports', 'Widgets', 'Conditions', 'Countries', 'Categories', 'UnavailableApps'
    ],

    defaultToken : '',

    init: function () {

    },

    launch: function () {
        var me = this;
        Rpc.rpc = me.rpc;

        Ext.getStore('policies').loadData(me.rpc.appsViews);

        // need to check if reports enabled an load it if so
        if (me.rpc.nodeManager.node('untangle-node-reports')) {
            Rpc.loadReports().then(function (reports) {
                Ext.getStore('reports').loadData(reports.list);
                me.loadMainView();
            });
        } else {
            me.loadMainView();
        }

        // uncomment this to retreive the class load order inside browser
        // Ung.Util.getClassOrder();
    },

    loadMainView: function () {
        try {
            Ung.app.setMainView('Ung.view.main.Main');
        } catch (ex) {
            Ung.Util.exceptionToast(ex);
            return;
        }

        // start metrics
        Ung.util.Metrics.start();

        // destroy app loader
        Ext.Function.defer(function() {
            Ext.get('app-loader').addCls('removing');
            Ext.Function.defer(function () {
                Ext.get('app-loader').destroy();
            }, 200);
        }, 150);
    }
});
