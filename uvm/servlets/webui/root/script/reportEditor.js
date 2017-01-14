/*global
 Ext, Ung, i18n, rpc
 */

Ext.define("Ung.window.ReportEditor", {
    extend: "Ung.RowEditorWindow",
    rowEditorLabelWidth: 150,
    parentCmp: null,
    initComponent: function () {
        this.bbar = ['->', {
            name: "Update",
            itemId: 'updateReportBtn',
            iconCls: 'apply-icon',
            text: i18n._('Update'),
            handler: function () {
                var data = {};
                this.updateActionRecursive(this.items, data, 0);
                this.record.set(data);
                var entry = this.record.getData();
                this.setLoading('<strong>' + i18n._('Updating report') + ':</strong> ' + entry.title + '...');
                Ung.Main.getReportsManager().saveReportEntry(Ext.bind(function (result, exception) {
                    this.setLoading(false);
                    if (Ung.Util.handleException(exception)) {
                        return;
                    }
                    this.closeWindow();
                    this.parentCmp.initEntry = entry;
                    Ung.dashboard.reportEntriesModified = true;
                    Ung.Util.userActionToast('<span style="color: #FFF;">' + entry.title + '</span> ' + i18n._('updated successfully'));
                    this.reloadReports();
                }, this), entry);
            },
            scope: this
        }, '-', {
            xtype: 'button',
            itemId: 'saveReportBtn',
            text: i18n._('Save as New Report'),
            iconCls: 'save-icon',
            handler: function () {
                if (this.validate() !== true) {
                    return false;
                }
                if (this.record !== null) {
                    var data = {};
                    this.updateActionRecursive(this.items, data, 0);
                    this.record.set(data);
                    this.record.set("readOnly", false);
                    this.record.set("uniqueId", this.getUniqueId());
                    var entry = this.record.getData();

                    this.setLoading('<strong>' + i18n._('Creating report') + ':</strong> ' + entry.title + '...');
                    Ung.Main.getReportsManager().saveReportEntry(Ext.bind(function (result, exception) {
                        this.setLoading(false);
                        if (Ung.Util.handleException(exception)) {
                            return;
                        }
                        this.closeWindow();
                        this.parentCmp.initEntry = entry;
                        Ung.dashboard.reportEntriesModified = true;
                        Ung.Util.userActionToast('<span style="color: #FFF;">' + entry.title + '</span> ' + i18n._('created successfully'));
                        this.reloadReports();
                    }, this), entry);
                }
            },
            scope: this
        }, '-', {
            name: "Cancel",
            id: this.getId() + "_cancelBtn",
            iconCls: 'cancel-icon',
            text: i18n._('Cancel'),
            handler: Ext.bind(function () {
                this.cancelAction();
            }, this)
        }];

        var categoryStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["displayName"],
            data: []
        });
        rpc.nodeManager.getAllNodeProperties(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            var data = [{displayName: 'System'}],
                nodeProperties = result.list,
                i;
            for (i = 0; i < nodeProperties.length; i += 1) {
                if (!nodeProperties[i].invisible || nodeProperties[i].displayName == 'Shield') {
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
        Ung.Main.getReportsManager().getTables(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            var tables = [], i;
            if ( result != null && result.length != null ) {
                for (i = 0; i < result.length;  i += 1) {
                    tables.push({ name: result[i]});
                }
            }
            tablesStore.loadData(tables);
        }, this));

        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "header",
            fields: ["dataIndex", "header"],
            data: []
        });
        var chartTypes = [["TEXT", i18n._("Text")], ["PIE_GRAPH", i18n._("Pie Graph")], ["TIME_GRAPH", i18n._("Time Graph")], ["TIME_GRAPH_DYNAMIC", i18n._("Time Graph Dynamic")], ["EVENT_LIST", i18n._("Event List")]];

        var gridSqlConditionsEditor = Ext.create('Ung.grid.Panel', {
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
                    typeAhead: true,
                    allowBlank: false,
                    valueField: "dataIndex",
                    queryMode: 'local',
                    width: 600,
                    tpl: Ext.create('Ext.XTemplate',
                        '<ul class="x-list-plain"><tpl for=".">',
                            '<li role="option" class="x-boundlist-item"><b>{header}</b> <span style="float: right;">[{dataIndex}]</span></li>',
                        '</tpl></ul>'
                        ),
                    // template for the content inside text field
                    displayTpl: Ext.create('Ext.XTemplate',
                        '<tpl for=".">',
                            '{header} [{dataIndex}]',
                        '</tpl>'
                        ),
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
                this.reload({data: data});
            },
            getValue: function () {
                var val = this.getList();
                return val.length == 0 ? null : val;
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
            disabled: true,
            store: categoryStore
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
            disabled: true,
            listeners: {
                "select": {
                    fn: Ext.bind(function (combo, records, eOpts) {
                        this.syncComponents();
                    }, this)
                }
            }
        }, {
            xtype: 'textfield',
            name: "Title",
            dataIndex: "title",
            allowBlank: false,
            fieldLabel: i18n._("Title"),
            emptyText: i18n._("[enter title]"),
            width: '100%'
        }, {
            xtype: 'textfield',
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
                xtype: 'checkbox',
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
            xtype: 'textfield',
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
                    fn: Ext.bind(function (elem, newValue) {
                        this.down('[dataIndex=defaultColumns]').setValue([]);
                        Ung.TableConfig.getColumnsForTable(newValue, this.columnsStore);
                    }, this)
                }
            }
        }, {
            xtype: 'fieldcontainer',
            name: "Columns",
            dataIndex: "defaultColumns",
            defaultType: 'checkboxfield',
            queryMode: 'local',
            fieldLabel: i18n._('Columns'),
            items: [],
            layout: {
                type: 'column'
            },
            setValue: Ext.bind(function (defaultColumns) {
                if (defaultColumns) {
                    var table = this.down('[dataIndex=table]').getValue(), tableConfig = Ext.clone(Ung.TableConfig.getConfig(table)), i, col;
                    if (table === this.entry.table) {
                        defaultColumns = this.entry.defaultColumns;
                    }
                    this.down('[dataIndex=defaultColumns]').removeAll();
                    for (i = 0; i < tableConfig.columns.length; i += 1) {
                        col = tableConfig.columns[i];
                        this.down('[dataIndex=defaultColumns]').add({
                            //boxLabel: col.header + ' <span style="color: #999;">[' + col.dataIndex + ']</span>',
                            boxLabel: col.header,
                            name: col.dataIndex,
                            inputValue: col.dataIndex,
                            checked: defaultColumns.indexOf(col.dataIndex) >= 0,
                            padding: '0 5',
                            width: 300
                        });
                    }
                }
            }, this),
            getValue: function () {
                var cols = [], cks = this.query('checkbox'), i;
                for (i = 0; i < cks.length; i += 1) {
                    if (cks[i].getValue()) {
                        cols.push(cks[i].getName());
                    }
                }
                return cols;
            }
        }, {
            xtype: "container",
            dataIndex: "textColumns",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype: 'textareafield',
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
            setValue: function (value) {
                var textColumns  = this.down('textfield[name="textColumns"]');
                textColumns.setValue((value || []).join("\n"));
            },
            getValue: function () {
                var textColumns = [];
                var val  = this.down('textfield[name="textColumns"]').getValue(), i;
                if (!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for (i = 0; i < valArr.length; i += 1) {
                        colVal = valArr[i].trim();
                        if (!Ext.isEmpty(colVal)) {
                            textColumns.push(colVal);
                        }
                    }
                }

                return textColumns.length == 0 ? null : textColumns;
            },
            setReadOnly: function (val) {
                this.down('textfield[name="textColumns"]').setReadOnly(val);
            }
        }, {
            xtype: 'textfield',
            name: "textString",
            dataIndex: "textString",
            alowBlank: false,
            fieldLabel: i18n._("Text String"),
            width: '100%'
        }, {
            xtype: 'textfield',
            name: "pieGroupColumn",
            dataIndex: "pieGroupColumn",
            fieldLabel: i18n._("Pie Group Column"),
            width: 500
        }, {
            xtype: 'textfield',
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
            name: 'pieStyle',
            margin: '10 0 10 0',
            dataIndex: "pieStyle",
            allowBlank: false,
            editable: false,
            fieldLabel: i18n._('Style'),
            queryMode: 'local',
            width: 350,
            store: [
                ["PIE", i18n._("Pie")],
                ["PIE_3D", i18n._("Pie 3D")],
                ["DONUT", i18n._("Donut")],
                ["DONUT_3D", i18n._("Donut 3D")],
                ["COLUMN", i18n._("Column")],
                ["COLUMN_3D", i18n._("Column 3D")]
            ]
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
                ["AREA_STACKED", i18n._("Stacked Area")],
                ["BAR", i18n._("Bar")],
                ["BAR_OVERLAPPED", i18n._("Bar Overlapped")],
                ["BAR_STACKED", i18n._("Stacked Columns")]
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
                xtype: 'textareafield',
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
            setValue: function (value) {
                var timeDataColumns  = this.down('textfield[name="timeDataColumns"]');
                timeDataColumns.setValue((value || []).join("\n"));
            },
            getValue: function () {
                var timeDataColumns = [];
                var val  = this.down('textfield[name="timeDataColumns"]').getValue();
                if (!Ext.isEmpty(val)) {
                    var valArr = val.split("\n"), colVal, i;
                    for (i = 0; i < valArr.length; i += 1) {
                        colVal = valArr[i].trim();
                        if (!Ext.isEmpty(colVal)) {
                            timeDataColumns.push(colVal);
                        }
                    }
                }

                return timeDataColumns.length == 0 ? null : timeDataColumns;
            },
            setReadOnly: function (val) {
                this.down('textfield[name="timeDataColumns"]').setReadOnly(val);
            }
        }, {
            xtype: 'textfield',
            name: "timeDataDynamicValue",
            dataIndex: "timeDataDynamicValue",
            fieldLabel: i18n._("Time Data Dynamic Value"),
            width: 500
        }, {
            xtype: 'textfield',
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
            xtype: 'textfield',
            name: "timeDataDynamicAggregationFunction",
            dataIndex: "timeDataDynamicAggregationFunction",
            fieldLabel: i18n._("Time Data Aggregation Function"),
            width: 500
        }, {
            xtype: 'checkbox',
            name: "timeDataDynamicAllowNull",
            dataIndex: "timeDataDynamicAllowNull",
            fieldLabel: i18n._("Time Data Dynamic Allow Null"),
            width: 500
        }, {
            xtype: 'textfield',
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
                xtype: 'textareafield',
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
            setValue: function (value) {
                var timeDataColumns = this.down('textfield[name="colors"]');
                timeDataColumns.setValue((value || []).join("\n"));
            },
            getValue: function () {
                var colors = [];
                var val  = this.down('textfield[name="colors"]').getValue();
                if (!Ext.isEmpty(val)) {
                    var valArr = val.split("\n"), colVal, i;
                    for (i = 0; i < valArr.length; i += 1) {
                        colVal = valArr[i].trim();
                        if (!Ext.isEmpty(colVal)) {
                            colors.push(colVal);
                        }
                    }
                }

                return colors.length == 0 ? null : colors;
            },
            setReadOnly: function (val) {
                this.down('textfield[name="colors"]').setReadOnly(val);
            }
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '10 0 10 0',
            items: [{
                xtype: 'textfield',
                name: "orderByColumn",
                dataIndex: "orderByColumn",
                fieldLabel: i18n._("Order By Column"),
                labelWidth: 150,
                width: 350
            }, {
                xtype: 'combo',
                name: 'orderDesc',
                dataIndex: "orderDesc",
                editable: false,
                fieldLabel: i18n._('Order Direction'),
                labelWidth: 150,
                queryMode: 'local',
                width: 300,
                style: { marginLeft: '10px'},
                store: [[null, ""], [false, i18n._("Ascending")], [true, i18n._("Descending")]]
            }]
        }, {
            xtype: 'fieldset',
            title: i18n._("Sql Conditions:"),
            items: [gridSqlConditionsEditor]
        }];
        this.callParent(arguments);
    },
    getUniqueId: function () {
        return "report-" + Math.random().toString(36).substr(2);
    },
    populate: function (record, addMode) {
        this.down('#updateReportBtn').setHidden(record.getData().readOnly);
        this.entry = record.getData();
        Ung.TableConfig.getColumnsForTable(record.get("table"), this.columnsStore);
        if (!record.get("uniqueId")) {
            record.set("uniqueId", this.getUniqueId());
        }
        /*
        if(this.forReportCustomization && record.get("title").indexOf(i18n._("Custom")) == -1) {
            record.set("title", record.get("title") + " - " + i18n._("Custom"));
        }
        */
        this.callParent(arguments);
    },
    syncComponents: function () {
        if (!this.cmps) {
            this.cmps = {
                typeCmp: this.down('combo[dataIndex=type]'),
                textColumns: this.down('[dataIndex=textColumns]'),
                textString: this.down('[dataIndex=textString]'),
                pieGroupColumn: this.down('[dataIndex=pieGroupColumn]'),
                pieSumColumn: this.down('[dataIndex=pieSumColumn]'),
                pieNumSlices: this.down('[dataIndex=pieNumSlices]'),
                pieStyle: this.down('[dataIndex=pieStyle]'),
                timeStyle: this.down('[dataIndex=timeStyle]'),
                timeDataInterval: this.down('[dataIndex=timeDataInterval]'),
                timeDataColumns: this.down('[dataIndex=timeDataColumns]'),
                timeDataDynamicValue: this.down('[dataIndex=timeDataDynamicValue]'),
                timeDataDynamicColumn: this.down('[dataIndex=timeDataDynamicColumn]'),
                timeDataDynamicLimit: this.down('[dataIndex=timeDataDynamicLimit]'),
                timeDataDynamicAggregationFunction: this.down('[dataIndex=timeDataDynamicAggregationFunction]'),
                timeDataDynamicAllowNull: this.down('[dataIndex=timeDataDynamicAllowNull]'),
                seriesRenderer: this.down('[dataIndex=seriesRenderer]'),
                colors: this.down('[dataIndex=colors]'),
                units: this.down('[dataIndex=units]'),
                orderByColumn: this.down('[dataIndex=orderByColumn]'),
                orderDirection: this.down('[dataIndex=orderDesc]'),
                defaultColumns: this.down('[dataIndex=defaultColumns]')
            };
        }
        var type = this.cmps.typeCmp.getValue();

        this.cmps.textColumns.setVisible(type == "TEXT");
        this.cmps.textColumns.setDisabled(type != "TEXT");

        this.cmps.textString.setVisible(type == "TEXT");
        this.cmps.textString.setDisabled(type != "TEXT");

        this.cmps.pieGroupColumn.setVisible(type == "PIE_GRAPH");
        this.cmps.pieGroupColumn.setDisabled(type != "PIE_GRAPH");

        this.cmps.pieSumColumn.setVisible(type == "PIE_GRAPH");
        this.cmps.pieSumColumn.setDisabled(type != "PIE_GRAPH");

        this.cmps.pieNumSlices.setVisible(type == "PIE_GRAPH");
        this.cmps.pieNumSlices.setDisabled(type != "PIE_GRAPH");

        this.cmps.pieStyle.setVisible(type == "PIE_GRAPH");
        this.cmps.pieStyle.setDisabled(type != "PIE_GRAPH");

        this.cmps.timeStyle.setVisible(type == "TIME_GRAPH" || type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeStyle.setDisabled(type != "TIME_GRAPH" && type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataInterval.setVisible(type == "TIME_GRAPH" || type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataInterval.setDisabled(type != "TIME_GRAPH" && type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataColumns.setVisible(type == "TIME_GRAPH");
        this.cmps.timeDataColumns.setDisabled(type != "TIME_GRAPH");

        this.cmps.timeDataDynamicValue.setVisible(type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicValue.setDisabled(type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicColumn.setVisible(type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicColumn.setDisabled(type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicLimit.setVisible(type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicLimit.setDisabled(type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicAggregationFunction.setVisible(type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicAggregationFunction.setDisabled(type != "TIME_GRAPH_DYNAMIC");

        this.cmps.timeDataDynamicAllowNull.setVisible(type == "TIME_GRAPH_DYNAMIC");
        this.cmps.timeDataDynamicAllowNull.setDisabled(type != "TIME_GRAPH_DYNAMIC");
        
        this.cmps.seriesRenderer.setVisible(type == "TIME_GRAPH" || type == "TIME_GRAPH_DYNAMIC");
        this.cmps.seriesRenderer.setDisabled(type != "TIME_GRAPH" && type != "TIME_GRAPH_DYNAMIC");

        this.cmps.colors.setVisible(type != "TEXT" && type != "EVENT_LIST");
        this.cmps.colors.setDisabled(type == "TEXT" && type == "EVENT_LIST");

        this.cmps.units.setVisible(type != "TEXT" && type != "EVENT_LIST");
        this.cmps.units.setDisabled(type == "TEXT" && type == "EVENT_LIST");

        this.cmps.orderByColumn.setVisible(type != "TEXT" && type != "EVENT_LIST");
        this.cmps.orderByColumn.setDisabled(type == "TEXT" && type == "EVENT_LIST");

        this.cmps.orderDirection.setVisible(type != "TEXT" && type != "EVENT_LIST");
        this.cmps.orderDirection.setDisabled(type == "TEXT" && type == "EVENT_LIST");

        this.cmps.defaultColumns.setVisible(type === "EVENT_LIST");
        this.cmps.defaultColumns.setDisabled(type !== "EVENT_LIST");

    },

    reloadReports: function () {
        if (Ext.isFunction(this.parentCmp.reloadReports)) {
            this.parentCmp.reloadReports();
        }
        if (this.parentCmp && Ext.isFunction(this.parentCmp.getSettings)) {
            Ext.MessageBox.wait(i18n._("Reloading..."), i18n._("Please wait"));
            this.parentCmp.getSettings(Ext.bind(function () {
                this.parentCmp.clearDirty();
                Ext.MessageBox.hide();
            }, this));
        }
    }
});
