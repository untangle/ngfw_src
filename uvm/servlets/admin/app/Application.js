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
    context: 'ADMIN', // set the app context ADMIN or REPORTS
    conditionsContext: 'REPORTS', // can be REPORTS or DASHBOARD because they don't share conditions

    /**
     * used to know when data is loaded for the first time
     * when app has loaded and fetched initial resources (e.g. reports)
     * */
    initialLoad: false,

    launch: function () {

        // Build policies tree
        Ext.getStore('policies').loadData(Rpc.directData('rpc.appsViews'));
        Ext.getStore('policiestree').build();

        Ext.Deferred.parallel([
            Rpc.directPromise('rpc.companyName'),
            Rpc.directPromise('rpc.hostname')
        ], this)
        .then(function(result){
            if(Util.isDestroyed(window)){
                return;
            }
            window.document.title = result[0] + (result[1] ? ' - ' + result[1] : '');
        }, function(ex) {
        });

        Ext.get('app-loader').destroy();

        // Start metrics
        Metrics.start();

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
        var mainView = Ung.app.getMainView();

        if(!Rpc.directData('rpc.appManager.app', 'reports')){
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
        }else{
            rpc.reportsManager = Rpc.directData('rpc.appManager.app("reports").getReportsManager');

            Ext.Deferred.parallel([
                Rpc.asyncPromise('rpc.reportsManager.getReportEntries'),
                Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications')
            ]).then(function (result) {
                if(Util.isDestroyed(mainView)){
                    return;
                }
                if (result[0]) { Ext.getStore('reports').loadData(result[0].list); }
                if (result[1]) { Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result[1].list)); }

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
                    enabled: Rpc.directData('rpc.appManager.app("reports").getRunState') === 'RUNNING'
                });
            }, function (ex) {
            });

        }
    }
});
