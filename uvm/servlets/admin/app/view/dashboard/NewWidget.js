Ext.define('Ung.view.dashboard.NewWidget', {
    extend: 'Ext.window.Window',
    alias: 'widget.new-widget',

    title: 'Add/Edit Widgets',
    controller: 'new-widget',

    modal: true,
    width: 800,
    height: 500,
    layout: 'border',

    viewModel: {
        data: {
            widget: null,
            entry: null,
            onDashboard: false,
        },
        formulas: {
            _autorefresh: {
                get: function(get) {
                    var interval = get('widget.refreshIntervalSec');
                    return interval ? true : false;
                },
                set: function (value) {
                    this.set('widget.refreshIntervalSec', value ? 60 : '');
                }
            },
            _timeframe: {
                get: function(get) {
                    return get('widget.timeframe') ? get('widget.timeframe') / 3600 : '';
                },
                set: function (value) {
                    this.set('widget.timeframe', value ? value * 3600 : null);
                }
            },
            _timeframeck: {
                get: function(get) {
                    return get('_timeframe') === '';
                },
                set: function (value) {
                    this.set('_timeframe', value ? '' : 1);
                }
            },
            _columnsck: {
                get: function(get) {
                    return get('widget.displayColumns') ? false : true;
                },
                set: function (value) {
                    if (value) {
                        this.set('widget.displayColumns', null);
                    } else {
                        this.set('widget.displayColumns', this.get('entry.defaultColumns'));
                    }
                }
            },
            // _columns: {
            //     get: function (get) {
            //         console.log(get('entry.displayColumns'));
            //         return get('widget.displayColumns') || get('entry.displayColumns');
            //     },
            //     set: function (val) {
            //         console.log(val);
            //     }
            // }
        }
    },

    items: [{
        xtype: 'treepanel',
        reference: 'tree',
        // title: 'Reports'.t(),
        // collapsible: true,
        // titleCollapse: true,
        region: 'west',
        weight: 20,
        width: 250,
        minWidth: 250,
        split: true,
        // border: false,
        // singleExpand: true,
        useArrows: true,
        rootVisible: false,

        viewConfig: {
            selectionModel: {
                type: 'treemodel'
                // pruneRemoved: false
            }
        },
        store: 'reportstree',
        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'text',
            // scope: 'controller',
            renderer: 'treeNavNodeRenderer'
        }],

        tbar: [{
            xtype: 'textfield',
            emptyText: 'Filter reports ...',
            // enableKeyEvents: true,
            flex: 1,
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
                    // handler: 'onTreeFilterClear'
                }
            },
            listeners: {
                change: 'filterTree',
                buffer: 300
            }
        }],
        listeners: {
            select: 'selectNode'
        }
    }, {
        region: 'center',
        reference: 'report',
        layout: 'fit',
        items: [{
            xtype: 'container',
            layout: 'center',
            bind: {
                hidden: '{entry}'
            },
            items: [{
                xtype: 'component',
                width: '100%',
                style: {
                    textAlign: 'center',
                    color: '#777'
                },
                html: '<i class="fa fa-info-circle fa-lg"></i> <br/>' + 'Select a report first ...'
            }]
        }]
    }, {
        region: 'south',
        // title: 'Settings'.t(),
        height: 'auto',

        // split: true,
        // maxHeight: 140,
        // minHeight: 140,
        // layout: 'fit',

        dockedItems: [{
            xtype: 'component',
            dock: 'top',
            ui: 'footer',
            margin: '10 0',
            style: {
                textAlign: 'center',
                fontSize: '11px',
                color: '#333'
            },
            hidden: true,
            bind: {
                hidden: '{!(entry && onDashboard)}',
                html: '<i class="fa fa-info-circle fa-lg"></i> ' + 'This report widget exists in Dashboard and is <strong>{widget.enabled ? "enabled" : "disabled"}</strong>!',
            }
        }],

        items: [{
            // xtype: 'form',
            border: false,
            bodyPadding: 10,
            layout: {
                type: 'vbox'
                // align: 'stretch'
            },
            hidden: true,
            bind: {
                hidden: '{!entry}'
            },

            items: [{
                xtype: 'checkbox',
                boxLabel: '<strong>' + 'Enabled'.t() + '</strong>',
                bind: '{widget.enabled}'
            }, {
                xtype: 'container',
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'checkbox',
                    boxLabel: '<strong>' + 'Auto Refresh'.t() + '</strong>: ',
                    bind: '{_autorefresh}'
                }, {
                    xtype: 'numberfield',
                    width: 50,
                    maxValue: 600,
                    minValue: 10,
                    allowBlank: true,
                    margin: '0 5',
                    hidden: true,
                    bind: {
                        value: '{widget.refreshIntervalSec}',
                        hidden: '{!_autorefresh}'
                    }
                }, {
                    xtype: 'component',
                    style: {
                        fontSize: '11px',
                        color: '#777'
                    },
                    html: '(seconds)'.t(),
                    hidden: true,
                    bind: {
                         hidden: '{!_autorefresh}'
                    }
                }]
            }, {
                xtype: 'checkbox',
                reference: 'columnsck',
                fieldLabel: '<strong>' + 'Columns'.t() + '</strong>',
                labelWidth: 'auto',
                boxLabel: 'report defaults',
                hidden: true,
                // publishes: 'value',
                bind: {
                    value: '{_columnsck}',
                    hidden: '{entry.type !== "EVENT_LIST"}'
                }
            }, {
                xtype: 'tagfield',
                hidden: true,
                width: '100%',
                store: {
                    fields: ['name', 'text']
                },
                displayField: 'text',
                valueField: 'name',
                bind: {
                    value: '{widget.displayColumns}',
                    hidden: '{_columnsck}'
                },
                queryMode: 'local',
                // listeners: {
                //     change: 'updateColumns'
                // }
            }]
        }],

        buttons: [{
            text: 'Remove'.t(),
            iconCls: 'fa fa-trash-o fa-red',
            hidden: true,
            bind: { hidden: '{!(entry && onDashboard)}' },
            handler: 'onRemoveWidget'
        }, '->', {
            text: 'Cancel'.t(),
            iconCls: 'fa fa-ban',
            handler: function (btn) {
                btn.up('window').close();
            }
        }, {
            text: 'Add'.t(),
            iconCls: 'fa fa-plus-circle',
            hidden: true,
            bind: { hidden: '{!(entry && !onDashboard)}' },
            handler: 'onAdd'
        }, {
            text: 'Save'.t(),
            iconCls: 'fa fa-floppy-o',
            hidden: true,
            bind: { hidden: '{!(entry && onDashboard)}' },
            handler: 'onSave'
        }]
    }],

    // dockedItems: [{
    //     xtype: 'toolbar',
    //     dock: 'bottom',
    //     weight: 0,



    // }]
});
