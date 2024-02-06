Ext.define('Ung.store.Devices', {
    extend: 'Ext.data.Store',
    storeId: 'devices',

    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }

});
