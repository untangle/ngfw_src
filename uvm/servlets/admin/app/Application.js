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

    // mainView: 'Ung.view.main.Main',

    init: function () {

    },

    launch: function () {
        var me = this;
        Rpc.rpc = me.rpc;

        Ext.getStore('policies').loadData(me.rpc.appsViews);

        // Ext.get('app-message').setHtml('Reports ...');

        // need to check if reports enabled an load it if so

        //Rpc.loadDashboardSettings().then(function(settings) {

            //Ext.getStore('widgets').loadData(settings.widgets.list);

            if (me.rpc.nodeManager.node('untangle-node-reports')) {
                Rpc.loadReports().then(function (reports) {
                    Ext.getStore('reports').loadData(reports.list);
                    me.loadMainView();
                });
            } else {
                me.loadMainView();
            }
            // me.loadMainView();
            // me.getView().setSettings(settings);
            // if (vm.get('reportsInstalled')) {
            //     // load unavailable apps needed for showing the widgets
            //     console.time('unavailApps');
            //     rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
            //         if (ex) { Ung.Util.exceptionToast(ex); return false; }

            //         Ext.getStore('unavailableApps').loadRawData(result.map);
            //         Ext.getStore('widgets').loadData(settings.widgets.list);
            //         console.timeEnd('unavailApps');
            //         me.loadWidgets();
            //     });
            // } else {
            //     Ext.getStore('widgets').loadData(settings.widgets.list);
            //     me.loadWidgets();
            // }
            // me.populateMenus();
        //});

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
        Ext.get('app-loader').destroy();

        // Ext.Function.defer(function() {
        //     Ext.get('app-loader').addCls('removing');
        //     Ext.Function.defer(function () {
        //         Ext.get('app-loader').destroy();
        //     }, 300);
        // }, 1500);
    }
});
