Ext.syncRequire('Ung.common.threatprevention');
Ext.define('TableConfig', {
    alternateClassName: 'TableConfig',
    singleton: true,

    tableConfig: {},

    initialized: false,
    initialize: function() {
        /**
         * generate the tables fields, columns configurations used for reports grids
         * this replaces the older TableConfig.tableConfig definitions
         */
        Ext.Object.each(Map.tables, function (table, fields) {
            var tFields = [], tColumns = [];

            Ext.Array.each(fields, function (key) {
                var field = Map.fields[key].fld;
                var column = Map.fields[key].col;

                column.dataIndex = column.dataIndex || key;
                if (!column.filter) { column.filter = Rndr.filters.string; }
                tColumns.push(column);

                field.name = field.name || key;
                tFields.push(field);
            });

            TableConfig.tableConfig[table] = {
                fields: tFields,
                columns: tColumns
            };
        });

        if(this.initialized){
            return;
        }
        var me = this;

        Ext.Object.each(Ung.common.TableConfig, function(key, value){
            Ung.common.TableConfig[key].initialize(me);
        });
        this.initialized = true;
    },

    getConfig: function(tableName) {
        if(TableConfig.validated == false){
            TableConfig.validate();
        }
        return TableConfig.tableConfig[tableName];
    },

    checkHealth: function() {
        if(!rpc.reportsManager) {
            console.info('Reports not installed!');
            return;
        }
        var i, table, column, systemColumns, systemColumnsMap, tableConfigColumns, tableConfigColumnsMap;
        var systemTables = rpc.reportsManager.getTables();
        var systemTablesMap={};
        var missingTables = [];
        for(i=0; i<systemTables.length;i++) {
            systemTablesMap[systemTables[i]] = true;

            if(!this.tableConfig[systemTables[i]]) {

                // ignore 'totals' tables (from old reports and will be deprecated soon)
                if ( systemTables[i].indexOf('totals') !== -1 ) {
                    continue;
                }
                // ignore 'mail_msgs' table (will be deprecated soon)
                if ( systemTables[i].indexOf('mail_msgs') !== -1 ) {
                    continue;
                }
                missingTables.push(systemTables[i]);
            }
        }
        if(missingTables.length>0) {
            console.log('Warning: Missing tables: ' + missingTables.join(', '));
        }
        var extraTables = [];
        for (table in this.tableConfig) {
            if (this.tableConfig.hasOwnProperty(table)) {
                if(!systemTablesMap[table]) {
                    extraTables.push(table);
                }
            }
        }
        if (extraTables.length > 0) {
            console.log('Warning: Extra tables: ' + extraTables.join(', '));
        }

        for (table in this.tableConfig) {
            tableConfigColumns = this.tableConfig[table].columns;
            if(systemTablesMap[table]) {
                systemColumns = rpc.reportsManager.getColumnsForTable(table);
                systemColumnsMap = {};
                tableConfigColumnsMap = {};
                for(i=0;i<tableConfigColumns.length; i++) {
                    tableConfigColumnsMap[tableConfigColumns[i].dataIndex] = tableConfigColumns[i];
                }
                var missingColumns = [];
                for(i=0;i<systemColumns.length; i++) {
                    systemColumnsMap[systemColumns[i]] = true;
                    var columnConfig = tableConfigColumnsMap[systemColumns[i]];
                    if ( columnConfig === null ) {
                        missingColumns.push(systemColumns[i]);
                    } else {
                        if (! columnConfig.width ) {
                            console.log('Warning: Table "' + table + '" Columns: "' + columnConfig.dataIndex + '" missing width');
                        }
                    }
                }
                if (missingColumns.length > 0) {
                    console.log('Warning: Table "' + table + '" Missing columns: ' + missingColumns.join(', '));
                }

                var extraColumns = [];
                for (column in tableConfigColumnsMap) {
                    if (!systemColumnsMap[column]) {
                        extraColumns.push(column);
                    }
                }
                if (extraColumns.length > 0) {
                    console.log('Warning: Table "' + table + '" Extra columns: ' + extraColumns.join(', '));
                }

            }
        }

    },

    getColumnsForTable: function(table, store) {
        if(table !== null) {
            var tableConfig = this.getConfig(table);
            var columns = [], col;
            if(tableConfig !== null && Ext.isArray(tableConfig.columns)) {
                for(var i = 0; i<tableConfig.columns.length; i++) {
                    col = tableConfig.columns[i];
                    var name = col.header;
                    columns.push({
                        dataIndex: col.dataIndex,
                        header: name
                    });
                }
            }

            store.loadData(columns);
        }
    },

    getColumnHumanReadableName: function(columnName) {
        if(!this.columnsHumanReadableNames) {
            this.columnsHumanReadableNames = {};
            if(!this.tableConfig) {
                this.buildTableConfig();
            }
            var i, table, columns, dataIndex;
            for (table in this.tableConfig) {
                columns = this.tableConfig[table].columns;
                for(i=0; i<columns.length; i++) {
                    dataIndex = columns[i].dataIndex;
                    if(dataIndex && !this.columnsHumanReadableNames[dataIndex]) {
                        this.columnsHumanReadableNames[dataIndex] = columns[i].header;
                    }
                }
            }
        }
        if(!columnName) {
            columnName = '';
        }
        var readableName = this.columnsHumanReadableNames[columnName];
        return readableName !== null ? readableName : columnName.replace(/_/g,' ');
    },

    // new methods .........
    generate: function (table) {
        var checkboxes = [], comboItems = [];
        var tableConfig = this.tableConfig[table];

        if (!tableConfig) {
            console.log('Table not found!');
            return;
        }

        // generate checkboxes and menu
        Ext.Array.each(tableConfig.columns, function (column) {
            checkboxes.push({ boxLabel: column.header, inputValue: column.dataIndex, name: 'cbGroup' });
            comboItems.push({
                text: column.header,
                value: column.dataIndex
            });
        });
        tableConfig.checkboxes = checkboxes;
        tableConfig.comboItems = comboItems;

        return tableConfig;
    },

    validated: false,
    validate: function(){
        for(var table in TableConfig.tableConfig){
            if(table == 'syslog'){
                continue;
            }
            TableConfig.tableConfig[table].fields.forEach( function( field ){
                // if(!field.type &&
                //     ( !field.sortType ||
                //       field.sortType != 'asTimestamp' ) ){
                //     console.log(table + ": field=" + field.name + ", missing type" );
                // }
            });
            TableConfig.tableConfig[table].columns.forEach( function( column ){
                if(column.width === undefined){
                    console.log(table + ":" + column.header + ", no width");
                }
                if(!column.filter &&
                    ( !column.xtype || column.xtype != "actioncolumn") ){
                    console.log(table + ": column=" + column.header + ", no filter");
                }
            });
        }
        TableConfig.validated = true;
    },

    // end new methods

    // checks if a table contains at least one of the columns passed as param
    containsColumns: function (table, columns) {
        var contains = false, fields = TableConfig.tableConfig[table].fields;
        Ext.Array.each(fields, function (field) {
            Ext.Array.each(columns, function (col) {
                if (field.name === col) {
                    contains = true;
                }
            });
        });
        return contains;
    },

    getTableField: function(table, name){
        var tableField = null;
        if(TableConfig.tableConfig[table] &&
            TableConfig.tableConfig[table]['fields']){
            TableConfig.tableConfig[table]['fields'].forEach( function(field){
                if(field['name'] == name){
                    tableField = field;
                }
            });
        }
        return tableField;
    },

    /**
     * Add table fields to a table's field object.
     * @param String identifier of table.
     * @param Object (or Array of objects) of fields to add.
     */
    setTableField: function(table, field){
        Ext.Array.push(
            TableConfig.tableConfig[table]['fields'],
            field
        );
    },

    getFromType: function(table, name){
        var fromType = null;
        if(TableConfig.tableConfig[table] &&
            TableConfig.tableConfig[table]['fields']){
            TableConfig.tableConfig[table]['fields'].forEach( function(field){
                if( ( field['name'] == name ) &&
                    field['fromType'] &&
                    TableConfig.fromTypes[field['fromType']]){
                    fromType = TableConfig.fromTypes[field['fromType']];
                }
            });
        }
        return fromType;
    },

    /**
     * Set from type for fields.
     * @param Object of fromTypes to add.
     */
    setFromType: function(fromTypes){
        Ext.Object.each( fromTypes, function(key, value){
            TableConfig.fromTypes[key] = value;
        });
    },

    getTableColumn: function(table, name){
        var tableColumn = null;
        if(table == null){
            table = TableConfig.getFirstTableFromField(name);
        }
        if(TableConfig.tableConfig[table] &&
            TableConfig.tableConfig[table]['columns']){
            TableConfig.tableConfig[table]['columns'].forEach( function(column){
                if(column['dataIndex'] == name){
                    tableColumn = column;
                }
            });
        }
        return tableColumn;
    },

    /**
     * Add table columns to a table's column list.
     * @param String identifier of table.
     * @param Object (or Array of objects) of columns to add.
     */
    setTableColumn: function(table, column){
        Ext.Array.push(
            TableConfig.tableConfig[table]['columns'],
            column
        );
    },

    setTableListener: function(table, listener){
        if(!("listeners" in TableConfig.tableConfig[table])){
            TableConfig.tableConfig[table]["listeners"] = {};
        }
        Ext.Object.each( listener, function(key, value){
            TableConfig.tableConfig[table]["listeners"][key] = value;
        });
    },

    getFirstTableFromField: function(fieldName){
        var table = null;
        Ext.Object.each(TableConfig.tableConfig, function(tableName, tableValues){
            tableValues['fields'].forEach(function(field){
                if(field['name'] == fieldName){
                    table = tableName;
                    return false;
                }
            });
            if(table != null){
                return false;
            }
        });
        return table;
    },

    getDisplayValue: function(value, table, field, record){
        if(arguments[6]){
            var column = arguments[6].getGridColumns()[arguments[4]];
            table = column.table;
            field = column.field;
        }
        var tableField = TableConfig.getTableField(table, field);
        var tableColumn = TableConfig.getTableColumn(table, field);
        if(tableField && tableField['convert']){
            value = tableField['convert'](value, null, record);
        }
        if(tableColumn && tableColumn['renderer']){
            value = tableColumn['renderer'](value, null, record);
        }
        return value;
    },

    getValues: function( table, field, record){
        var values = [];
        var tableColumn = TableConfig.getTableColumn(table, field);
        if(tableColumn && tableColumn['renderer']){
            var rendererValues = tableColumn['renderer'](Renderer.listKey, null, record);
            if(Array.isArray(rendererValues)){
                values = rendererValues;
            }else if(typeof rendererValues === 'object'){
                for(var key in rendererValues){
                    if(rendererValues.hasOwnProperty(key)){
                        values.push([String(key), rendererValues[key]]);
                    }
                }
            }
        }
        return values;
    },

    fromTypes: {},

});
TableConfig.initialize();
