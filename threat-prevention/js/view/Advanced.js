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
        padding: 10,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Enable',
            bind: '{settings.customBlockPageEnabled}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Custom block page URL'.t(),
            labelAlign: 'top',
            emptyText: 'http://example.com',
            disabled: true,
            bind: {
                value: '{settings.customBlockPageUrl}',
                disabled: '{!settings.customBlockPageEnabled}'
            }
        }]
    }]
});
