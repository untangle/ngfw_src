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
        this.getView().appManager.sendBackup(function(result, ex) {
            Ext.MessageBox.hide();
            if (ex) { Util.exceptionToast(ex); return; }
        });
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
            var googleDrive = new Ung.cmp.GoogleDrive();
            vm.set( 'googleDriveIsConfigured', googleDrive.isConfigured() );
            vm.set( 'googleDriveConfigure', function(){ googleDrive.configure(); });
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

    }

});
