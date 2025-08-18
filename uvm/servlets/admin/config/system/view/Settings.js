Ext.define('Ung.config.system.view.settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-settings',
    itemId: 'settings',
    scrollable: true,
    withValidation: false,
    title: 'Settings'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/system/settings', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
