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

    items: [{
        xtype: 'form',
        url: 'gridSettings',
        border: false,
        layout: 'fit',
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
                    model: 'Ung.model.Report'
                    // groupField: 'category',
                    // sorters: [{
                    //     property: 'category',
                    //     direction: 'ASC'
                    // }]
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
                header: 'Type'.t(),
                dataIndex: 'type',
                width: 120,
                renderer: function (val, meta, record) {
                    var str = '<i class="fa ' + record.get('icon') + ' fa-gray" style="font-size: 14px;"></i> ';
                    switch (val) {
                    case 'TEXT': str += 'Text'.t(); break;
                    case 'PIE_GRAPH': str += 'Pie Graph'.t(); break;
                    case 'TIME_GRAPH': str += 'Time Graph'.t(); break;
                    case 'TIME_GRAPH_DYNAMIC': str += 'Time Graph Dynamic'.t(); break;
                    case 'EVENT_LIST': str += 'Event List'.t(); break;
                    }
                    return str;
                }
            }]
        }, {
            xtype: 'hidden',
            name: 'type',
            value: 'import'
        }, {
            xtype: 'hidden',
            name: 'importMode',
            value: 'replace'
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

        doImport: function (btn) {
            var me = this, vm = me.getViewModel();
            Rpc.asyncData( 'rpc.appManager.app("reports").getSettings')
            .then(function(result){
                if(Util.isDestroyed(me, vm)){
                    return;
                }

                var reportsSettings = result,
                    selection = me.lookup('reportsGrid').getSelection(),
                    reports = [],
                    titleConflict = false;

                Ext.Array.each(selection, function (record) {
                    if (Ext.Array.contains(me.titles, record.get('title')) && !me.lookup('replaceAllCk').getValue() && !titleConflict) {
                        titleConflict = true;
                    }
                });

                if (titleConflict) {
                    Ext.MessageBox.alert('Warning'.t(), 'There are some report titles conflicts. Make sure selected reports have unique name!'.t());
                    return;
                }

                Ext.Array.each(selection, function (record) {
                    var rep = record.getData();
                    delete rep._id;
                    delete rep.localizedTitle;
                    delete rep.localizedDescription;
                    delete rep.slug;
                    delete rep.categorySlug;
                    delete rep.url;
                    delete rep.icon;

                    // if appending check uniqueId conflit and generate another
                    if (!rep.uniqueId || (!me.lookup('replaceAllCk').getValue() && Ext.getStore('reports').find('uniqueId', rep.uniqueId) > 0)) {
                        rep.uniqueId = 'report-' + Math.random().toString(36).substr(2);
                    }
                    reports.push(rep);
                });

                // if replace all
                if (me.lookup('replaceAllCk').getValue()) {
                    reportsSettings.reportEntries.list = reports;
                } else {
                    // append
                    Ext.Array.push(reportsSettings.reportEntries.list, reports);
                }

                Rpc.asyncData( 'rpc.appManager.app("reports").setSettings', reportsSettings)
                .then(function(result){
                    Rpc.asyncData( 'rpc.appManager.app("reports").getSettings' )
                    .then(function(settings){
                        Ext.getStore('reports').loadData(settings.reportEntries.list);
                        Ext.getStore('reportstree').build();
                        btn.up('window').close();
                        Ung.app.redirectTo('#reports');
                        Ext.MessageBox.alert('Import done!'.t(), reports.length + ' ' + 'Report(s) imported!'.t());
                        if(reports && reports.length){
                            var url = '#reports?cat=' + reports[0].category.replace(/ /g, '-').toLowerCase();
                            if(reports.length == 1){
                                url += '&rep=' + reports[0].title.replace(/ /g, '-').toLowerCase();
                            }
                            Ung.app.redirectTo(url);
                        }
                    },function(ex){
                        Util.handleException(ex);
                    });
                }, function(ex) {
                    Util.handleException(ex);
                });
            }, function(ex) {
                Util.handleException(ex);
            });
        }
    }

});
