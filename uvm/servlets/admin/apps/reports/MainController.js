Ext.define('Ung.apps.reports.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-reports',

    control: {
        '#': {
            afterrender: 'getSettings',
            activate: 'checkGoogleDrive'
        }
    },

    getSettings: function () {
        console.log('afterrender');
        var me = this, v = me.getView(), vm = me.getViewModel();
        vm.set('isExpertMode', rpc.isExpertMode);

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

    checkGoogleDrive: function () {
        var vm = this.getViewModel(), googleDriveConfigured = false, directoryConnectorLicense, directoryConnectorApp, googleManager,
            licenseManager = rpc.UvmContext.licenseManager();
        try {
            directoryConnectorLicense = licenseManager.isLicenseValid('directory-connector');
            directoryConnectorApp = rpc.appManager.app('directory-connector');
            if (directoryConnectorLicense && directoryConnectorApp) {
                googleManager = directoryConnectorApp.getGoogleManager();
                if (googleManager && googleManager.isGoogleDriveConnected()) {
                    googleDriveConfigured = true;
                }
            }
        } catch (ex) {
            Util.exceptionToast(ex);
        }
        vm.set('googleDriveConfigured', googleDriveConfigured);
    },

    reportTypeRenderer: function (value) {
        switch (value) {
            case 'TEXT': return 'Text'.t();
            case 'TIME_GRAPH': return 'Time Graph'.t();
            case 'TIME_GRAPH_DYNAMIC': return 'Time Graph Dynamic'.t();
            case 'PIE_GRAPH': return 'Pie Graph'.t();
            case 'EVENT_LIST': return 'Event List'.t();
        }
    },

    isDisabledCategory: function (view, rowIndex, colIndex, item, record) {
        if (!Ext.getStore('categories').findRecord('displayName', record.get('category'))) {
            return true;
        }
        return false;
    },

    configureGoogleDrive: function () {
        if (rpc.appManager.app('directory-connector')) {
            Ung.app.redirectTo('#service/directory-connector/google');
        } else {
            Ext.MessageBox.alert('Error'.t(), 'Google Drive requires Directory Connector application.'.t());
        }
    },

    onUpload: function (btn) {
        var formPanel = btn.up('form');
        formPanel.submit({
            waitMsg: 'Please wait while data is imported...'.t(),
            success: function () {
                formPanel.down('filefield').reset();
                // Ext.MessageBox.alert('Succeeded'.t(), 'Upload Data Succeeded'.t());
                Util.successToast('Upload Data Succeeded'.t());
            },
            failure: function (form, action) {
                // var errorMsg = 'Upload Data Failed'.t() + ' ' + action.result;
                // Ext.MessageBox.alert('Failed'.t(), errorMsg);
                Util.exceptionToast('Upload Data Failed'.t() + ' ' + action.result);
            }
        });
    },

    // Users
    templatesRenderer: function (value) {
        var vm = this.getViewModel(),
            templates = vm.get('settings.emailTemplates.list');
        console.log(value);
        Ext.Array.each(templates, function (template) {

        });
    }

});
