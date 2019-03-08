Ext.define('Ung.store.Hosts', {
    extend: 'Ext.data.Store',
    storeId: 'hosts',
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }

});
