Ext.define('Ung.apps.webmonitor.view.Categories', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-monitor-categories',
    itemId: 'categories',
    title: 'Categories'.t(),

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'Flag access to sites associated with the specified category.'.t()
    }],

    listProperty: 'settings.categories.list',

    bind: '{categories}',

    columns: [{
        header: 'Category'.t(),
        width: 200,
        dataIndex: 'name'
    }, {
        xtype: 'checkcolumn',
        width: 55,
        header: 'Flag'.t(),
        dataIndex: 'flagged',
        resizable: false,
        tooltip: 'Flag as Violation'.t()
        // checkAll: {}
    }, {
        header: 'Description'.t(),
        flex: 1,
        width: 400,
        dataIndex: 'description',
        editor: {
            xtype: 'textfield',
            allowBlank: false
        }
    }]
});
