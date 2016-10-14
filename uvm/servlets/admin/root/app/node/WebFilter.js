Ext.define('Ung.node.WebFilter', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-web-filter',
    layout: 'fit',
    requires: [
        'Ung.view.grid.Grid',
        'Ung.view.grid.ActionColumn',
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
                summary: 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t()
            }
        }
    }, {
        title: 'Block Categories'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Block Categories'.t() + '</h3><p>' + 'Block or flag access to sites associated with the specified category.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            columnFeatures: ['edit'],
            inlineEdit: 'cell', // 'cell' or 'row'
            dataProperty: 'categories',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.categories.list}',
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        },
                        validations: [
                            { type: 'format', field: 'string', matcher: /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g }
                        ]
                    }
                }
            },

            columns: [{
                text: 'Category'.t(),
                dataIndex: 'name',
                width: 200,
                hideable: false,
                editDisabled: true,
                editorField: 'textfield'
            }, {
                xtype: 'checkcolumn',
                text: 'Block'.t(),
                dataIndex: 'blocked',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox',
                editor: {
                    xtype: 'checkbox'
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Flag'.t(),
                dataIndex: 'flagged',
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
                editorField: 'textarea',
                editor: {
                    emptyText: 'Site description'.t()
                }
            }]
        }]
    }, {
        title: 'Block Sites'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Blocked Sites'.t() + '</h3><p>' + 'Block or flag access to the specified site.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'delete', 'revert', 'importexport'],
            columnFeatures: ['delete', 'edit', 'reorder', 'select'],
            inlineEdit: 'cell', // 'cell' or 'row'
            dataProperty: 'blockedUrls',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.blockedUrls.list}',
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        },
                        validations: [
                            { type: 'format', field: 'string', matcher: /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g }
                        ]
                    }
                }
            },

            columns: [{
                text: 'Site'.t(),
                dataIndex: 'string',
                width: 200,
                hideable: false,
                editorField: 'textfield',
                editorValidator: Ung.Util.urlValidator,
                editor: {
                    allowBlank: false,
                    emptyText: 'Site URL'.t(),
                    validator: Ung.Util.urlValidator
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Block'.t(),
                dataIndex: 'blocked',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox',
                editor: {
                    xtype: 'checkbox'
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Flag'.t(),
                dataIndex: 'flagged',
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
                editorField: 'textarea',
                editor: {
                    emptyText: 'Site description'.t()
                }
            }]
        }]
    }, {
        title: 'Block File Types'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Block File Types'.t() + '</h3><p>' + 'Block or flag access to files associated with the specified file type.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['edit', 'delete'],
            inlineEdit: 'cell',
            dataProperty: 'blockedExtensions',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.blockedExtensions.list}',
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        }
                    }
                }
            },

            columns: [{
                text: 'File Type'.t(),
                dataIndex: 'string',
                width: 80,
                hideable: false,
                editorField: 'textfield',
                editor: {
                    allowBlank: false,
                    emptyText: 'File Type'.t()
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Block'.t(),
                dataIndex: 'blocked',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox'
            }, {
                xtype: 'checkcolumn',
                text: 'Flag'.t(),
                dataIndex: 'flagged',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox'
            }, {
                text: 'Category'.t(),
                dataIndex: 'category',
                width: 200,
                editorField: 'textfield',
                editor: true
            }, {
                text: 'Description'.t(),
                dataIndex: 'description',
                flex: 1,
                editorField: 'textarea',
                editor: true
            }]
        }]
    }, {
        title: 'Block Mime Types'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Block MIME Types'.t() + '</h3><p>' + 'Block or flag access to files associated with the specified MIME type.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['edit', 'delete'],
            inlineEdit: 'cell',
            dataProperty: 'blockedMimeTypes',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.blockedMimeTypes.list}',
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        }
                    }
                }
            },

            columns: [{
                text: 'MIME Type'.t(),
                dataIndex: 'string',
                width: 200,
                hideable: false,
                editorField: 'textfield',
                editor: {
                    allowBlank: false,
                    emptyText: 'File Type'.t()
                }
            }, {
                xtype: 'checkcolumn',
                text: 'Block'.t(),
                dataIndex: 'blocked',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox'
            }, {
                xtype: 'checkcolumn',
                text: 'Flag'.t(),
                dataIndex: 'flagged',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true,
                editorField: 'checkbox'
            }, {
                text: 'Category'.t(),
                dataIndex: 'category',
                width: 200,
                editorField: 'textfield',
                editor: true
            }, {
                text: 'Description'.t(),
                dataIndex: 'description',
                flex: 1,
                editorField: 'textarea',
                editor: true
            }]
        }]
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
            html: '<h3>' + 'Pass Sites'.t() + '</h3><p>' + 'Allow access to the specified site regardless of matching block policies.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['delete', 'edit'],
            inlineEdit: 'cell', // 'cell' or 'row'
            dataProperty: 'passedUrls',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.passedUrls.list}',
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
                editorValidator: Ung.Util.urlValidator,
                editor: {
                    allowBlank: false,
                    emptyText: 'Site URL'.t(),
                    validator: Ung.Util.urlValidator
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
        title: 'Pass Clients'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<h3>' + 'Pass Sites'.t() + '</h3><p>' + 'Allow access for client networks regardless of matching block policies.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['delete', 'edit'],
            inlineEdit: 'cell', // 'cell' or 'row'
            dataProperty: 'passedClients',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.GenericRule',
                        data: '{settings.passedClients.list}',
                        sorters: ['string'],
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        }
                    }
                }
            },

            columns: [{
                text: 'IP address/range'.t(),
                dataIndex: 'string',
                width: 200,
                hideable: false,
                menuDisabled: true,
                editorField: 'textfield',
                editor: {
                    allowBlank: false,
                    emptyText: 'Site URL'.t(),
                    vtype: 'ipall'
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
            categoryName: 'Web Filter',
            appName: 'untangle-node-web-filter',
            displayName: 'Web Filter'.t(),
            icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/untangle-node-web-filter_80x80.png'
        }
    }]
});