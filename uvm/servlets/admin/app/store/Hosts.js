Ext.define('Ung.store.Hosts', {
    extend: 'Ext.data.Store',
    storeId: 'hosts',
    // model: 'Ung.model.Session'
    fields: [
        { name: 'address', type: 'string' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
            // rootProperty: 'list'
        }
    }

});
