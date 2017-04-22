Ext.define('Ung.apps.spamblockerlite.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-spam-blocker-lite',

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
            if (ex) { Util.handleException(ex); return; }
            vm.set('settings', result);
            v.lookup('predefinedStrength').setValue(result.smtpConfig.strength);
        });

        var lastUpdate = v.appManager.getLastUpdate();
        var lastUpdateCheck = v.appManager.getLastUpdateCheck();
        if (lastUpdate) {
            vm.set('lastUpdate', 'Spam Blocker Lite was last updated'.t() + ': <strong>' + Ext.util.Format.date(new Date(lastUpdate.time), 'timestamp_fmt'.t()) + '</strong>');
        } else {
            vm.set('lastUpdate', 'Spam Blocker Lite was last updated'.t() + ': <strong>' + 'never'.t() + '</strong>');
        }
        if (lastUpdateCheck) {
            vm.set('lastUpdateCheck', 'Spam Blocker Lite last checked for updates'.t() + ': <strong>' + Ext.util.Format.date(new Date(lastUpdateCheck.time), 'timestamp_fmt'.t()) + '</strong>');
        } else {
            vm.set('lastUpdateCheck', 'Spam Blocker Lite last checked for updates'.t() + ': <strong>' + 'never'.t() + '</strong>');
        }
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    setStrength: function (combo, newValue, oldValue) {
        var me = this, vm = this.getViewModel();
        if (!Ext.Array.contains([30, 33, 35, 43, 50], newValue)) {
            me.lookup('predefinedStrength').setValue(0);
            vm.set('strength', oldValue/10);
        } else {
            vm.set('settings.smtpConfig.strength', newValue);
        }
    }

});
