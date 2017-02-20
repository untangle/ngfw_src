Ext.define('Ung.node.VirusBlocker', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-virus-blocker',
    layout: 'fit',
    requires: [
        'Ung.view.grid.Grid',
        'Ung.model.GenericRule'
    ],

    defaults: {
        border: false
    },


    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: "Virus Blocker detects and blocks malware before it reaches users' desktops or mailboxes.".t()
            }
        }
    }, {
        title: 'Pass Sites'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Pass Sites'.t() + '</h3><p>' + 'Do not scan traffic to the specified sites. Use caution!'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['delete', 'edit'],
            inlineEdit: 'cell', // 'cell' or 'row'
            dataProperty: 'passSites',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.passSites.list}',
                        sorters: ['string'],
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        }
                    }
                }
            },

            columns: [{
                text: 'Site'.t(),
                dataIndex: 'string',
                width: 200,
                hideable: false,
                menuDisabled: true,
                editorField: 'textfield',
                editorValidator: Util.urlValidator,
                editor: {
                    allowBlank: false,
                    emptyText: 'Site URL'.t(),
                    validator: Util.urlValidator
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Pass'.t(),
                dataIndex: 'enabled',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox',
                editor: {
                    xtype: 'checkbox'
                }
            }, {
                text: 'Description'.t(),
                dataIndex: 'description',
                flex: 1,
                menuDisabled: true,
                editorField: 'textarea',
                editor: {
                    emptyText: 'Site description'.t()
                }
            }]
        }]
    }, {
        title: 'Reports'.t(),
        xtype: 'ung.reports',
        border: false,
        initCategory: {
            categoryName: 'Virus Blocker',
            appName: 'untangle-node-virus-blocker',
            displayName: 'Virus Blocker'.t(),
            icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/untangle-node-virus-blocker_80x80.png'
        }
    }]
});
