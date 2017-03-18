Ext.define('Ung.apps.intrusionprevention.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-intrusion-prevention',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);

        v.appManager.getLastUpdateCheck(function (result, ex) {
            if (ex) { Util.exceptionToast(ex); return; }
            var lastUpdateCheck = result;
            vm.set('lastUpdateCheck', (lastUpdateCheck !== null && lastUpdateCheck.time !== 0 ) ? Util.timestampFormat(lastUpdateCheck) : "Never".t() );

            v.appManager.getLastUpdate(function (result, ex) {
                if (ex) { Util.exceptionToast(ex); return; }
                var lastUpdate = result;
                vm.set('lastUpdate', ( lastUpdateCheck != null && lastUpdateCheck.time !== 0 && lastUpdate !== null && lastUpdate.time !== 0 ) ? Util.timestampFormat(lastUpdate) : "Never".t() );
            });
        });


        Ext.Ajax.request({
            url: "/webui/download",
            method: 'POST',
            params: {
                type: "IntrusionPreventionSettings",
                arg1: "load",
                arg2: v.appManager.getAppSettings().id
            },
            scope: v.appManager,
            timeout: 600000,
            success: function(response){
                // this.openSettings.call(this, Ext.decode( response.responseText ) );
                vm.set('settings', Ext.decode( response.responseText ) );
                console.log(vm.get('settings'));
                v.setLoading(false);
            },
            failure: function(response){
                // this.openSettings.call(this, null );
                vm.set('settings', null );
                v.setLoading(false);
            }
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

});
