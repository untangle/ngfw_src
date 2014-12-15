var groupingFeature = Ext.create('Ext.grid.feature.Grouping',{
    groupHeaderTpl: 'Classification: {name} ({rows.length} Item{[values.rows.length > 1 ? "s" : ""]})',
    startCollapsed: true
});

Ext.define('Ung.RuleEditorGrid', {
    extend: 'Ung.EditorGrid',
    requires: [
        'Ext.toolbar.TextItem',
        'Ext.form.field.Checkbox',
        'Ext.form.field.Text',
        'Ext.ux.statusbar.StatusBar'
    ],

    /**
     * @private
     * search value initialization
     */
    searchValue: null,

    /**
     * @private
     * The row indexes where matching strings are found. (used by previous and next buttons)
     */
    indexes: [],

    /**
     * @private
     * The row index of the first search, it could change if next or previous buttons are used.
     */
    currentIndex: null,

    /**
     * @private
     * The generated regular expression used for searching.
     */
    searchRegExp: null,

    /**
     * @private
     * Regular expression mode.
     */
    regExpMode: false,

    /**
     * @cfg {String} matchCls
     * The matched string css classe.
     */
    matchCls: 'x-livesearch-match',

    defaultStatusText: 'Nothing Found',

    // Component initialization override: adds the top and bottom toolbars and setup headers renderer.
    initComponent: function() {
        var me = this;
        me.tbar = ['Search',{
                 xtype: 'textfield',
                 name: 'searchField',
                 hideLabel: true,
                 width: 200,
                 listeners: {
                     change: {
                         fn: me.onTextFieldChange,
                         scope: this,
                         buffer: 100
                     }
                 }
            }, {
                xtype: 'button',
                text: '<',
                tooltip: 'Find Previous Row',
                handler: me.onPreviousClick,
                scope: me
            },{
                xtype: 'button',
                text: '>',
                tooltip: 'Find Next Row',
                handler: me.onNextClick,
                scope: me
            }, '-', {
                xtype: 'checkbox',
                hideLabel: true,
                margin: '0 0 0 4px',
                handler: me.regExpToggle,
                scope: me
            }, 'Regular expression', {
                xtype: 'checkbox',
                hideLabel: true,
                margin: '0 0 0 4px',
                handler: me.caseSensitiveToggle,
                scope: me
            }, 'Case sensitive'];

        me.bbar = Ext.create('Ext.ux.StatusBar', {
            defaultText: me.defaultStatusText,
            name: 'searchStatusBar'
        });

        me.callParent(arguments);
    },

    // afterRender override: it adds textfield and statusbar reference and start monitoring keydown events in textfield input
    afterRender: function() {
        var me = this;
        me.callParent(arguments);
        me.textField = me.down('textfield[name=searchField]');
        me.statusBar = me.down('statusbar[name=searchStatusBar]');
    },
    // detects html tag
    tagsRe: /<[^>]*>/gm,

    // DEL ASCII code
    tagsProtect: '\x0f',

    // detects regexp reserved word
    regExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,

    /**
     * In normal mode it returns the value with protected regexp characters.
     * In regular expression mode it returns the raw value except if the regexp is invalid.
     * @return {String} The value to process or null if the textfield value is blank or invalid.
     * @private
     */
    getSearchValue: function() {
        var me = this,
            value = me.textField.getValue();

        console.log("getSearchValue, text value=" + me.textField.getValue() );

        if (value === '') {
            return null;
        }
        if (!me.regExpMode) {
            value = value.replace(me.regExpProtect, function(m) {
                return '\\' + m;
            });
        } else {
            try {
                new RegExp(value);
            } catch (error) {
                me.statusBar.setStatus({
                    text: error.message,
                    iconCls: 'x-status-error'
                });
                return null;
            }
            // this is stupid
            if (value === '^' || value === '$') {
                return null;
            }
        }

        var length = value.length,
            resultArray = [me.tagsProtect + '*'],
            i = 0,
            c;

        for(; i < length; i++) {
            c = value.charAt(i);
            resultArray.push(c);
            if (c !== '\\') {
                resultArray.push(me.tagsProtect + '*');
            }
        }
        return resultArray.join('');
    },

    /**
     * Finds all strings that matches the searched value in each grid cells.
     * @private
     */
     onTextFieldChange: function() {
         var me = this,
             count = 0;

         me.view.refresh();
         // reset the statusbar
         me.statusBar.setStatus({
             text: me.defaultStatusText,
             iconCls: ''
         });

         me.searchValue = me.getSearchValue();
         me.indexes = [];
         me.currentIndex = null;

         if (me.searchValue !== null) {
//                 me.searchRegExp = new RegExp(me.searchValue, 'g' + (me.caseSensitive ? '' : 'i'));
             me.searchRegExp = new RegExp(me.searchValue, 'g' + 'i');
             console.log("search value=" + me.searchValue);

             var walkedCount = 0;
             me.store.each(function(record, idx) {
                if( Ext.fly(me.view.getNode(idx)) != null ){
                 var td = Ext.fly(me.view.getNode(idx)).down('td'),
                     cells, cell, matches, cellHTML;
                 while(td) {
                     cells = td.query('.x-grid-cell-inner');
                     if( cells.length == 0 ){
                        break;
                     }
                     for( var i = 0; i < cells.length; i++ ){
                        cell = cells[i];
                        matches = cell.innerHTML.match(me.tagsRe);
                        cellHTML = cell.innerHTML.replace(me.tagsRe, me.tagsProtect);

                        // populate indexes array, set currentIndex, and replace wrap matched string in a span
                        cellHTML = cellHTML.replace(me.searchRegExp, function(m) {
                            count += 1;
                            if (Ext.Array.indexOf(me.indexes, idx) === -1) {
                                me.indexes.push(idx);
                            }
                            if (me.currentIndex === null) {
                                me.currentIndex = idx;
                            }
                            return '<span class="' + me.matchCls + '">' + m + '</span>';
                        });
                        // restore protected tags
                        Ext.each(matches, function(match) {
                            cellHTML = cellHTML.replace(me.tagsProtect, match);
                        });
                        walkedCount++;
                        cell.innerHTML = cellHTML;
                    }
                    td = td.next();
                 }
                }
             }, me);
            console.log("search count=" + count + ", walkedCount=" + walkedCount);

             // results found
             if (me.currentIndex !== null) {
                 me.getSelectionModel().select(me.currentIndex);
                 me.statusBar.setStatus({
                     text: count + ' matche(s) found.',
                     iconCls: 'x-status-valid'
                 });
             }
         }

         // no results found
         if (me.currentIndex === null) {
             me.getSelectionModel().deselectAll();
         }

         // force textfield focus
         me.textField.focus();
     },

    /**
     * Selects the previous row containing a match.
     * @private
     */
    onPreviousClick: function() {
        var me = this,
            idx;

        if ((idx = Ext.Array.indexOf(me.indexes, me.currentIndex)) !== -1) {
            me.currentIndex = me.indexes[idx - 1] || me.indexes[me.indexes.length - 1];
            me.getSelectionModel().select(me.currentIndex);
         }
    },

    /**
     * Selects the next row containing a match.
     * @private
     */
    onNextClick: function() {
         var me = this,
             idx;

         if ((idx = Ext.Array.indexOf(me.indexes, me.currentIndex)) !== -1) {
            me.currentIndex = me.indexes[idx + 1] || me.indexes[0];
            me.getSelectionModel().select(me.currentIndex);
         }
    },

    /**
     * Switch to case sensitive mode.
     * @private
     */
    caseSensitiveToggle: function(checkbox, checked) {
        this.caseSensitive = checked;
        this.onTextFieldChange();
    },

    /**
     * Switch to regular expression mode
     * @private
     */
    regExpToggle: function(checkbox, checked) {
        this.regExpMode = checked;
        this.onTextFieldChange();
    }
});

