Ext.define('Ung.cmp.GridFilter', {
    extend: 'Ext.form.field.Text',
    alias: 'widget.ungridfilter',

    controller: 'ungridfilter',

    reference: 'filterfield',
    fieldLabel: 'Filter'.t(),
    emptyText: 'Filter data ...'.t(),
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
        change: 'filterEventList',
        buffer: 100
    }

});