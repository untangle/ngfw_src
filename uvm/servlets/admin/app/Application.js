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

    launch: function () {
        window.document.title = rpc.companyName + (rpc.hostname ? ' - ' + rpc.hostname : '');
        Ext.get('app-loader').destroy();

        Ext.getStore('policies').loadData(rpc.appsViews);
        Metrics.start();

        Ext.fireEvent('afterlaunch'); // used in Main view ctrl

        // check for reports app running in the first policy
        rpc.reportsRunning = false;
        var reportsApp = rpc.appManager.app('reports');
        if (reportsApp != null) {
            rpc.reportsRunning = reportsApp.getRunState() === 'RUNNING';
            rpc.reportsManager = reportsApp.getReportsManager();
        }

        if (rpc.reportsManager && rpc.reportsRunning) {
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
