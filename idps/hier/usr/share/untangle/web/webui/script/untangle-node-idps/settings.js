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
     * The generated regular expression used for searching.
     */
    searchRegExp: null,

    defaultStatusText: i18n._('Loading...'),

    /*
     * @public
     * store fields to search
     */
    searchFields:[
        'category',
        'rule'
    ],

    /*
     * @public
     * Minimum number of characters to start search
     */
    searchMinimumCharacters: 2,

    // Component initialization override: adds the top and bottom toolbars and setup headers renderer.
    initComponent: function() {
        var me = this;
        me.bbar = [
            i18n._('Search'),
            {
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
            },{
                xtype: 'statusbar',
                defaultText: me.defaultStatusText,
                name: 'searchStatusBar',
                border: 0
            }
        ];

        me.callParent(arguments);
    },

    // afterRender override: it adds textfield and statusbar reference and start monitoring keydown events in textfield input
    afterRender: function() {
        var me = this;
        me.callParent(arguments);

        me.searchTextField = me.down('textfield[name=searchField]');
        me.searchStatusBar = me.down('statusbar[name=searchStatusBar]');
    },

    afterDataBuild: function(handler){
        var me = this;
        me.callParent(arguments);

        me.searchStatusBar.setStatus({
            text: me.store.count() + ' ' + i18n._('total rules'),
            iconCls: 'x-status-valid'
        });

        me.storeCategories = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.storeClasstypes = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.store.each(
            function( record ){
                var category = record.get("category");
                if( this.storeCategories.find( 'id', category ) == -1 ){
                    this.storeCategories.add( { id: category, value: category } );
                }
                var classtype = record.get("classtype");
                if( this.storeClasstypes.find( 'id', classtype ) == -1 ){
                    this.storeClasstypes.add( { id: classtype, value: classtype } );
                }
            },
            this
        );
        me.rowEditor.down('combo[name=Classtype]').bindStore(me.storeClasstypes);
        me.rowEditor.down('combo[name=Category]').bindStore(me.storeCategories);
    },

    /*
     * @private
     * DEL ASCII code
    */
    searchTagsProtect: '\x0f',

    /*
     * @private 
     * detects regexp reserved word
     */
    searchRegExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
    /**
     * In normal mode it returns the value with protected regexp characters.
     * In regular expression mode it returns the raw value except if the regexp is invalid.
     * @return {String} The value to process or null if the textfield value is blank or invalid.
     * @private
     */
    getSearchValue: function() {
        var me = this,
            value = me.searchTextField.getValue();

        if (value === '') {
            return null;
        }
        value = value.replace(me.searchRegExpProtect, function(m) {
            return '\\' + m;
        });

        var length = value.length,
            resultArray = [me.searchTagsProtect + '*'],
            i = 0,
            c;

        for(; i < length; i++) {
            c = value.charAt(i);
            resultArray.push(c);
            if (c !== '\\') {
                resultArray.push(me.searchTagsProtect + '*');
            }
        }
        return resultArray.join('');
    },

    /**
     * Finds all strings that matches the searched value in each grid cells.
     * @private
     */
    onTextFieldChange: function() {
        var me = this;

        me.store.clearFilter(false);
        me.searchValue = me.getSearchValue();
        if( ( me.searchValue !== null ) &&
            ( me.searchTextField.getValue().length > me.searchMinimumCharacters ) ){

            me.searchRegExp = new RegExp(me.searchValue, 'g' + 'i');

            me.store.filterBy( function( record, id ) {
                me = this;
                for( var i = 0 ; i < me.searchFields.length; i++){
                    if( me.searchRegExp.test( record.get( me.searchFields[i] )) ){
                        return true;
                    }
                }
                return false;
            }, me );

            var count = me.store.count();
            me.searchStatusBar.setStatus({
                text: count ? count + ' ' + i18n._(' matche(s) found') : i18n._('No matches found') ,
                iconCls: 'x-status-valid'
            });
         }else{
            if( ( me.searchValue !== null ) &&
                ( me.searchTextField.getValue().length < ( me.searchMinimumCharacters + 1 ) ) ){
                me.searchStatusBar.setStatus({
                    text: i18n._("(type more than 2 characters)"),
                    iconCls: 'x-status-valid'
                });
            }else{
                me.searchStatusBar.setStatus({
                    text: me.store.count() + ' ' + i18n._('total rules'),
                    iconCls: 'x-status-valid'
                });
            }
         }

         // force textfield focus
         me.searchTextField.focus();
     },

     getPageList: function(useId, useInternalId) {
        this.store.clearFilter(true);
        return this.callSuper( useId, useInternalId );
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
            items: [
                this.gridRules = Ext.create('Ung.RuleEditorGrid', {
                    name: 'Rules',
                    flex: 1,
                    groupField: 'classtype',
                    sortField: 'category',
                    style: "margin-bottom:10px;",
                    settingsCmp: this,
                    title: this.i18n._("Rules"),
                    recordJavaClass: "com.untangle.node.idps.IpsRule",
                    dataProperty: 'rules',
                    paginated: false,
                    columnsDefaultSortable: true,
                    plugins: {
                        ptype: 'bufferedrenderer',
                        trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
                        leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
                    },
                    features: [ 
                        Ext.create('Ext.grid.feature.Grouping',{
                            groupHeaderTpl: '{columnName}: {name} ({rows.length} rule{[values.rows.length > 1 ? "s" : ""]})',
                            startCollapsed: true
                        })            
                    ] ,
                    fields: [{
                        name: 'sid',
                        sortType: 'asInt'
                    },{
                        name: 'category'
                    },{
                        name: 'classtype'
                    },{
                        name: 'msg'
                    },{
                        name: 'rule'
                    },{
                        name: 'log'
                    },{
                        name: 'block'
                    }],
                    emptyRow: {
                        "sid": "0",
                        "log": true,
                        "block": false,
                        "category": "",
                        "classtype": "",
                        "name" : "",
                        "rule": ""
                    },
                    columns: [{
                        header: this.i18n._("Sid"),
                        dataIndex: 'sid',
                        sortable: true,
                        width: 70,
                        editor: null,
                        menuDisabled: false
                    },{
                        header: this.i18n._("Classtype"),
                        dataIndex: 'classtype',
                        sortable: true,
                        width: 100,
                        flex:1,
                        editor: null,
                        menuDisabled: false
                    },{
                        header: this.i18n._("Category"),
                        dataIndex: 'category',
                        sortable: true,
                        width: 100,
                        flex:1,
                        editor: {
                            xtype:'texfield',
                            emptyText: this.i18n._("[enter category]"),
                            allowBlank: false
                        },
                        menuDisabled: false
                    },{
                        header: this.i18n._("Msg"),
                        dataIndex: 'msg',
                        sortable: true,
                        width: 200,
                        flex:3,
                        editor: null,
                        menuDisabled: false
                    },{
                        xtype:'checkcolumn',
                        header: this.i18n._("Log"),
                        dataIndex: 'log',
                        sortable: true,
                        resizable: false,
                        width:55,
                        menuDisabled: false
                    },{
                        xtype:'checkcolumn',
                        header: this.i18n._("Block"),
                        dataIndex: 'block',
                        sortable: true,
                        resizable: false,
                        width:55,
                        menuDisabled: false
                    }],
                    rowEditorInputLines: [{
                        name: "Classtype",
                        dataIndex: "classtype",
                        fieldLabel: this.i18n._("Classtype"),
                        emptyText: this.i18n._("[enter class]"),
                        allowBlank: false,
                        width: 400,
                        xtype: 'combo',
                        queryMode: 'local',
                        valueField: 'id',
                        displayField: 'value'
                  },{
                        name: "Category",
                        fieldLabel: this.i18n._("Category"),
                        dataIndex: "category",
                        emptyText: this.i18n._("[enter category]"),
                        allowBlank: false,
                        width: 400,
                        xtype: 'combo',
                        queryMode: 'local',
                        valueField: 'id',
                        displayField: 'value'
                    },{
                        xtype:'textfield',
                        name: "Name",
                        dataIndex: "msg",
                        fieldLabel: this.i18n._("Msg"),
                        emptyText: this.i18n._("[enter name]"),
                        allowBlank: false,
                        width: 400
                    },{
                        xtype:'textfield',
                        name: "Sid",
                        dataIndex: "sid",
                        fieldLabel: this.i18n._("Sid"),
                        emptyText: this.i18n._("[enter sid]"),
                        allowBlank: false,
                        width: 400
                    },{
                        xtype:'checkbox',
                        name: "Block",
                        dataIndex: "block",
                        fieldLabel: this.i18n._("Block")
                    },{
                        xtype:'checkbox',
                        name: "Log",
                        dataIndex: "log",
                        fieldLabel: this.i18n._("Log")
                    },{
                        xtype:'textfield',
                        name: "Rule",
                        dataIndex: "rule",
                        fieldLabel: this.i18n._("Rule"),
                        emptyText: this.i18n._("[enter rule]"),
                        allowBlank: false,
                        width: 1000
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
                    },{
                        name: 'variable',
                        type: 'string'
                    },{
                        name: 'definition',
                        type: 'string'
                    },{
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
                    },{
                        id: 'definition',
                        header: this.i18n._("pass"),
                        width: 300,
                        dataIndex: 'definition',
                        editor: {
                            xtype:'textfield',
                            emptyText: this.i18n._("[enter definition]"),
                            allowBlank: false
                        }
                    },{
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
                    },{
                        xtype:'textfield',
                        name: "Pass",
                        dataIndex: "definition",
                        fieldLabel: this.i18n._("Pass"),
                        emptyText: this.i18n._("[enter definition]"),
                        allowBlank: false,
                        width: 400
                    },{
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