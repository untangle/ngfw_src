Ext.syncRequire('Ung.common.threatprevention');
Ext.define('Ung.apps.threatprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-threat-prevention',

    viewModel: {
        type: 'app-threat-prevention',
        data: {
            title: 'Threat Prevention'.t(),
            iconName: 'threat-prevention',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/threat-prevention', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'threat-prevention',
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
