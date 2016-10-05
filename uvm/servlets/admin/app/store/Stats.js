Ext.define('Ung.store.Stats', {
    extend: 'Ext.data.Store',
    storeId: 'stats',
    data: [{}],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});