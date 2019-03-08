Ext.define('Ung.apps.intrusionprevention.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusion-prevention-advanced',
    itemId: 'advanced',
    scrollable: true,

    name: 'advanced',

    region: 'center',

    title: "Advanced".t(),

    bodyPadding: 10,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    defaults: {
        xtype: 'fieldset',
        padding: 10,
    },

    items: [{
        title: 'Content Processing'.t(),
        defaults: {
            margin: '5 0'
        },
        items: [{
            xtype: 'numberfield',
            fieldLabel: 'Maximum scan depth (bytes)'.t(),
            labelWidth: 200,
            width: 300,
            allowBlank: false,
            minValue: 0,
            bind: '{settings.iptablesMaxScanSize}'
        }]
    },{
        title: 'Signature Options'.t(),
        defaults: {
            margin: '5 0'
        },
        items: [{
            xtype: 'combo',
            fieldLabel: 'Block action'.t(),
            bind: '{settings.blockAction}',
            queryMode: 'local',
            editable: false,
            displayField: 'description',
            valueField: 'value',
            store: [
                ['reject', 'reject (default)'.t()],
                ['drop', 'drop'.t()]
            ]
        }]
    }]
});
