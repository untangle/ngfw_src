Ext.define('Ung.cmp.RulesModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.rulesmodel',

    formulas: {
        currentRecord: {
            bind: {
                bindTo: '{grid.selection}',
                deep: true
            },
            get: function (record) {
                return record;
            }
        }

    }
});