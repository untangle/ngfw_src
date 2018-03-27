Ext.define('Ung.apps.reports.view.AllReports', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.app-reports-allreports',
    itemId: 'all-reports',
    title: 'All Reports'.t(),
    scrollable: true,

    store: 'reports',

    features: [{
        ftype: 'grouping',
        // groupHeaderTpl: 'Category: {category}'
        // startCollapsed: true
    }],

    columns: [{
        header: 'Title'.t(),
        width: 300,
        dataIndex: 'title',
        renderer: function (value, meta, record) {
            return '<i class="fa ' + record.get('icon') + ' fa-gray"></i> ' + record.get('localizedTitle');
        }
    }, {
        header: 'Type'.t(),
        width: 150,
        dataIndex: 'type',
        renderer: 'reportTypeRenderer'
    }, {
        header: 'Description'.t(),
        flex: 1,
        dataIndex: 'description',
        renderer: function (value, meta, record) {
            return record.get('localizedDescription');
        }
    }, {
        header: 'Units'.t(),
        width: 90,
        dataIndex: 'units'
    }, {
        header: 'Display Order'.t(),
        width: 90,
        dataIndex: 'displayOrder'
    }, {
        header: 'View'.t(),
        menuText: 'View'.t(),
        xtype: 'actioncolumn',
        width: 70,
        align: 'center',
        iconCls: 'fa fa-external-link-square',
        isDisabled: 'isDisabledCategory',
        handler: function (view, rowIndex, colIndex, item, e, record) {
            Ung.app.redirectTo('#reports?' + record.get('url'));
        }
    }]

});
