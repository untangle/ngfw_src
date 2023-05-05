Ext.define('Ung.apps.webmonitor.view.Categories', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-monitor-categories',
    itemId: 'categories',
    title: 'Categories'.t(),
    withValidation: false,
    tbar: [{
        xtype: 'ungridfilter'
    },{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'Flag access to sites associated with the specified category.'.t()
    }],

    features: [{
        ftype: 'grouping',
        groupHeaderTpl: ['Group: {name:this.formatName} ({rows.length} {[values.rows.length > 1 ? "categories" : "category"]})'.t(),{
            formatName: function(name){
                return Ext.String.trim(name);
            }
        }],
        startCollapsed: false
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
        header: 'Flag'.t(),
        dataIndex: 'flagged',
        resizable: false,
        tooltip: 'Flag as Violation'.t()
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
