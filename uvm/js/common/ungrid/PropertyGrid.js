Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.unpropertygrid',

    controller: 'unpropertygrid',

    editable: false,
    width: 400,
    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    cls: 'prop-grid',

    viewConfig: {
        getRowClass: function(record) {
            if (record.get('value') === null || record.get('value') === '') {
                return 'empty';
            }
            return;
        }
    },

    nameColumnWidth: 200,

    // features: [{
    //     ftype: 'grouping',
    //     groupHeaderTpl: '{name}'
    // }],

    listeners: {
        beforeedit: function () { return false; },
        beforeexpand: 'onBeforeExpand',
        beforerender: 'onBeforeRender'
    }
});
