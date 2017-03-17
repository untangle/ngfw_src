Ext.define('Ung.store.UnavailableApps', {
    extend: 'Ext.data.Store',
    storeId: 'unavailableApps',
    alias: 'store.unavailableApps',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
