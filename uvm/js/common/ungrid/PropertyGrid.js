/**
 * Component showing the details of an event record
 *
 * Changed property grid to normal grid/store
 * PropertyGrid uses `source` and `sourceConfig` as props to set and populate data
 * `source` can be only a { key: value } object
 * - advantage: can be specified a renderer for each value from a row
 * - disadvantage: inability to have more data than the { key: value } (e.g. category)
 * Normal store:
 * - advantage: we can define multiple columns and extra properties allowing data grouping
 * - disadvantage: the renderer is specified on entire column
 *   (which cannot be applied in this case as each row represents a different column/value in master grid)
 */
Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.unpropertygrid',

    controller: 'unpropertygrid',

    viewModel: true,

    editable: false,
    width: Renderer.calculateWith(4),
    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    // cls: 'prop-grid',

    disableSelection: true,

    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Selection!</p>',

    store: {
        fields: ['name', 'value', 'category'],
        groupField: 'category',
        sorters: 'name',
        data: []
    },

    columns: [{
        text: 'Name',
        flex: 1,
        dataIndex: 'name',
        sortable: false,
        hideable: false,
        menuDisabled: true
    }, {
        text: 'Value',
        flex: 1,
        dataIndex: 'value',
        sortable: false,
        hideable: false,
        menuDisabled: true
    }, {
        text: 'Category',
        dataIndex: 'category',
        hidden: true
    }],

    viewConfig: {
        enableTextSelection: true,
        getRowClass: function(record) {
            var cls = 'x-selectable'; // NGFW-11399 force selectable text
            if (record.get('value') === null || record.get('value') === '') {
                cls += ' empty';
            }
            return cls;
        }
    }
    // initComponent: function () {
    //     // var me = this;
    //     // if(me.emptyText){
    //     //     me.emptyText = '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + this.emptyText + '</p>';
    //     // }
    //     // this.callParent(arguments);
    // }
});
