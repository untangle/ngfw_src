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
        items: ['->', '@replace', '@export']
    }],

    columns: [{
        header: 'Application'.t(),
        width: 120,
        dataIndex: 'guid'
    }, {
        xtype: 'checkcolumn',
        header: 'Block'.t(),
        dataIndex: 'block',
        width: 50,
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
        width: 50,
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
        width: 50,
        resizable: false
    }, {
        header: 'Name'.t(),
        width: 150,
        dataIndex: 'name'
    }, {
        header: 'Category'.t(),
        width: 150,
        dataIndex: 'category'
    }, {
        header: 'Productivity'.t(),
        width: 80,
        dataIndex: 'productivity'
    }, {
        header: 'Risk',
        width: 80,
        dataIndex: 'risk'
    }, {
        header: 'Description (click for full text)'.t(),
        width: 300,
        dataIndex: 'description',
        flex: 1
    }]
});
