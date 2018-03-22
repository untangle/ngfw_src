Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',

    requires: [
        'Ung.cmp.GoogleDrive'
    ],

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    backupNow: function (btn) {
        Ext.MessageBox.wait('Backing up... This may take a few minutes.'.t(), 'Please wait'.t());

        Rpc.asyncData(this.getView().appManager, 'sendBackup')
        .then(function(result){
            Ext.MessageBox.hide();
        },function(ext){
            Util.handleException(ex);
            Ext.MessageBox.hide();
        });
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);
            var googleDrive = new Ung.cmp.GoogleDrive();
            vm.set( 'googleDriveIsConfigured', googleDrive.isConfigured() );
            vm.set( 'googleDriveConfigure', function(){ googleDrive.configure(vm.get('policyId')); });

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

        if (!Util.validateForms(v)) {
            return;
        }

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

    }

});
