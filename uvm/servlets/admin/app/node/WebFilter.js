Ext.define('Ung.node.WebFilter', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-web-filter',
    layout: 'fit',
    /* requires-start */
    requires: [
        'Ung.view.grid.Grid',
        'Ung.model.GenericRule'
    ],
    /* requires-end */

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
        title: 'Categories'.t(),
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
                editorValidator: Util.urlValidator,
                editor: {
                    allowBlank: false,
                    emptyText: 'Site URL'.t(),
                    validator: Util.urlValidator
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
        title: 'Rules'.t(),
        layout: {
            type: 'hbox',
            align: 'stretch'
        },
        viewModel: {
            formulas: {
                currentRule: {
                    bind: '{rules.selection}',
                    get: function (rule) {
                        this.set('current.rule', rule);
                        return rule;
                    }
                }
            }
        },
        items: [{
            xtype: 'grid',
            reference: 'rules',
            flex: 1,
            bind: {
                store: {
                    // model: 'Ung.model.GenericRule',
                    data: '{settings.filterRules.list}'
                }
            },
            columns: [{
                xtype: 'checkcolumn',
                text: 'Enabled'.t(),
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
                text: 'Rule ID'.t(),
                dataIndex: 'ruleId',
                menuDisabled: true
            }, {
                xtype: 'checkcolumn',
                text: 'Flagged'.t(),
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
                xtype: 'checkcolumn',
                text: 'Blocked'.t(),
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
                text: 'Description'.t(),
                dataIndex: 'description',
                flex: 1,
                menuDisabled: true,
                editorField: 'textarea',
                editor: {
                    emptyText: 'Site description'.t()
                }
            }]
        }, {
            xtype: 'form',
            title: 'Customer',
            width: 400,
            items: [{
                bind: {
                    html: '{current.rule.description}'
                }
            }, {
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                name: 'description',
                bind: {
                    value: '{current.rule.description}',
                    disabled: '{!current.rule}'
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
            icon: '/skins/modern-rack/images/admin/apps/untangle-node-web-filter_80x80.png'
        }
    }]
});
