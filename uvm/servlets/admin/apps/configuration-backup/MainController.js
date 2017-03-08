Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',

    backupNow: function (btn) {
        Ext.MessageBox.wait('Backing up... This may take a few minutes.'.t(), 'Please wait'.t());
        this.getView().appManager.sendBackup(function(result, ex) {
            Ext.MessageBox.hide();
            if (ex) { Util.exceptionToast(ex); return; }
        });
    }
});
