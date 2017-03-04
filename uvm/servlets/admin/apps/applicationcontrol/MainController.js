Ext.define('Ung.apps.applicationcontrol.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-applicationcontrol',

    control: {
        '#status': {
            afterrender: 'statusAfterRender'
        }
    },

    statusAfterRender: function () {
        var me = this;
        me.getView().appManager.getStatistics(function (result, ex) {
            if (ex) { Util.exceptionToast(ex); return; }
            console.log(result);
            me.getViewModel().set('statistics', result);
        });
    }
});
