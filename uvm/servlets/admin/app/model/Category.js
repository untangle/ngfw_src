Ext.define ('Ung.model.Category', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'name', type: 'string' },
        { name: 'displayName', type: 'string' },
        { name: 'icon', type: 'string' }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
