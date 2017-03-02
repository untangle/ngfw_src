Ext.define('Ung.store.Reports', {
    extend: 'Ext.data.Store',
    storeId: 'reports',
    model: 'Ung.model.Report',
    sorters: [{
        property: 'displayOrder',
        direction: 'ASC'
    }]
});
