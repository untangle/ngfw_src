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
            Ext.MessageBox.alert('Captive Portal is Disabled'.t(),
                'You must turn on the Captive Portal to preview the Captive Page.'.t());
            return;
        }

        var custfile = this.getViewModel().get('settings.customFilename');
        var pagetype = vm.get('settings.pageType');

        if ( (pagetype == 'CUSTOM') && ((custfile == null) || (custfile.length === 0)) ) {
            Ext.MessageBox.alert('Missing Custom Captive Page'.t(),
                'You must upload a custom captive page to use this feature.'.t());
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
    },

    uploadCustomFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=upload_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
            }
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Upload Success'.t(), action.result.msg);
                this.getViewModel().set('settings.customFilename', action.result.msg);
                this.setSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Upload Failure'.t(), action.result.msg);
            }, this)
        });
    },

    removeCustomFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=remove_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=custom_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            return;
            }
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Remove Success'.t(), action.result.msg);
                this.getViewModel().set('settings.customFilename', null);
                this.setSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Remove Failure'.t(), action.result.msg);
            }, this)
        });
    },

    logoutUser: function(view, row, col, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();

        var netaddr = record.get("userNetAddress");
        var macaddr = record.get("userMacAddress");
        v.setLoading('Logging Out User...'.t());

        if ( (vm.get('settings.useMacAddress') == true) && (macaddr != null) && (macaddr.length > 12) ) {
            v.appManager.userAdminMacLogout(Ext.bind(function(result, ex) {
                if (exception) { Util.exceptionToast(ex); return; }
                // this gives the app a couple seconds to process the disconnect before we refresh the list
                var timer = setTimeout(function() {
                    me.getActiveUsers(view);
                    v.setLoading(false);
                },500);
            }, this), macaddr);
        } else {
            v.appManager.userAdminNetLogout(Ext.bind(function(result, ex) {
                if (ex) { Util.exceptionToast(ex); return; }
                // this gives the app a couple seconds to process the disconnect before we refresh the list
                var timer = setTimeout(function() {
                    me.getActiveUsers(view);
                    v.setLoading(false);
                },500);
            }, this), netaddr);
        }
    }

});
