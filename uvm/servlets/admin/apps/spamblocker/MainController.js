Ext.define('Ung.apps.spamblocker.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-spamblocker',

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
        var lastUpdateCheck = v.appManager.getLastUpdateCheck();
        if (lastUpdate) {
            vm.set('lastUpdate', 'Spam Blocker was last updated'.t() + ': <strong>' + Ext.util.Format.date(new Date(lastUpdate.time), 'timestamp_fmt'.t()) + '</strong>');
        } else {
            vm.set('lastUpdate', 'Spam Blocker was last updated'.t() + ': <strong>' + 'never'.t() + '</strong>');
        }
        if (lastUpdateCheck) {
            vm.set('lastUpdateCheck', 'Spam Blocker last checked for updates'.t() + ': <strong>' + Ext.util.Format.date(new Date(lastUpdateCheck.time), 'timestamp_fmt'.t()) + '</strong>');
        } else {
            vm.set('lastUpdateCheck', 'Spam Blocker last checked for updates'.t() + ': <strong>' + 'never'.t() + '</strong>');
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
