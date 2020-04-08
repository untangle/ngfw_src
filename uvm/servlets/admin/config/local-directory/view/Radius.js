Ext.define('Ung.config.local-directory.view.Radius', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius',
    itemId: 'radius',
    title: 'RADIUS',
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        padding: '10 20',
        width: 500,
        title: 'Wi-Fi Authentication (RADIUS server)'.t(),
        items: [{
            xtype: 'fieldcontainer',
            labelWidth: 120,
            anchor: '100%',
            padding: '5 0 10 0',
            fieldLabel: 'Server Certificate'.t(),
            layout: { type: 'hbox', align: 'middle' },
            items: [{
                xtype: 'combo',
                displayField: 'value',
                flex: 1,
                editable: false,
                queryMode: 'local',
                allowBlank: false,
                value: 'default',
                store: [
                    ['default', 'Default'.t()]
                ]
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Edit ...'
            }]
        }, {
            xtype: 'checkbox',
            reference: 'externalAccess',
            padding: '5 0',
            boxLabel: 'Enable external access point authentication'.t(),
            value: true
        }, {
            xtype: 'textfield',
            fieldLabel: 'RADIUS password'.t(),
            labelWidth: 120,
            width: '100%',
            inputType: 'password',
            allowBlank: false,
            bind: {
                disabled: '{!externalAccess.checked}'
            }
        }]
    }]

});
