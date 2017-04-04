Ext.define('Ung.view.main.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.main',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    init: function (view) {
        var vm = view.getViewModel();
        //view.getViewModel().set('widgets', Ext.getStore('widgets'));
        vm.set('reports', Ext.getStore('reports'));
        vm.set('policyId', 1);
    },

    onBeforeRender: function(view) {
        var vm = view.getViewModel();

        // vm.bind('{reportsEnabled}', function(enabled) {
        //     if (enabled) {
        //         view.down('#main').insert(3, {
        //             xtype: 'ung.reports',
        //             itemId: 'reports'
        //         });
        //     } else {
        //         view.down('#main').remove('reports');
        //     }
        // });

        vm.set('reportsInstalled', rpc.appManager.app('reports') !== null);
        if (rpc.appManager.app('reports')) {
            vm.set('reportsRunning', rpc.appManager.app('reports').getRunState() === 'RUNNING');
        }
        vm.notify();
    }
});
