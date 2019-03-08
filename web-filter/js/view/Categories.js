Ext.define('Ung.apps.webfilter.view.Categories', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-filter-categories',
    itemId: 'categories',
    title: 'Categories'.t(),

    tbar: [{
        xtype: 'ungridfilter'
    },{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'Block or flag access to sites associated with the specified category.'.t()
    }],

    listProperty: 'settings.categories.list',

    bind: '{categories}',

    columns: [{
        header: 'Category'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'name'
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Block'.t(),
        dataIndex: 'blocked',
        resizable: false
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Flag'.t(),
        dataIndex: 'flagged',
        resizable: false,
        //tooltip: 'Flag as Violation'.t()
        // checkAll: {}
    }, {
        header: 'Description'.t(),
        flex: 4,
        width: Renderer.messageWidth,
        dataIndex: 'description',
        editor: {
            xtype: 'textfield',
            allowBlank: false
        }
    }]
});
