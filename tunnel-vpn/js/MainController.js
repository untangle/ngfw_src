Ext.define('Ung.apps.tunnel-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
            vm.set('settings', result);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (me.validateSettings() != true) return;

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
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    validateSettings: function() {
        return(true);
    },

    uploadFile: function(cmp) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=uploadConfigFileName]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
        }
        Ext.MessageBox.wait("Uploading File...".t(), "Please Wait".t());
        form.submit({
            url: "/tunnel-vpn/uploadConfig",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                Ext.MessageBox.alert('Success'.t(), 'The configuration has been imported.'.t());
                me.getSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                Ext.MessageBox.alert('Failure'.t(), 'Import failure'.t() + ": " + action.result.code);
            }, this)
        });
    },

});

