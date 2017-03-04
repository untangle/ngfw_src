Ext.define('Ung.apps.captiveportal.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-captiveportal',

    control: {
        '#activeUsers': {
            afterrender: 'getActiveUsers'
        }
    },

    getActiveUsers: function (cmp) {
        var vm = this.getViewModel(),
            grid = (cmp.getXType() === 'gridpanel') ? cmp : cmp.up('grid');
        grid.setLoading(true);
        this.getView().appManager.getActiveUsers(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('activeUsers', result.list);
        });
    }
});
