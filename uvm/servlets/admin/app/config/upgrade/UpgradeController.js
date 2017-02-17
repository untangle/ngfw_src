Ext.define('Ung.config.upgrade.UpgradeController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.upgrade',

    control: {
        '#': {
            afterrender: 'loadSettings'
        }
    },

    loadSettings: function (view) {

        // view.getViewModel().bind('{settings.autoUpgradeHour}', function (value) {
        //     console.log(value);
        // });

        view.down('progressbar').wait({
            interval: 500,
            text: 'Checking for upgrades...'.t()
        });
        // this.checkUpgrades();
        console.log(view.getViewModel().get('settings'));
    },

    saveSettings: function () {
        var me = this;
        console.log(me.getViewModel().get('settings'));
        me.getView().setLoading('Saving ...');
        rpc.systemManager.setSettings(function (result, ex) {
            me.getView().setLoading(false);
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            Ung.Util.successToast('Upgrade Settings'.t() + ' saved!');
        }, me.getViewModel().get('settings'));

    },

    checkUpgrades: function () {
        var v = this.getView();
        rpc.systemManager.upgradesAvailable(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            v.down('progressbar').reset();
            v.down('progressbar').hide();
        });
    },

    onUpgradeTimeChange: function (field, value) {
        this.getViewModel().set('settings.autoUpgradeHour', value.getHours());
        this.getViewModel().set('settings.autoUpgradeMinute', value.getMinutes());
    }

});