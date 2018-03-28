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
    context: 'ADMIN', // set the context

    /**
     * used to know when data is loaded for the first time
     * when app has loaded and fetched initial resources (e.g. reports)
     * */
    initialLoad: false,

    launch: function () {
        window.document.title = rpc.companyName + (rpc.hostname ? ' - ' + rpc.hostname : '');
        Ext.get('app-loader').destroy();

        // Start metrics
        Metrics.start();

        // Fetch policies and build policies tree
        Ext.getStore('policies').loadData(rpc.appsViews);
        Ext.getStore('policiestree').build();

        Ext.fireEvent('afterlaunch'); // used in Main view ctrl

        document.addEventListener('paste', function(evt){
            var currentHash =  window.location.hash;
            if (currentHash != ''){
                return;
            }
            var url = evt.clipboardData.getData('text/plain');
            if(url && url.indexOf('#') > -1){
                var hashStart=url.substring(url.indexOf('#'));
                Ung.app.redirectTo(hashStart);
            }
        });
    },

    /**
     * Method called when app loads or when
     * Reports App is installed/removed or enabled/disabled
     */
    reportscheck: function () {
        var mainView = Ung.app.getMainView(),
            reportsApp = rpc.appManager.app('reports');

        if (!reportsApp) {
            rpc.reportsManager = null;
            Ext.getStore('reports').loadData([]);
            Ext.getStore('reportstree').build();

            if (!Ung.app.initialLoad) {
                Ung.app.initialLoad = true;
                Ext.fireEvent('initialload');
            }

            mainView.getViewModel().set('reportsAppStatus', {
                installed: false,
                enabled: false
            });
            return;
        }

        rpc.reportsManager = reportsApp.getReportsManager();

        Ext.Deferred.parallel([
            Rpc.asyncPromise('rpc.reportsManager.getReportEntries'),
            Rpc.asyncPromise('rpc.reportsManager.getUnavailableApplicationsMap'),
            Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications')
        ]).then(function (result) {
            if (result[0]) { Ext.getStore('reports').loadData(result[0].list); }
            if (result[1]) { Ext.getStore('unavailableApps').loadRawData(result[1].map); }
            if (result[2]) { Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result[2].list)); }

            // build reports tree
            Ext.getStore('reportstree').build();

            /**
             * this is needed to initialize global conditions
             * because the query binding fires before reports have been loaded
             */
            if (!Ung.app.initialLoad) {
                Ung.app.initialLoad = true;
                Ext.fireEvent('initialload');
            }

            /**
             * Set the reportsAppStatus viewmodel prop.
             * This is watched in different places when changes, and the view updates based on the status
             */
            mainView.getViewModel().set('reportsAppStatus', {
                installed: true,
                enabled: reportsApp.getRunState() === 'RUNNING'
            });

        }, function (ex) {
            console.log(ex);
        });
    }

});
