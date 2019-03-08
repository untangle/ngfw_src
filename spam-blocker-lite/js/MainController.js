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
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise(v.appManager, 'getLastUpdateCheck'),
            Rpc.asyncPromise(v.appManager, 'getLastUpdate')
        ]).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set({
                settings: result[0],
                lastUpdateCheck: result[1].time ? Renderer.timestamp(result[1].time) : 'Never'.t(),
                lastUpdate: result[2].time ? Renderer.timestamp(result[2].time) : 'Never'.t()
            });

            v.lookup('predefinedStrength').setValue(result[0].smtpConfig.strength);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
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
