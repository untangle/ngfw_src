Ext.define('Ung.apps.reports.view.NameMap', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-reports-namemap',
    itemId: 'name-map',
    title: 'Name Map'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],

    listProperty: 'settings.hostnameMap.list',
    emptyRow: {
        address: '1.2.3.4',
        hostname: '',
        javaClass: 'com.untangle.app.reports.ReportsHostnameMapEntry',
    },

    emptyText: 'No Names Defined'.t(),

    bind: '{hostnames}',

    columns: [{
        header: 'IP Address'.t(),
        width: Renderer.ipWidth,
        dataIndex: 'address',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter IP address]'.t(),
            allowBlank: false,
            vtype: 'ipAddress'
        }
    }, {
        header: 'Name'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'hostname',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter name]'.t(),
            regex: /^[^'"]+$/,
            regexText: 'Quotes and double quotes are not allowed'.t(),
            allowBlank: false
        }
    }],
    editorFields: [{
        xtype: 'textfield',
        bind: '{record.address}',
        fieldLabel: 'IP Address'.t(),
        emptyText: '[enter IP address]'.t(),
        allowBlank: false,
        width: 300,
        vtype: 'ipAddress',
        validator: function (val) {
            if (this.up('grid').getStore().findExact('address', val) >= 0) {
                return 'IP address is already defined in Name Map'.t();
            } else {
                return true;
            }
        }
    }, {
        xtype:'textfield',
        bind: '{record.hostname}',
        fieldLabel: 'Name'.t(),
        emptyText: '[enter name]'.t(),
        regex: /^[^'"]+$/,
        regexText: 'Quotes and double quotes are not allowed'.t(),
        allowBlank: false,
        width: 300
    }]

});
