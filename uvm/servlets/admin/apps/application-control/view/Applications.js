Ext.define('Ung.apps.applicationcontrol.view.Applications', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-application-control-applications',
    itemId: 'applications',
    title: 'Applications'.t(),

    listProperty: 'settings.protoRules.list',
    bind: '{protoRules}',

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
        // listeners: {
        //     checkchange: Ext.bind(function(elem, rowIndex, checked) {
        //         if(checked) {
        //             var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
        //             record.set('tarpit', false);
        //         }
        //     }, this)
        // }
    }, {
        xtype: 'checkcolumn',
        header: 'Tarpit'.t(),
        dataIndex: 'tarpit',
        width: 50,
        resizable: false,
        // listeners: {
        //     checkchange: Ext.bind(function(elem, rowIndex, checked) {
        //         if(checked) {
        //             var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
        //             record.set('block', false);
        //         }
        //     }, this)
        // }
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
