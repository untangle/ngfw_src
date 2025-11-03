Ext.define('Ung.apps.dynamic-blocklists.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-dynamic-blocklists',
    layout: 'border',
    
    viewModel: {
        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/dynamic-blocklist', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});