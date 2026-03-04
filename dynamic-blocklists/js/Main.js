Ext.define('Ung.apps.dynamic-blocklists.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-dynamic-blocklists',
    layout: 'border',
    
    viewModel: {
        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/dynamic-blocklist', false);

            if (!panel.messageListenerAttached) {
                panel.messageListenerAttached = true;

                panel.statusHandler = function (event) {
                    var data = event.data;
                    if (!data.action) return;

                    var vm = panel.getViewModel();
                    if (!vm) return;

                    if (data.action.appName === 'dynamic-blocklists' && data.action.type === 'REFRESH_APP_STATUS') {
                        vm.set('instance.targetState', data.action.targetState);

                        var appState = vm.get('state');
                        if (appState && typeof appState.detect === 'function') {
                            appState.detect();
                            rpc.appsViews = rpc.appManager.getAppsViews();
                            Ext.getStore('policies').loadData(rpc.appsViews);
                            Ung.app.getGlobalController().getAppsView().getController().getApps();
                        }
                    }
                };
                window.addEventListener('message', panel.statusHandler);
            }
        },

        destroy: function (panel) {
            if (panel.statusHandler) {
                window.removeEventListener('message', panel.statusHandler);
                panel.statusHandler = null;
                panel.messageListenerAttached = false;
            }
        }
    },

    items: [
        Field.iframeHolder
    ]
});