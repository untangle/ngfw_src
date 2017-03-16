Ext.define('Ung.apps.wanfailover.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wan-failover',

    control: {
        '#': {
            beforerender: 'getSettings'
        },
        '#wanStatus': {
            afterrender: 'getWanStatus'
        }
    },

    getSettings: function () {
        var vm = this.getViewModel();
        this.getView().appManager.getSettings(function (result, ex) {
            if (ex) { Util.exceptionToast(ex); return; }
            console.log(result);
            vm.set('settings', result);
            // me.getWanStatus();
        });
    },

    getWanStatus: function (cmp) {
        var vm = this.getViewModel(),
            grid = (cmp.getXType() === 'gridpanel') ? cmp : cmp.up('grid');
        grid.setLoading(true);
        this.getView().appManager.getWanStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('wans', result.list);

            var wanWarnings = [],
                tests = vm.get('settings.tests.list');

            Ext.Array.each(result.list, function (wan) {
                if (tests.length === 0 || Ext.Array.findBy(tests, function (test) {
                    return test.enabled && (wan.interfaceId === test.interfaceId);
                })) {
                    wanWarnings.push('<li>'  + Ext.String.format('Warning: The <i>{0}</i> needs a test configured!'.t(), wan.interfaceName) + '</li>');
                }
            });
            vm.set('wanWarnings', wanWarnings.join('<br/>'));
        });
    }
});
