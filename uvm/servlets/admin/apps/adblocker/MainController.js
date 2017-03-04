Ext.define('Ung.apps.adblocker.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-adblocker',

    control: {
        '#status': {
            afterrender: 'statusAfterRender'
        }
    },

    statusAfterRender: function () {
        var me = this,
            vm = this.getViewModel();
        me.getView().appManager.getSettings(function (result, ex) {
            vm.set('settings', result);
        });
    }
});
