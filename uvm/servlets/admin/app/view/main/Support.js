Ext.define('Ung.view.main.Support', {
    extend: 'Ext.window.Window',
    alias: 'widget.support',

    title: 'Contact Untangle Support'.t(),

    name: 'support',
    helpSource: 'support',

    controller: 'support',

    modal: true,
    closable: false,
    width: 800,
    height: 210,
    shadow: false,
    resizable: false,
    draggable: false,
    collapsible: false,

    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'panel',
        itemId: 'supportEnabled',
        border: false,
        layout: 'anchor',
        buttonAlign: 'right',
        buttons:[{
            xtype: 'button',
            text: '<strong>' + 'Cancel'.t() + '</strong>',
            iconCls: 'fa fa-ban',
            handler: 'cancelHandler'
        },{
            xtype: 'button',
            text: '<strong>' + 'No - Do Not Enable Support Access'.t() + '</strong>',
            iconCls: 'fa fa-check',
            handler: 'noSupportHandler'
        }, {
            xtype: 'button',
            text: '<strong>' + 'Yes - Enable Support Access'.t() + '</strong>',
            iconCls: 'fa fa-save',
            handler: 'yesSupportHandler'
        }],
        items: [{
            xtype: 'component',
            cls: 'welcome',
            padding: 10,
            html: '<h3>' + 'Remote Support Access is not enabled'.t() + '</h3>' +
                '<p>' + 'Enabling remote support access is recommended for better and fast support.'.t() + '</p>' +
                '<p>' + 'Would you like enable the support team to access your server remotely?'.t() + '</p>'
        }]
    }]
});
