Ext.define('Ung.store.Metrics', {
    extend: 'Ext.data.Store',
    storeId: 'metrics',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'users'
        }
    }
});
