Ext.define('Ung.store.Policies', {
    extend: 'Ext.data.Store',
    storeId: 'policies',
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