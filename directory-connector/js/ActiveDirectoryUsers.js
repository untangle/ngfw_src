Ext.define('Ung.apps.directoryconnector.view.ActiveDirectoryUsers', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-directory-connector-activedirectoryusers',
    width: 900,
    height: 600,

    layout: 'fit',
    scrollable: true,

    onEsc: Ext.emptyFn,
    closable: false,
    modal: true,

    tbar: [{
        xtype: 'ungridfilter'
    }],

    items: [{
        xtype: 'ungrid',
        name: 'mapGrid',
        hasEdit: false,
        hasDelete: false,
        hasAdd: false,
        sortField: 'uid',
        // reserveScrollbar: true,
        store: [],
        plugins:['gridfilters'],
        columnMenuDisabled: false,
        fields:[{
            name: 'uid'
        },{
            name:'domain'
        }],
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true,
            // Workaround for text selection in Ext 6.2
            getRowClass: function () {
                return this.enableTextSelection ? 'x-selectable' : '';
            }
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex:'uid',
            sortable: true,
            width: Renderer.usernameWidth,
            flex: 1,
            filter: {
                type: 'string'
            }
        },{
            header: 'Groups'.t(),
            dataIndex:'groups',
            sortable: true,
            width: Renderer.usernameWidth,
            flex: 1,
            filter: {
                type: 'string'
            }
        },{
            header: 'Domain'.t(),
            dataIndex: 'domain',
            sortable: true,
            width: Renderer.hostnameWidth,
            flex: 1,
            filter: {
                type: 'string'
            }
        }]
    }],

    buttons: [{
        text: 'Close',
        iconCls: 'fa fa-close',
        handler: 'closeWindow'
    }]

});

