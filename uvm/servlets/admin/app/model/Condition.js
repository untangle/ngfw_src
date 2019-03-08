Ext.define ('Ung.model.Condition', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'javaClass', type: 'string'},
        { name: 'conditionType', type: 'string' },
        { name: 'invert', type: 'boolean', defaultValue: false },
        { name: 'value', type: 'auto', defaultValue: '' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
