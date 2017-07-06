Ext.define('Ung.store.Reports', {
    extend: 'Ext.data.Store',
    alias: 'store.reports',
    storeId: 'reports',
    model: 'Ung.model.Report',
    groupField: 'category',
    sorters: [{
        property: 'displayOrder',
        direction: 'ASC'
    }]
});
