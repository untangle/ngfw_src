Ext.define('Ung.apps.captiveportal.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-captive-portal',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#activeUsers': {
            afterrender: 'getActiveUsers'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            console.log(result);
            vm.set('settings', result);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    getActiveUsers: function (cmp) {
        var vm = this.getViewModel(),
            grid = (cmp.getXType() === 'gridpanel') ? cmp : cmp.up('grid');
        grid.setLoading(true);
        this.getView().appManager.getActiveUsers(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('activeUsers', result.list);
        });
    },

    previewCaptivePage: function () {
        var vm = this.getViewModel();
        if (vm.get('instance.targetState') !== 'RUNNING') {
            Ext.MessageBox.alert('Captive Portal is Disabled',
                    'You must turn on the Captive Portal to preview the Captive Page.'.t());
            return;
        }
        window.open('/capture/handler.py/index?appid=' + vm.get('instance.id') , '_blank');
    },

    configureLocalDirectory: function (btn) {
        var vm = this.getViewModel();
        var policyId = vm.get('policyId');
        var authType = this.getViewModel().get('settings.authenticationType');
        var dircon = rpc.appManager.app('directory-connector');

        switch (authType) {
            case 'LOCAL_DIRECTORY':
                Ung.app.redirectTo('#config/localdirectory');
                break;
            case 'GOOGLE':
                if (dircon == null) this.showMissingServiceWarning();
                else Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/google');
                break;
            case 'FACEBOOK':
                if (dircon == null) this.showMissingServiceWarning();
                else Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/facebook');
                break;
            case 'RADIUS':
                if (dircon == null) this.showMissingServiceWarning();
                else Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/radius');
                break;
            case 'ACTIVE_DIRECTORY':
                if (dircon == null) this.showMissingServiceWarning();
                else Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/activedirectory');
                break;
            default: return;
        }
    },

    showMissingServiceWarning: function() {
        Ext.MessageBox.alert('Service Not Installed'.t(), 'The Directory Connector application must be installed to use this feature.'.t());
    }

});
