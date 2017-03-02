Ext.define('Ung.cmp.ConfigPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.configpanel',
    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back'.t(),
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'component',
            padding: '0 5',
            bind: { html: '<img src="/skins/modern-rack/images/admin/config/{iconName}.png" style="vertical-align: middle;" width="17" height="17"/> <strong>{title}</strong>' }
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        // border: false,
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            // scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }]
});
