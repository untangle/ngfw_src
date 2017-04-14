// test
Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    namespace: 'Ung',

    autoCreateViewport: false,
    name: 'Ung',

    rpc: null,

    controllers: ['Global'],

    defaultToken : '',

    mainView: 'Ung.view.main.Main',

    init: function () {
        // if (!rpc.translations.decimal_sep) { rpc.translations.decimal_sep = '.'; }
        // if (!rpc.translations.thousand_sep) { rpc.translations.thousand_sep = ','; }
        // if (!rpc.translations.date_fmt) { rpc.translations.date_fmt = 'Y-m-d'; }
        // if (!rpc.translations.timestamp_fmt) { rpc.translations.timestamp_fmt = 'Y-m-d h:i:s a'; }
    },

    launch: function () {
        window.document.title = rpc.companyName + (rpc.hostname ? ' - ' + rpc.hostname : '');
        Ext.get('app-loader').destroy();

        Ext.getStore('policies').loadData(rpc.appsViews);
        Metrics.start();

        Ext.fireEvent('afterlaunch'); // used in Main view ctrl

        try {
            rpc.reportsManager = rpc.appManager.app('reports').getReportsManager();
        } catch (ex) {
            // console.log(ex);
        }

        if (rpc.reportsManager) {
            // reports installed
            Ext.Deferred.parallel([
                Rpc.asyncPromise('rpc.dashboardManager.getSettings'),
                Rpc.asyncPromise('rpc.reportsManager.getReportEntries'),
                Rpc.asyncPromise('rpc.reportsManager.getUnavailableApplicationsMap'),
                Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications')
            ]).then(function (result) {
                Ung.dashboardSettings = result[0];

                Ext.getStore('widgets').loadData(result[0].widgets.list);
                if (result[1]) {
                    Ext.getStore('reports').loadData(result[1].list);
                }
                if (result[2]) {
                    Ext.getStore('unavailableApps').loadRawData(result[2].map);
                }
                if (result[3]) {
                    Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result[3].list));
                }

                Ext.getStore('reportstree').build();
                Ext.getStore('policiestree').build();
                Ext.fireEvent('init');
            }, function (ex) {
                console.log(ex);
            });
        } else {
            Rpc.asyncData('rpc.dashboardManager.getSettings')
                .then(function (result) {
                    Ung.dashboardSettings = result;
                    Ext.getStore('widgets').loadData(result.widgets.list);

                    Ext.getStore('policiestree').build();
                    Ext.fireEvent('init');
                });
        }
    }
});
