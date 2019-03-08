/**
 * This shows when Reports App is not installed or is disabled
 * From here user can install or enable the Reports App
 */
Ext.define('Ung.view.reports.NoReports', {
    extend: 'Ext.container.Container',
    alias: 'widget.noreports',

    layout: {
        type: 'vbox',
        align: 'middle'
    },

    margin: 20,
    items: [{
        xtype: 'component',
        style: {
            textAlign: 'center',
            color: '#555',
            fontFamily: '"Roboto Condensed", sans-serif',
        },
        padding: 20,
        bind: {
            html: '<img src="/icons/apps/reports.svg" width=150 height=150/> <br/><br/> <h1 style="font-weight: 100;">' +
                  '{!reportsAppStatus.installed ? "' + 'Reports App is not installed!'.t() + '" : "' + 'Reports App is disabled!'.t() + '"}'.t() +
                  '</h1>'
        }
    }, {
        xtype: 'button',
        scale: 'medium',
        focusable: false,
        iconCls: 'fa fa-download fa-lg',
        text: '<strong>' + 'Install Reports'.t() + '</strong>',
        handler: 'installReports',
        hidden: true,
        bind: { hidden: '{reportsAppStatus.installed}' }
    }, {
        xtype: 'button',
        scale: 'medium',
        focusable: false,
        iconCls: 'fa fa-check fa-lg',
        text: '<strong>' + 'Enable Reports'.t() + '</strong>',
        handler: 'enableReports',
        hidden: true,
        bind: { hidden: '{!reportsAppStatus.installed || reportsAppStatus.enabled}' }
    }, {
        xtype: 'component',
        itemId: 'wait',
        style: {
            textAlign: 'center'
        },
        html: '<i class="fa fa-spinner fa-spin fa-lg"></i> <br/><br/> Please wait ...',
        hidden: true
    }],

    controller: {
        installReports: function (btn) {
            var wait = this.getView().down('#wait');

            btn.setHidden(true);
            wait.setHidden(false);
            Rpc.asyncData('rpc.appManager.instantiate', 'reports')
                .then(function (result, ex) {
                    if (ex) {
                        Ext.Msg.alert('Error', ex.message);
                        return;
                    }
                    if(Util.isDestroyed(wait)){
                        return;
                    }

                    Ung.app.reportscheck();

                    // refresh apps
                    rpc.appsViews = rpc.appManager.getAppsViews();
                    Ext.getStore('policies').loadData(rpc.appsViews);
                    Ung.app.getGlobalController().getAppsView().getController().getApps();

                    wait.setHidden(true);

                });
        },

        enableReports: function (btn) {
            var wait = this.getView().down('#wait'),
                appManager = rpc.appManager.app('reports');
            btn.setHidden(true);
            wait.setHidden(false);

            var runStateWantState = 'RUNNING',
                runStateWait = 10000,
                runStateDelay = 100,
                runStateTask = null;

            runStateTask = new Ext.util.DelayedTask(function() {
                appManager.getRunState(function (result, ex) {
                    if (ex) {
                        Util.handleException(ex);
                        return false;
                    }
                    if(Util.isDestroyed(wait)){
                        return;
                    }
                    runStateWait = runStateWait - runStateDelay;
                    if (result !== runStateWantState){
                        runStateTask.delay(runStateDelay);
                    } else {
                        Ung.app.reportscheck();

                        // refresh apps
                        rpc.appsViews = rpc.appManager.getAppsViews();
                        Ext.getStore('policies').loadData(rpc.appsViews);
                        Ung.app.getGlobalController().getAppsView().getController().getApps();

                        wait.setHidden(true);
                    }
                });
            });

            appManager.start(function (result, ex) {
                if (ex) {
                    Ext.Msg.alert('Error', ex.message);
                    runStateWantState = 'INITIALIZED';
                    runStateTask.delay(runStateDelay);
                    return;
                }
                runStateTask.delay( this.runStateDelay );
            });
        }
    }
});