Ext.define('Webui.untangle-node-idps.settings', {
    extend:'Ung.NodeWin',
    statics: {
        preloadSettings: function(node){
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IdpsSettings",
                    arg1: "load",
                    arg2: node.nodeId
                },
                scope: node,
                success: function(response){
                    // console.log("success, response=");
                    // console.log(response);
                    this.openSettings.call(this, Ext.decode( response.responseText ) );
                },
                failure: function(response){
                    // console.log("failure, response=");
                    // console.log(response);
                    this.openSettings.call(this, null );
                }
            });
        }
    },
    panelStatus: null,
    panelRules: null,
    gridRules: null,
    gridVariables: null,
    gridEventLog: null,
    statistics: null,
    // called when the component is rendered
    initComponent: function() {
//            this.statistics = this.getRpcNode().getStatistics();
//        this.buildStatus();
        this.buildRules();
        this.buildEventLog();
        // builds the tab panel with the tabs
//        this.buildTabPanel([this.panelStatus, this.panelRules, this.gridEventLog]);
        this.buildTabPanel([this.panelConfiguration, this.panelRules, this.gridEventLog]);
        this.callParent(arguments);
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            // helpSource: 'intrusion_detection_prevention_status', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            parentId: this.getId(),
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset',
                buttonAlign: 'left'
            },
            items: [{
                title: this.i18n._('Statistics'),
                labelWidth: 230,
                defaults: {
                    xtype: "textfield",
                    disabled: true
                },
                items: [{
                    fieldLabel: this.i18n._('Total Signatures Available'),
                    name: 'Total Signatures Available',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalAvailable
                }, {
                    fieldLabel: this.i18n._('Total Signatures Logging'),
                    name: 'Total Signatures Logging',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalLogging
                }, {
                    fieldLabel: this.i18n._('Total Signatures Blocking'),
                    name: 'Total Signatures Blocking',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalBlocking
                }]
            }, {
                title: this.i18n._('Note'),
                cls: 'description',
                html: Ext.String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."),
                        rpc.companyName)
            }]
        });
    },

    // Rules Panel
    buildRules: function() {
        this.panelRules = Ext.create('Ext.panel.Panel',{
            name: 'panelRules',
            // helpSource: 'intrusion_dection_prevention_rules', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            parentId: this.getId(),
            title: this.i18n._('Rules'),
            border: false,
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
//                items: [this.gridRules = Ext.create('Ung.EditorGrid', {
            items: [this.gridRules = Ext.create('Ung.RuleEditorGrid', {
                flex: 1,
            features: [groupingFeature],
            groupField: 'classification',
                style: "margin-bottom:10px;",
                name: 'Rules',
                settingsCmp: this,
                plugins: {
                    ptype: 'bufferedrenderer',
                    trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
                    leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
                },
                emptyRow: {
                    "category": "",
                    "name": "",
                    "text": "",
                    "sid": "0",
                    "live": true,
                    "log": true,
                    "description": ""
                },
                title: this.i18n._("Rules"),
                recordJavaClass: "com.untangle.node.idps.IpsRule",
                dataProperty: 'rules',
                paginated: false,
                fields: [{
                    name: 'id'
                }, {
                    name: 'text'
                }, {
                    name: 'sid'
                }, {
                    name: 'name'
                }, {
                    name: 'category',
                    type: 'string'
                }, {
                    name: 'classification'
                }, {
                    name: 'URL'
                }, {
                    name: 'live'
                }, {
                    name: 'log'
                }, {
                    name: 'description',
                    type: 'string'
                }],
                columns: [{
                    header: this.i18n._("Id"),
                    width: 70,
                    dataIndex: 'sid',
                    editor: null
                },{
                    xtype:'checkcolumn',
                    header: this.i18n._("Log"),
                    dataIndex: 'log',
                    resizable: false,
                    width:55
                },{
                    xtype:'checkcolumn',
                    header: this.i18n._("Block"),
                    dataIndex: 'live',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Category"),
                    width: 180,
                    dataIndex: 'category',
                    editor: {
                        xtype:'texfield',
                        emptyText: this.i18n._("[enter category]"),
                        allowBlank: false
                    }
                },{
                    header: this.i18n._("Classification"),
                    width: 200,
                    dataIndex: 'classification',
                    flex:1,
                    editor: null
                },{
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex:1,
                    editor: null
                }, {
                    header: this.i18n._("Info"),
                    width: 70,
                    dataIndex: 'URL',
                    editor: null,
                    sortable: false,
                    renderer: function(value) {
                        return (value == null || value.length == 0) ? "no info": "<a href='" + value + "' target='_blank'>info</a>";
                    }
                }],
                sortField: 'classification',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "Category",
                    dataIndex: "category",
                    fieldLabel: this.i18n._("Category"),
                    emptyText: this.i18n._("[enter category]"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'textfield',
                    name: "Signature",
                    dataIndex: "text",
                    fieldLabel: this.i18n._("Signature"),
                    emptyText: this.i18n._("[enter signature]"),
                    allowBlank: false,
                    width: 450
                },
                {
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Name"),
                    emptyText: this.i18n._("[enter name]"),
                    allowBlank: false,
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "SID",
                    dataIndex: "sid",
                    fieldLabel: this.i18n._("SID"),
                    allowBlank: false,
                    width: 150
                },
                {
                    xtype:'checkbox',
                    name: "Block",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Block")
                },
                {
                    xtype:'checkbox',
                    name: "Log",
                    dataIndex: "log",
                    fieldLabel: this.i18n._("Log")
                },
                {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    emptyText: this.i18n._("[enter description]"),
                    allowBlank: false,
                    width: 500
                }]
            }),
            this.gridVariables = Ext.create('Ung.EditorGrid', {
                flex: 1,
                name: 'Variables',
                settingsCmp: this,
                emptyRow: {
                    "variable": "",
                    "definition": "",
                    "description": ""
                },
                title: this.i18n._("Variables"),
                recordJavaClass: "com.untangle.node.idps.IpsVariable",
                dataProperty: 'variables',
                fields: [{
                    name: 'id'
                }, {
                    name: 'variable',
                    type: 'string'
                }, {
                    name: 'definition',
                    type: 'string'
                }, {
                    name: 'description',
                    type: 'string'
                }],
                columns: [{
                    header: this.i18n._("name"),
                    width: 170,
                    dataIndex: 'variable',
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter name]"),
                        allowBlank: false
                        }
                    },
                    {
                    id: 'definition',
                    header: this.i18n._("pass"),
                    width: 300,
                    dataIndex: 'definition',
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter definition]"),
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("description"),
                    width: 300,
                    dataIndex: 'description',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter description]"),
                        allowBlank: false
                    }
                }],
                sortField: 'variable',
                columnsDefaultSortable: true,
                rowEditorInputLines: [{
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "variable",
                    fieldLabel: this.i18n._("Name"),
                    emptyText: this.i18n._("[enter name]"),
                    allowBlank: false,
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "Pass",
                    dataIndex: "definition",
                    fieldLabel: this.i18n._("Pass"),
                    emptyText: this.i18n._("[enter definition]"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    emptyText: this.i18n._("[enter description]"),
                    allowBlank: false,
                    width: 400
                }]
            })
        ]});
    },
    // Event Log
    buildEventLog: function() {
        // this.gridEventLog = Ung.CustomEventLog.buildSessionEventLog (this, 'EventLog', i18n._('Event Log'),
        //     'intrusion_detection_prevention_event_log',
        //     ['time_stamp','sig_id', 'gen_id', 'class_id', 'source_addr', 'source_port', 'dest_addr', 'dest_port', 'protocol', 'blocked', 'category', 'classtype', 'description' ],
        //     this.getRpcNode().getEventQueries);
        // buildSessionEventLog: function(settingsCmpParam, nameParam, titleParam, helpSourceParam, visibleColumnsParam, eventQueriesFnParam) {
        var settingsCmpParam = this;
        var nameParam = 'EventLog';
        var titleParam = i18n._('Event Log');
        var helpSourceParam = 'intrusion_detection_prevention_event_log';
        var visibleColumnsParam = ['time_stamp','sig_id', 'source_addr', 'source_port', 'dest_addr', 'dest_port', 'protocol', 'blocked', 'category', 'classtype', 'description' ];
        var eventQueriesFnParam = this.getRpcNode().getEventQueries;
        this.gridEventLog = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'sig_id',
                sortType: 'asInt'
            }, {
                name: 'gen_id',
                sortType: 'asInt'
            }, {
                name: 'class_id',
                sortType: 'asInt'
            }, {
                name: 'source_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'source_port',
                sortType: 'asInt'
            }, {
                name: 'dest_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'dest_port',
                sortType: 'asInt'
            }, {
                name: 'protocol',
                sortType: 'asInt'
            }, {
                name: 'blocked',
                type: 'boolean'
            }, {
                name: 'category',
                type: 'string'
            }, {
                name: 'classtype',
                type: 'string'
            }, {
                name: 'description',
                type: 'string'
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('sig_id') < 0,
                header: i18n._("Signature ID"),
                width: 70,
                sortable: true,
                dataIndex: 'sig_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('gen_id') < 0,
                header: i18n._("Generator ID"),
                width: 70,
                sortable: true,
                dataIndex: 'gen_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('class_id') < 0,
                header: i18n._("Class ID"),
                width: 70,
                sortable: true,
                dataIndex: 'class_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('source_addr') < 0,
                header: i18n._("Source Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'source_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('source_port') < 0,
                header: i18n._("Source port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'source_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('dest_addr') < 0,
                header: i18n._("Destination Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'dest_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('dest_port') < 0,
                header: i18n._("Destination port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'dest_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('protocol') < 0,
                header: i18n._("Protocol"),
                width: 70,
                sortable: true,
                dataIndex: 'protocol',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('blocked') < 0,
                header: i18n._("Blocked"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('category') < 0,
                header: i18n._("Category"),
                width: 200,
                sortable: true,
                dataIndex: 'category'
            }, {
                hidden: visibleColumnsParam.indexOf('classtype') < 0,
                header: i18n._("Classtype"),
                width: 200,
                sortable: true,
                dataIndex: 'classtype'
            }, {
                hidden: visibleColumnsParam.indexOf('description') < 0,
                header: i18n._("Description"),
                width: 200,
                sortable: true,
                dataIndex: 'description'
            }]
        });
    },
    applyAction: function(){
        this.callParent();
    },
    beforeSave: function(isApply,handler) {
        this.settings.rules.list = null;
        this.settings.variables.list = null;
        this.settings.rules.list = this.gridRules.getPageList();
        this.settings.variables.list = this.gridVariables.getPageList();
        handler.call(this, isApply);
    },
    save: function(isApply) {
        // pop up a window        
        Ext.Ajax.request({
            url: "/webui/download",
            jsonData: this.settings,
            method: 'POST',
            params: {
                type: "IdpsSettings",
                arg1: "save",
                arg2: this.tid
            },
            scope: this,
            success: function(response){
                Ext.MessageBox.hide();

                if (!isApply) {
                    this.closeWindow();
                    return;
                }else{
                    this.clearDirty();
                }
            },
            failure: function(response){
                Ext.MessageBox.hide();
                Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
            }
        });
    },        
    afterSave: function() {
//            this.statistics = this.getRpcNode().getStatistics();
    }
});
//# sourceURL=ips-settings.js