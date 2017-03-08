Ext.define('Ung.apps.phishblocker.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-phish-blocker',

    control: {
        '#': {
            afterrender: 'getSettings',
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

        var lastUpdate = v.appManager.getLastUpdate();
        // var lastUpdateCheck = v.appManager.getLastUpdateCheck();
        // var signatureVersion = v.appManager.getSignatureVersion();


        if (lastUpdate) {
            vm.set('lastUpdate', 'Phish Blocker email signatures were last updated'.t() + ': <strong>' + Ext.util.Format.date(new Date(lastUpdate.time), 'timestamp_fmt'.t()) + '</strong>');
        } else {
            vm.set('lastUpdate', 'Phish Blocker email signatures were last updated'.t() + ': <strong>' + 'never'.t() + '</strong>');
        }
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    }


});
