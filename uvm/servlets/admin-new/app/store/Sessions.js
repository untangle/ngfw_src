Ext.define('Ung.store.Sessions', {
    extend: 'Ext.data.Store',
    storeId: 'sessions',
    model: 'Ung.model.Session',
    sorters: ['bypassed']
});
