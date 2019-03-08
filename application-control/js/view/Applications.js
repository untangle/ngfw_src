Ext.define('Ung.apps.applicationcontrol.view.Applications', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-application-control-applications',
    itemId: 'applications',
    title: 'Applications'.t(),

    listProperty: 'settings.protoRules.list',
    bind: '{protoRules}',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Application Control Applications allows for simple control over all recognized traffic types.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'ungridfilter'
        },
        '->', '@replace', '@export'
        ]
    }],

    columns: [{
        header: 'Application'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'guid'
    }, {
        xtype: 'checkcolumn',
        header: 'Block'.t(),
        dataIndex: 'block',
        width: Renderer.booleanWidth,
        resizable: false,
        listeners: {
            checkchange: function(col, row, checked, record) {
                if (checked) record.set('tarpit', false);
            }
        }
    }, {
        xtype: 'checkcolumn',
        header: 'Tarpit'.t(),
        dataIndex: 'tarpit',
        width: Renderer.booleanWidth,
        resizable: false,
        listeners: {
            checkchange: function(col, row, checked, record) {
                if (checked) record.set('block', false);
            }
        }
    }, {
        xtype: 'checkcolumn',
        header: 'Flag'.t(),
        dataIndex: 'flag',
        width: Renderer.booleanWidth,
        resizable: false
    }, {
        header: 'Name'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'name'
    }, {
        header: 'Category'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'category'
    }, {
        header: 'Productivity'.t(),
        width: Renderer.idWidth,
        dataIndex: 'productivity'
    }, {
        header: 'Risk',
        width: Renderer.idWidth,
        dataIndex: 'risk'
    }, {
        header: 'Description (click for full text)'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'description',
        flex: 1
    }]
});
