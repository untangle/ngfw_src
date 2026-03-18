Ext.define('Ung.apps.brandingmanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-branding-manager',
    controller: 'app-branding-manager',

    viewModel: {
        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/branding-manager', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
