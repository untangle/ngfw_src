Ext.define("Ung.window.ReportEditor", {
    extend: "Ung.RowEditorWindow",
    rowEditorLabelWidth: 150,
    parentCmp: null,
    initComponent: function() {
        if(!this.forReportCustomization) {
            this.tbar = [{
                xtype: 'button',
                text: i18n._('View Report'),
                iconCls: 'icon-play',
                handler: function() {
                    if (this.validate()!==true) {
                        return;
                    }
                    if (this.record !== null) {
                        var data = Ext.clone(this.record.getData());
                        this.updateActionRecursive(this.items, data, 0);
                        this.parentCmp.viewReport(data);
                    }
                },
                scope: this
            }, {
                xtype: 'button',
                text: i18n._('Copy Report'),
                iconCls: 'action-icon',
                handler: function() {
                    var data = Ext.clone(this.grid.emptyRow);
                    this.updateActionRecursive(this.items, data, 0);
                    Ext.apply(data, {
                        uniqueId: this.getUniqueId(),
                        title: Ext.String.format("Copy of {0}", data.title)
                    });
                    this.closeWindow();
                    this.grid.addHandler(null, null, data);
                    Ext.MessageBox.alert(i18n._("Copy Report"), Ext.String.format(i18n._("You are now editing the copied report: '{0}'"), data.title));
                },
                scope: this
            }];
        } else {
            this.tbar = [{
                xtype: 'button',
                text: i18n._('Save as New Report'),
                iconCls: 'save-icon',
                handler: function() {
                    if (this.validate()!==true) {
                        return false;
                    }
                    if (this.record !== null) {
                        var data = {};
                        this.updateActionRecursive(this.items, data, 0);
                        this.record.set(data);
                        this.record.set("readOnly", false);
                        this.record.set("uniqueId", this.getUniqueId());
                        var entry = this.record.getData();
                        
                        Ung.Main.getReportsManagerNew().saveReportEntry(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            this.closeWindow();
                            this.parentCmp.loadReportEntries(entry.uniqueId);
                        }, this), entry);
                    }
                },
                scope: this
            }];
        }

        var categoryStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["displayName"],
            data: []
        });
        rpc.nodeManager.getAllNodeProperties(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var data=[{displayName: 'System'}];
            var nodeProperties = result.list;
            for (var i=0; i< nodeProperties.length; i++) {
                if(!nodeProperties[i].invisible || nodeProperties[i].displayName == 'Shield') {
                    data.push(nodeProperties[i]);
                }
            }
            categoryStore.loadData(data);
        }, this));

        var tablesStore = Ext.create('Ext.data.Store', {
            sorters: "name",
            fields: ["name"],
            data: []
        });
        Ung.Main.getReportsManagerNew().getTables(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var tables = [];
            for (var i=0; i< result.length; i++) {
                tables.push({ name: result[i]});
            }
            tablesStore.loadData(tables);
        }, this));
        
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "header",
            fields: ["dataIndex", "header"],
            data: []
        });
        var chartTypes = [["TEXT", i18n._("Text")],["PIE_GRAPH", i18n._("Pie Graph")],["TIME_GRAPH", i18n._("Time Graph")],["TIME_GRAPH_DYNAMIC", i18n._("Time Graph Dynamic")]];
        
        var gridSqlConditionsEditor = Ext.create('Ung.grid.Panel',{
            name: 'Sql Conditions',
            height: 180,
            width: '100%',
            settingsCmp: this,
            addAtTop: false,
            hasImportExport: false,
            dataIndex: 'conditions',
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.node.reports.SqlCondition",
            emptyRow: {
                "column": "",
                "operator": "=",
                "value": ""
                
            },
            fields: ["column", "value", "operator"],
            columns: [{
                header: i18n._("Column"),
                dataIndex: 'column',
                width: 280
            }, {
                header: i18n._("Operator"),
                dataIndex: 'operator',
                width: 100
            }, {
                header: i18n._("Value"),
                dataIndex: 'value',
                flex: 1,
                width: 200
            }],
            rowEditorInputLines: [{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: 'combo',
                    emptyText: i18n._("[enter column]"),
                    dataIndex: "column",
                    fieldLabel: i18n._("Column"),
                    typeAhead:true,
                    allowBlank: false,
                    valueField: "dataIndex",
                    displayField: "header",
                    queryMode: 'local',
                    width: 600,
                    store: this.columnsStore
                }, {
                    xtype: 'label',
                    html: i18n._("(Columns list is loaded for the entered Table')"),
                    cls: 'boxlabel'
                }]
            }, {
                xtype: 'combo',
                emptyText: i18n._("[select operator]"),
                dataIndex: "operator",
                fieldLabel: i18n._("Operator"),
                editable: false,
                allowBlank: false,
                valueField: "name",
                displayField: "name",
                queryMode: 'local',
                store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"]
            }, {
                xtype: 'textfield',
                dataIndex: "value",
                fieldLabel: i18n._("Value"),
                emptyText: i18n._("[no value]"),
                width: '90%'
            }],
            setValue: function (val) {
                var data = val || [];
                this.reload({data:data});
            },
            getValue: function () {
                var val = this.getList();
                return val.length == 0 ? null: val;
            }
        });
        
        this.inputLines = [{
            xtype: 'combo',
            name: 'Category',
            dataIndex: "category",
            allowBlank: false,
            editable: false,
            valueField: 'displayName',
            displayField: 'displayName',
            fieldLabel: i18n._('Category'),
            emptyText: i18n._("[select category]"),
            queryMode: 'local',
            width: 500,
            readOnly: this.forReportCustomization,
            store: categoryStore
        }, {
            xtype:'textfield',
            name: "Title",
            dataIndex: "title",
            allowBlank: false,
            fieldLabel: i18n._("Title"),
            emptyText: i18n._("[enter title]"),
            width: '100%'
        }, {
            xtype:'textfield',
            name: "Description",
            dataIndex: "description",
            fieldLabel: i18n._("Description"),
            emptyText: i18n._("[no description]"),
            width: '100%'
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '5 0 5 0',
            items: [{
                xtype:'checkbox',
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enabled"),
                labelWidth: 150
            }, {
                xtype: 'numberfield',
                name: 'Display Order',
                fieldLabel: i18n._('Display Order'),
                dataIndex: "displayOrder",
                allowDecimals: false,
                minValue: 0,
                maxValue: 100000,
                allowBlank: false,
                width: 282,
                style: { marginLeft: '50px'}
            }]
        }, {
            xtype:'textfield',
            name: "Units",
            dataIndex: "units",
            fieldLabel: i18n._("Units"),
            emptyText: i18n._("[no units]"),
            width: 500
        }, {
            xtype: 'combo',
            name: "Table",
            dataIndex: "table",
            allowBlank: false,
            fieldLabel: i18n._("Table"),
            emptyText: i18n._("[enter table]"),
            valueField: "name",
            displayField: "name",
            queryMode: 'local',
            width: 500,
            store: tablesStore,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, newValue) {
                        Ung.TableConfig.getColumnsForTable(newValue, this.columnsStore);
                    }, this),
                    buffer: 600
                }
            }
        }, {
            xtype: 'combo',
            name: 'Type',
            margin: '10 0 10 0',
            dataIndex: "type",
            allowBlank: false,
            editable: false,
            fieldLabel: i18n._('Type'),
            queryMode: 'local',
            width: 350,
            store: chartTypes,
            listeners: {
                "select": {
                    fn: Ext.bind(function(combo, records, eOpts) {
                        this.syncComponents();
                    }, this)
                }
            }
        }, {
            xtype: "container",
            dataIndex: "textColumns",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "textColumns",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Text Columns"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one column per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var textColumns  = this.down('textfield[name="textColumns"]');
                textColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var textColumns = [];
                var val  = this.down('textfield[name="textColumns"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            textColumns.push(colVal);
                        }
                    }
                }
                
                return textColumns.length==0 ? null : textColumns;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="textColumns"]').setReadOnly(val);
            }
        }, {
            xtype:'textfield',
            name: "textString",
            dataIndex: "textString",
            alowBlank: false,
            fieldLabel: i18n._("Text String"),
            width: '100%'
        }, {
            xtype:'textfield',
            name: "pieGroupColumn",
            dataIndex: "pieGroupColumn",
            fieldLabel: i18n._("Pie Group Column"),
            width: 500
        }, {
            xtype:'textfield',
            name: "pieSumColumn",
            dataIndex: "pieSumColumn",
            fieldLabel: i18n._("Pie Sum Column"),
            width: 500
        }, {
            xtype: 'numberfield',
            name: 'pieNumSlices',
            fieldLabel: i18n._('Pie Slices Number'),
            dataIndex: "pieNumSlices",
            allowDecimals: false,
            minValue: 0,
            maxValue: 1000,
            allowBlank: false,
            width: 350
        }, {
            xtype: 'combo',
            name: 'timeStyle',
            dataIndex: "timeStyle",
            editable: false,
            fieldLabel: i18n._('Time Chart Style'),
            queryMode: 'local',
            allowBlank: false,
            width: 350,
            store: [
                ["LINE", i18n._("Line")],
                ["AREA", i18n._("Area")],
                ["BAR_3D", i18n._("Bar 3D")],
                ["BAR_3D_OVERLAPPED", i18n._("Bar 3D Overlapped")],
                ["BAR", i18n._("Bar")],
                ["BAR_OVERLAPPED", i18n._("Bar Overlapped")]
            ]
        }, {
            xtype: 'combo',
            name: 'timeDataInterval',
            dataIndex: "timeDataInterval",
            editable: false,
            fieldLabel: i18n._('Time Data Interval'),
            queryMode: 'local',
            allowBlank: false,
            width: 350,
            store: [
                ["AUTO", i18n._("Auto")],
                ["SECOND", i18n._("Second")],
                ["MINUTE", i18n._("Minute")],
                ["TENMINUTE", i18n._("10 Minutes")],
                ["HOUR", i18n._("Hour")],
                ["DAY", i18n._("Day")],
                ["WEEK", i18n._("Week")],
                ["MONTH", i18n._("Month")]
            ]
        }, {
            xtype: "container",
            dataIndex: "timeDataColumns",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "timeDataColumns",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Time Data Columns"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one column per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var timeDataColumns  = this.down('textfield[name="timeDataColumns"]');
                timeDataColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var timeDataColumns = [];
                var val  = this.down('textfield[name="timeDataColumns"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            timeDataColumns.push(colVal);
                        }
                    }
                }
                
                return timeDataColumns.length==0 ? null : timeDataColumns;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="timeDataColumns"]').setReadOnly(val);
            }
        }, {
            xtype:'textfield',
            name: "timeDataDynamicValue",
            dataIndex: "timeDataDynamicValue",
            fieldLabel: i18n._("Time Data Dynamic Value"),
            width: 500
        }, {
            xtype:'textfield',
            name: "timeDataDynamicColumn",
            dataIndex: "timeDataDynamicColumn",
            fieldLabel: i18n._("Time Data Dynamic Column"),
            width: 500
        }, {
            xtype: 'numberfield',
            name: 'timeDataDynamicLimit',
            dataIndex: "timeDataDynamicLimit",
            fieldLabel: i18n._('Time Data Dynamic Limit'),
            allowDecimals: false,
            minValue: 0,
            maxValue: 100000,
            width: 350
        }, {
            xtype:'textfield',
            name: "timeDataDynamicAggregationFunction",
            dataIndex: "timeDataDynamicAggregationFunction",
            fieldLabel: i18n._("Time Data Aggregation Function"),
            width: 500
        }, {
            xtype:'textfield',
            name: "seriesRenderer",
            dataIndex: "seriesRenderer",
            fieldLabel: i18n._("Series Renderer"),
            width: 500
        }, {
            xtype: "container",
            dataIndex: "colors",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "colors",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Colors"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one color per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var timeDataColumns = this.down('textfield[name="colors"]');
                timeDataColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var colors = [];
                var val  = this.down('textfield[name="colors"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            colors.push(colVal);
                        }
                    }
                }
                
                return colors.length==0 ? null : colors;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="colors"]').setReadOnly(val);
            }
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '10 0 10 0',
            items: [{
                xtype:'textfield',
                name: "orderByColumn",
                dataIndex: "orderByColumn",
                fieldLabel: i18n._("Order By Column"),
                labelWidth: 150,
                width: 350
            },{
                xtype: 'combo',
                name: 'orderDesc',
                dataIndex: "orderDesc",
                editable: false,
                fieldLabel: i18n._('Order Direction'),
                queryMode: 'local',
                width: 300,
                style: { marginLeft: '10px'},
                store: [[null, ""], [false, i18n._("Ascending")], [true, i18n._("Descending")]]
            }]
        }, {
            xtype:'fieldset',
            title: i18n._("Sql Conditions:"),
            items:[gridSqlConditionsEditor]
        }];
        this.callParent(arguments);
    },
    getUniqueId: function() {
        return "report-"+Math.random().toString(36).substr(2);
    },
    populate: function(record, addMode) {
        Ung.TableConfig.getColumnsForTable(record.get("table"), this.columnsStore);
        if(!record.get("uniqueId")) {
            record.set("uniqueId", this.getUniqueId());
        }
        if(this.forReportCustomization && record.get("title").indexOf(i18n._("Custom")) == -1) {
            record.set("title", record.get("title") + " - " + i18n._("Custom"));
        }
        this.callParent(arguments);
    },
    syncComponents: function () {
        if(!this.cmps) {
            this.cmps = {
                typeCmp: this.down('combo[dataIndex=type]'),
                textColumns: this.down('[dataIndex=textColumns]'),
                textString: this.down('[dataIndex=textString]'),
                pieGroupColumn: this.down('[dataIndex=pieGroupColumn]'),
                pieSumColumn: this.down('[dataIndex=pieSumColumn]'),
                pieNumSlices: this.down('[dataIndex=pieNumSlices]'),
                timeStyle: this.down('[dataIndex=timeStyle]'),
                timeDataInterval: this.down('[dataIndex=timeDataInterval]'),
                timeDataColumns: this.down('[dataIndex=timeDataColumns]'),
                timeDataDynamicValue: this.down('[dataIndex=timeDataDynamicValue]'),
                timeDataDynamicColumn: this.down('[dataIndex=timeDataDynamicColumn]'),
                timeDataDynamicLimit: this.down('[dataIndex=timeDataDynamicLimit]'),
                timeDataDynamicAggregationFunction: this.down('[dataIndex=timeDataDynamicAggregationFunction]'),
                seriesRenderer: this.down('[dataIndex=seriesRenderer]'),
                colors: this.down('[dataIndex=colors]')
            };
        }
        var type = this.cmps.typeCmp.getValue();
        
        this.cmps.textColumns.setVisible(type=="TEXT");
        this.cmps.textColumns.setDisabled(type!="TEXT");

        this.cmps.textString.setVisible(type=="TEXT");
        this.cmps.textString.setDisabled(type!="TEXT");

        this.cmps.pieGroupColumn.setVisible(type=="PIE_GRAPH");
        this.cmps.pieGroupColumn.setDisabled(type!="PIE_GRAPH");

        this.cmps.pieSumColumn.setVisible(type=="PIE_GRAPH");
        this.cmps.pieSumColumn.setDisabled(type!="PIE_GRAPH");

        this.cmps.pieNumSlices.setVisible(type=="PIE_GRAPH");
        this.cmps.pieNumSlices.setDisabled(type!="PIE_GRAPH");

        this.cmps.timeStyle.setVisible(type=="TIME_GRAPH");
        this.cmps.timeStyle.setDisabled(type!="TIME_GRAPH");

        this.cmps.timeDataInterval.setVisible(type=="TIME_GRAPH" || type=="TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataInterval.setDisabled(type!="TIME_GRAPH" && type!="TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataColumns.setVisible(type=="TIME_GRAPH");
        this.cmps.timeDataColumns.setDisabled(type!="TIME_GRAPH");

        this.cmps.timeDataDynamicValue.setVisible(type=="TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicValue.setDisabled(type!="TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicColumn.setVisible(type=="TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicColumn.setDisabled(type!="TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicLimit.setVisible(type=="TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicLimit.setDisabled(type!="TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicAggregationFunction.setVisible(type=="TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicAggregationFunction.setDisabled(type!="TIME_GRAPH_DYNAMIC");

        this.cmps.seriesRenderer.setVisible(type=="TIME_GRAPH" || type=="TIME_GRAPH_DYNAMIC");
        this.cmps.seriesRenderer.setDisabled(type!="TIME_GRAPH" && type!="TIME_GRAPH_DYNAMIC");

        this.cmps.colors.setVisible(type!="TEXT");
        this.cmps.colors.setDisabled(type=="TEXT");
    }
});