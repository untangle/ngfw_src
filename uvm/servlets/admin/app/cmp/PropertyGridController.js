Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    columnRenderer: function(value, metaData, record, rowIndex, columnIndex, store, view){
        var rtype = view.grid.sourceConfig[record.id].rtype;
        if(rtype != null){
            return Renderer[rtype](value);
        }
    }
});
