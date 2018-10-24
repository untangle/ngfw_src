Ext.define('Ung.cmp.GridFilter', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.ungridfilter',

    controller: 'ungridfilter',

    layout: 'hbox',
    border: 0,

    items: [{
        xtype: 'textfield',
        reference: 'filterfield',
        fieldLabel: 'Filter'.t(),
        bind: {
            // This does not work but we're leaving it here in the hope that it will.
            style: '{filterStyle}'
        },
        emptyText: 'Text to match ...'.t(),
        labelWidth: 'auto',
        enableKeyEvents: true,
        triggers: {
            clear: {
                cls: 'x-form-clear-trigger',
                hidden: true,
                handler: function (field) {
                    field.setValue('');
                }
            }
        },
        listeners: {
            change: 'changeFilter',
            buffer: 100
        }
    },{
        xtype: 'tbtext',
        bind: {
            style: '{filterStyle}',
            html: '{filterSummary}'
        },
        margin: 4,
    }]
});