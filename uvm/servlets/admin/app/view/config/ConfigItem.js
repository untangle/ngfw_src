Ext.define('Ung.view.config.ConfigItem', {
    extend: 'Ext.Component',
    xtype: 'ung.configitem',
    cls: 'appitem',

    viewModel: true,

    bind: {
        html: '<div class="node-image">' +
              '<img src="/skins/modern-rack/images/admin/config/icon_config_{iconName}.png" width=80 height=80/></div>' +
              '<div class="node-label">{displayName}</div>'
    },

    listeners: {
        afterrender: 'onItemBeforeRender'
    }
});
