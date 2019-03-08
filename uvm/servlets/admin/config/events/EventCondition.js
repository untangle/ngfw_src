Ext.define ('Ung.model.EventCondition', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'javaClass', type: 'string'},
        { name: 'field', type: 'string' },
        { name: 'comparator', type: 'string'},
        { name: 'fieldValue', type: 'string'}
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
