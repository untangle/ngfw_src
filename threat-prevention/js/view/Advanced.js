Ext.define('Ung.apps.threatprevention.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    scrollable: true,
    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Custom block page'.t(),
        checkboxToggle: true,
        checkbox: {
            bind: '{settings.customBlockPageEnabled}'
        },
        collapsible: true,
        collapsed: true,
        padding: 10,
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Custom block page URL'.t(),
            labelAlign: 'top',
            emptyText: 'http://example.com',
            bind: '{settings.customBlockPageUrl}'
        }]
    },{
        xtype: 'fieldset',
        title: 'Block options'.t(),
        padding: '10 15',
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Close connection for blocked HTTPS sessions without redirecting to block page'.t(),
            bind: '{settings.closeHttpsBlockEnabled}'
        }]
    }]
});
