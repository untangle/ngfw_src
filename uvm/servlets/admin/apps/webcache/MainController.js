Ext.define('Ung.apps.webcache.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-webcache',

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
            delete result.javaClass;
            me.getViewModel().set('statistics', result);
        });
    }
});
