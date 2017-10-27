Ext.define('Ung.reports.cmp.ImportDialog', {
    extend: 'Ext.window.Window',
    alias: 'widget.importdialog',
    width: 1000,
    height: 500,
    modal: true,
    title: 'Import Reports'.t(),

    layout: 'fit',

    viewModel: {
        data: { reports: [] }
    },

    tbar: [{
        xtype: 'filefield',
        reference: 'filefield',
        publishes: 'value',
        margin: 5,
        flex: 1,
        fieldLabel: 'Select File'.t(),
        labelAlign: 'right',
        allowBlank: false,
        listeners: {
            afterrender: 'onAfterRenderField'
        }
    }],

    fbar: [{
        xtype: 'checkbox',
        reference: 'replaceAllCk',
        boxLabel: '<strong>' + 'Replace ALL existing reports'.t() + '</strong>',
        listeners: {
            change: function (ck) {
                ck.up('window').down('grid').getView().refresh();
            }
        }
    }, '->', {
        text: 'Cancel'.t(),
        iconCls: 'fa fa-ban fa-lg',
        scale: 'medium',
        handler: function (btn) {
            btn.up('window').close();
        }
    }, {
        text: 'Import Selected'.t(),
        iconCls: 'fa fa-arrow-down',
        scale: 'medium',
        disabled: true,
        bind: {
            disabled: '{!reportsGrid.selection}'
        },
        handler: 'doImport'
    }],

    items: [{
        xtype: 'grid',
        reference: 'reportsGrid',
        enableColumnHide: false,
        hideHeaders: true,
        dockedItems: [{
            xtype: 'component',
            margin: 10,
            html: '<i class="fa fa-info-circle fa-lg"></i> ' + 'Select which Reports to import. Red Titles have to be renamed to avoid conflicts with existing records.'.t(),
            hidden: true,
            bind: {
                hidden: '{!filefield.value}'
            }
        }],
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 20px; font-size: 14px;"><i class="fa fa-info-circle fa-gray fa-lg"></i> ' + 'Select a file from which to import data!'.t() + '</p>'
        },
        // features: [{
        //     ftype: 'grouping',
        // }],
        plugins: [{
            ptype: 'cellediting',
            clicksToEdit: 1
        }],
        selModel: {
            selType: 'checkboxmodel',
            checkOnly: true
        },
        bind: {
            hideHeaders: '{!filefield.value}',
            store: {
                data: '{reports}',
                model: 'Ung.model.Report',
                groupField: 'category',
                sorters: [{
                    property: 'category',
                    direction: 'ASC'
                }]
            }
        },
        columns: [{
            header: 'Title'.t(),
            dataIndex: 'title',
            width: 200,
            renderer: 'titleRenderer',
            editor: {
                xtype: 'textfield',
                allowBlank: false
            }
        }, {
            header: 'Category'.t(),
            dataIndex: 'category',
            width: 150
        }, {
            header: 'ReadOnly'.t(),
            width: 70,
            xtype: 'checkcolumn',
            dataIndex: 'readOnly'
        }, {
            header: 'Description'.t(),
            dataIndex: 'description',
            flex: 1,
            editor: {
                xtype: 'textfield'
            }
        }, {
            dataIndex: 'icon',
            resizable: false,
            width: 30,
            align: 'center',
            renderer: function (val) {
                return '<i class="fa ' + val + ' fa-gray" style="font-size: 14px;"></i>';
            }
        }, {
            header: 'Type'.t(),
            dataIndex: 'type',
            width: 120,
            renderer: function (val) {
                switch (val) {
                case 'TEXT': return 'Text'.t();
                case 'PIE_GRAPH': return 'Pie Graph'.t();
                case 'TIME_GRAPH': return 'Time Graph'.t();
                case 'TIME_GRAPH_DYNAMIC': return 'Time Graph Dynamic'.t();
                case 'EVENT_LIST': return 'Event List'.t();
                }
            }
        }]
    }],

    controller: {
        onAfterRenderField: function (el) {
            var vm = this.getViewModel();
            if (!window.FileReader) {
                console.info('Browser is not supporting FileReader!');
                return;
            }
            // add change event to underlaying dom file field
            el.getEl().dom.addEventListener('change', function (e) {
                var reader = new FileReader();
                reader.addEventListener('loadend', function () {
                    vm.set('reports', Ext.JSON.decode(reader.result));
                });
                reader.readAsText(e.target.files[0]);
            });
        },

        control: {
            '#': {
                afterrender: 'onAfterRender',
            }
        },

        onAfterRender: function () {
            // build existing reports names list
            var reportTitles = [];
            Ext.getStore('reports').each(function (report) {
                reportTitles.push(report.get('title'));
            });
            this.titles = reportTitles;
        },

        titleRenderer: function (value, metaData) {
            var me = this;
            if (Ext.Array.contains(this.titles, value) && !me.lookup('replaceAllCk').getValue()) {
                metaData.tdStyle = 'color: red';
            }
            return value;
        },

        doImport: function () {
            var selection = this.lookup('reportsGrid').getSelection(), reports = [];

            Ext.Array.each(selection, function (record) {
                reports.push(record.getData());
            });

            console.log(reports);
        }
    }

});
