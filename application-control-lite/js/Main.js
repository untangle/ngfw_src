Ext.define('Ung.apps.applicationcontrollite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control-lite',
    controller: 'app-application-control-lite',

    viewModel: {
        data: {
            title: 'Application Control Lite'.t(),
            iconName: 'application-control-lite',
            vueMigrated: true
        },
        // stores: {
        //     signatureList: {
        //         data: '{settings.patterns.list}'
        //     }
        // }
    },

    // items: [
    //     { xtype: 'app-application-control-lite-status' },
    //     { xtype: 'app-application-control-lite-signatures' }
    // ]

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/application-control-lite', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'application-control-lite',
                enableRemoveHandler: true
            });
        },

        destroy: function (panel) {
            Util.cleanupVueMessageHandlers(panel);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
