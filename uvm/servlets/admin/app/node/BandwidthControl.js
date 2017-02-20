Ext.define('Ung.node.BandwidthControl', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-bandwidth-control',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t()
            }
        }

    }, {
        title: 'Rules'.t(),
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            cls: 'grid-description',
            html: '<p>' + 'Rules are evaluated in-order on network traffic.'.t() + '</p>'
        }, {
            xtype: 'ung.grid',
            flex: 1,

            toolbarFeatures: ['add', 'importexport'],
            columnFeatures: ['edit', 'delete', 'reorder'],

            dataProperty: 'rules',

            viewModel: {
                stores: {
                    store: {
                        model: 'Ung.model.Rule',
                        data: '{settings.rules.list}',
                        listeners: {
                            update: 'checkChanges',
                            datachanged: 'checkChanges'
                        }
                    }
                }
            },

            columns: [{
                xtype: 'checkcolumn',
                text: 'Enabled'.t(),
                dataIndex: 'enabled',
                width: 80,
                hideable: false,
                resizable: false,
                menuDisabled: true,
                editorField: 'checkbox',
                editor: {
                    xtype: 'checkbox'
                }
            }, {
                text: 'Rule ID'.t(),
                dataIndex: 'ruleId',
                width: 80,
                resizable: false,
                hideable: false,
                menuDisabled: true
            }, {
                text: 'Description'.t(),
                dataIndex: 'description',
                width: 200,
                editorField: 'textfield',
                editor: {
                    emptyText: 'Site description'.t()
                }
            }, {
                text: 'Conditions'.t(),
                dataIndex: 'conditions',
                flex: 1,
                editorField: 'conditions',
                renderer: function (value) {
                    var conditions = '';
                    value.list.forEach(function (cond) {
                        conditions += cond.conditionType + ' <strong>' + (cond.invert ? '&ne;' : '=') + '</strong> ' + cond.value + '<br/>';
                    });
                    return conditions;
                }
            }, {
                text: 'Action'.t(),
                dataIndex: 'action',
                width: 200,

                renderer: function (value) {
                    if (typeof value === 'undefined') {
                        return 'Unknown action'.t();
                    }
                    var priority = '';
                    switch (value.priority) {
                    case 0: priority = ''; break;
                    case 1: priority = 'Very High'.t(); break;
                    case 2: priority = 'High'.t(); break;
                    case 3: priority = 'Medium'.t(); break;
                    case 4: priority = 'Low'.t(); break;
                    case 5: priority = 'Limited'.t(); break;
                    case 6: priority = 'Limited More'.t(); break;
                    case 7: priority = 'Limited Severely'.t(); break;
                    default: priority = Ext.String.format('Unknown Priority: {0}'.t(), value);
                    }

                    switch (value.actionType) {
                    case 'SET_PRIORITY': return 'Set Priority'.t() + ' [' + priority + ']';
                    case 'PENALTY_BOX_CLIENT_HOST': return 'Send Client to Penalty Box'.t();
                    case 'APPLY_PENALTY_PRIORITY': return 'Apply Penalty Priority'.t(); // DEPRECATED
                    case 'GIVE_CLIENT_HOST_QUOTA': return 'Give Client a Quota'.t();
                    default: return 'Unknown Action: '.t() + value;
                    }
                },
                editorField: 'action'
            }]
        }]
    }]
});
