Ext.define('Ung.store.Site', {
    extend: 'Ext.data.Store',
    storeId: 'site',
    model: 'Ung.model.Policy',
    //fields: ['policyId'],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'list'
        }
    }
});
