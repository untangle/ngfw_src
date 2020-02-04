/**
 * Compnent replacing PropertyGrid
 * PropertyGrid is used exclusively for reports events
 *
 * For other types of grid this shoud be used
 */
Ext.define('Ung.cmp.RecordDetails', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.recorddetails',

    controller: 'recorddetails',

    viewModel: true,

    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    // cls: 'prop-grid',

    nameColumnWidth: 200,

    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Selection!</p>',

    viewConfig: {
        enableTextSelection: true,
        getRowClass: function(record) {
            var cls = 'x-selectable'; // NGFW-11399 force selectable text
            if (record.get('value') === null || record.get('value') === '') {
                cls += ' empty';
            }
            return cls;
        }
    },

    listeners: {
        // avoid editing props
        beforeedit: function() { return false; },
        beforerender: 'onBeforeRender'
    }
});
