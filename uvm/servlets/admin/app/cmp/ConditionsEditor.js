Ext.define('Ung.cmp.ConditionsEditor', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.conditionseditor',

    controller: 'conditionseditorcontroller',

    model: 'Ung.model.Condition',
    comparatorField: 'invert',
    valueField: 'value',

    margin: '0 -10 0 -10',

    actions: {
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Condition'.t(),
            iconCls: 'fa fa-plus'
        }
    },

    isFormField: true,
        
    conditionText: 'If all of the following conditions are met:'.t(),
    actionText: 'Perform the following action(s):'.t(),

    border: 0,
    items:[{
        xtype: 'component',
        name: 'conditionLabel',
        padding: '10 0 0 0',
        tpl: '<strong>{conditionText}</strong>'
    },{
        xtype: 'grid',
        // !!! bad name
        name: 'stuff',
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        layout: 'fit',
        padding: '10 0',
        tbar: ['@addCondition'],
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Conditions! Add from the menu...</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Type'.t(),
            menuDisabled: true,
            dataIndex: 'conditionType',
            align: 'right',
            width: Renderer.uriWidth,
            renderer: 'conditionRenderer'
            // Option to make this comparator
            // bind: '{record.comparator}',
            // onWidgetAttach: 'comparatorWidgetAttach'
        }, {
            xtype: 'widgetcolumn',
            widgetType: 'comparator',
            menuDisabled: true,
            width: Renderer.conditionsWidth,
            resizable: false,
            cellWrap: true,
            sortable: false,
            widget: {
                xtype: 'container',
                layout: 'fit',
                // bind: '{record.invert}',
            },
            onWidgetAttach: 'onWidgetAttach',
        }, {
            // DO NOT use flex here.  See onAfterGridLayout() in the controller.
            header: 'Value'.t(),
            widgetType: 'value',
            xtype: 'widgetcolumn',
            cellWrap: true,
            menuDisabled: true,
            sortable: false,
            widget: {
                xtype: 'container',
                layout: 'fit',
                // bind: '{record.value}'
            },
            onWidgetAttach: 'onWidgetAttach',
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            sortable: false,
            width: Renderer.iconWidth,
            align: 'center',
            iconCls: 'fa fa-minus-circle fa-red',
            tdCls: 'action-cell-cond',
            handler: 'removeCondition'
        }]
    },{
        xtype: 'component',
        name: 'actionLabel',
        padding: '0 0 10 0',
        tpl: '<strong>{actionText}</strong>'
    }],

    constructor: function(config) {
        var me = this;
        var bind = config.bind;
        var model = config.model;
        if(bind){
            me.recordName = bind.substring(1,bind.length - 1);
            me.items.forEach(function(item){
                if(item.xtype == 'grid'){
                    Ext.apply(item, {
                        bind: {
                            store: {
                                model: model,
                                data: '{' + me.recordName +'.list}'
                            }
                        }
                    });
                    if(item.columns){
                        item.columns.forEach( function(column){
                            if(column.widgetType == 'comparator'){
                                column.widget.bind = '{record.' + me.comparatorField + '}';
                            }else if(column.widgetType == 'value'){
                                column.widget.bind = '{record.' + me.valueField + '}';
                            }
                        });
                    }
                }
            });
            delete config.bind;
        }
        me.callParent([config]);
    },

    validate: function(){
        return this.getController().validate();
    },
    isValid: function(){
        return this.getController().validate();
    },

    statics:{
        /**
         * Nearly all rule conditions use the same condition.  Most that don't use a subset.
         * Define what you want by the name values such as:
         *     buildConditions('DST_ADDR', 'DST_PORT', ...)
         * 
         * If you want to customize, specify as an object such as:
         *     buildConditions( {
         *         name: 'DST_LOCAL', 
         *         visibile: false
         *     }, ...)
         * 
         * If you want to pick from this set but add a new condition that won't be used anywhere
         *  else, specify the displayName and type such as:
         *     buildConditions( {
         *         name: 'NEW_CONDITION',
         *         displayName: 'New condition'.t(),
         *         type: 'textField'
         *     }, ...)
         *
         * If your condition set doesn't use any of these and is only used in one module,
         * just define those as a static list without calling this method such as:
         * conditions: [{
         *     name: 'CONDITION_ONLY_HERE',
         *     displayName: 'Only Here Condition'.t(),
         *     type: 'textField'
         * }]
         * 
         * @return {[type]} [description]
         */
        buildConditions: function(){
            var conditions = [];

            var found = false;
            for(var i = 0; i < arguments.length; i++){
                var argument = typeof(arguments[i]) == 'object' ? arguments[i] : {name: arguments[i]};

                found = false;
                Ung.cmp.ConditionsEditor.conditions.forEach( function(condition){
                    if(condition.name == argument.name){
                        var newCondition = Ext.clone(condition);
                        Ext.merge(newCondition, argument);
                        conditions.push(newCondition);
                        found = true;
                    }
                });
                if(!found){
                    if('name' in argument && 'displayName' in argument && 'type' in argument ){
                        conditions.push(argument);
                    }else{
                        console.log("not found");
                        console.log(argument);
                    }
                }
            }
            return conditions;
        },

        // If not specified, visible is treated as true.
        conditions: [{
            name:"DST_LOCAL",
            displayName: "Destined Local".t(),
            type: "boolean"
        },{
            name:"DST_ADDR",
            displayName: "Destination Address".t(),
            type: 'textfield',
            vtype:"ipMatcher"
        },{
            name:"DST_PORT",
            displayName: "Destination Port".t(),
            type: 'textfield',
            vtype:"portMatcher"
        },{
            name:"DST_INTF",
            displayName: "Destination Interface".t(),
            type: 'checkboxgroup',
            values: Util.getInterfaceList(true, true),
        },{
            name:"SRC_MAC",
            displayName: "Source MAC".t(), 
            type: 'textfield'
        },{
            name:"PROTOCOL",
            displayName: "Protocol".t(),
            type: 'checkboxgroup',
            values: [[
                "TCP","TCP"
            ],[
                "UDP","UDP"
            ],[
                "ICMP","ICMP"
            ],[
                "GRE","GRE"
            ],[
                "ESP","ESP"
            ],[
                "AH","AH"
            ],[
                "SCTP","SCTP"
            ],[
                "OSPF","OSPF"
            ]]
        },{
            name:"SRC_INTF",
            displayName: "Source Interface".t(),
            type: 'checkboxgroup',
            values: Util.getInterfaceList(true, true),
        },{
            name:"SRC_ADDR",
            displayName: "Source Address".t(),
            type: 'textfield',
            vtype:"ipMatcher"
        },{
            name:"SRC_PORT",
            displayName: "Source Port".t(),
            type: 'textfield',
            vtype:"portMatcher",
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:"CLIENT_TAGGED",
            displayName: 'Client Tagged'.t(),
            type: 'textfield'
        },{
            name:"SERVER_TAGGED",
            displayName: 'Server Tagged'.t(),
            type: 'textfield'
        },{
            name:'TAGGED',
            displayName: 'Tagged'.t(),
            type: 'textfield'
        },{
            name:'USERNAME',
            displayName: 'Username'.t(),
            type: 'userfield'
        },{
            name:'HOST_HOSTNAME',
            displayName: 'Host Hostname'.t(),
            type: 'textfield'
        },{
            name:'CLIENT_HOSTNAME',
            displayName: 'Client Hostname'.t(),
            type: 'textfield',
            visible: false
        },{
            name:'SERVER_HOSTNAME',
            displayName: 'Server Hostname'.t(),
            type: 'textfield',
            visible: false
        },{
            name:'SRC_MAC',
            displayName: 'Client MAC Address'.t(),
            type: 'textfield'
        },{
            name:'DST_MAC',
            displayName: 'Server MAC Address'.t(),
            type: 'textfield'
        },{
            name:'CLIENT_MAC_VENDOR',
            displayName: 'Client MAC Vendor'.t(),
            type: 'textfield'
        },{
            name:'SERVER_MAC_VENDOR',
            displayName: 'Server MAC Vendor'.t(),
            type: 'textfield'
        },{
            name:'CLIENT_IN_PENALTY_BOX',
            displayName: 'Client in Penalty Box'.t(),
            type: 'boolean',
            visible: false
        },{
            name:'SERVER_IN_PENALTY_BOX',
            displayName: 'Server in Penalty Box'.t(),
            type: 'boolean',
            visible: false
        },{
            name:'HOST_HAS_NO_QUOTA',
            displayName: 'Host has no Quota'.t(),
            type: 'boolean'
        },{
            name:'USER_HAS_NO_QUOTA',
            displayName: 'User has no Quota'.t(),
            type: 'boolean'
        },{
            name:'CLIENT_HAS_NO_QUOTA',
            displayName: 'Client has no Quota'.t(),
            type: 'boolean',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'SERVER_HAS_NO_QUOTA',
            displayName: 'Server has no Quota'.t(),
            type: 'boolean',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'HOST_QUOTA_EXCEEDED',
            displayName: 'Host has exceeded Quota'.t(),
            type: 'boolean'
        },{
            name:'USER_QUOTA_EXCEEDED',
            displayName: 'User has exceeded Quota'.t(),
            type: 'boolean'
        },{
            name:'CLIENT_QUOTA_EXCEEDED',
            displayName: 'Client has exceeded Quota'.t(),
            type: 'boolean',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'SERVER_QUOTA_EXCEEDED',
            displayName: 'Server has exceeded Quota'.t(),
            type: 'boolean',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'HOST_QUOTA_ATTAINMENT',
            displayName: 'Host Quota Attainment'.t(),
            type: 'textfield'
        },{
            name:'USER_QUOTA_ATTAINMENT',
            displayName: 'User Quota Attainment'.t(),
            type: 'textfield'
        },{
            name:'CLIENT_QUOTA_ATTAINMENT',
            displayName: 'Client Quota Attainment'.t(),
            type: 'textfield',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'SERVER_QUOTA_ATTAINMENT',
            displayName: 'Server Quota Attainment'.t(),
            type: 'textfield',
            visible: Rpc.directData('rpc.isExpertMode')
        },{
            name:'HOST_ENTITLED',
            displayName: 'Host Entitled'.t(), 
            type: 'boolean'
        },{
            name:'HTTP_HOST',
            displayName: 'HTTP: Hostname'.t(),
            type: 'textfield'
        },{
            name:'HTTP_REFERER',
            displayName: 'HTTP: Referer'.t(),
            type: 'textfield'
        },{
            name:'HTTP_URI', 
            displayName: 'HTTP: URI'.t(),
            type: 'textfield'
        },{
            name:'HTTP_URL',
            displayName: 'HTTP: URL'.t(),
            type: 'textfield'
        },{
            name:'HTTP_CONTENT_TYPE',
            displayName: 'HTTP: Content Type'.t(),
            type: 'textfield'
        },{
            name:'HTTP_CONTENT_LENGTH',
            displayName: 'HTTP: Content Length'.t(),
            type: 'textfield'
        },{
            name:'HTTP_REQUEST_METHOD',
            displayName: 'HTTP: Request Method'.t(),
            type: 'textfield'
        },{
            name:'HTTP_REQUEST_FILE_PATH',
            displayName: 'HTTP: Request File Path'.t(),
            type: 'textfield'
        },{
            name:'HTTP_REQUEST_FILE_NAME',
            displayName: 'HTTP: Request File Name'.t(),
            type: 'textfield'
        },{
            name:'HTTP_REQUEST_FILE_EXTENSION',
            displayName: 'HTTP: Request File Extension'.t(),
            type: 'textfield'
        },{
            name:'HTTP_RESPONSE_FILE_NAME',
            displayName: 'HTTP: Response File Name'.t(),
            type: 'textfield'
        },{
            name:'HTTP_RESPONSE_FILE_EXTENSION',
            displayName: 'HTTP: Response File Extension'.t(),
            type: 'textfield'
        },{
            name:'HTTP_USER_AGENT',
            displayName: 'HTTP: Client User Agent'.t(),
            type: 'textfield'
        },{
            name:'HTTP_USER_AGENT_OS',
            displayName: 'HTTP: Client User OS'.t(), 
            type: 'textfield', 
            visible: false
        },{
            name:'APPLICATION_CONTROL_APPLICATION',
            displayName: 'Application Control: Application'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_CATEGORY', 
            displayName: 'Application Control: Application Category'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_PROTOCHAIN',
            displayName: 'Application Control: ProtoChain'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_DETAIL',
            displayName: 'Application Control: Detail'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_CONFIDENCE',
            displayName: 'Application Control: Confidence'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_PRODUCTIVITY',
            displayName: 'Application Control: Productivity'.t(),
            type: 'textfield'
        },{
            name:'APPLICATION_CONTROL_RISK',
            displayName: 'Application Control: Risk'.t(),
            type: 'textfield'
        },{
            name:'PROTOCOL_CONTROL_SIGNATURE',
            displayName: 'Application Control Lite: Signature'.t(),
            type: 'textfield'
        },{
            name:'PROTOCOL_CONTROL_CATEGORY',
            displayName: 'Application Control Lite: Category'.t(),
            type: 'textfield'
        },{
            name:'PROTOCOL_CONTROL_DESCRIPTION',
            displayName: 'Application Control Lite: Description'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_CATEGORY',
            displayName: 'Web Filter: Category'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_CATEGORY_DESCRIPTION',
            displayName: 'Web Filter: Category Description'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_FLAGGED',
            displayName: 'Web Filter: Website is Flagged'.t(),
            type: 'boolean'
        },{
            name:'WEB_FILTER_REQUEST_METHOD',
            displayName: 'Web Filter: Request Method'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_REQUEST_FILE_PATH',
            displayName: 'Web Filter: Request File Path'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_REQUEST_FILE_NAME',
            displayName: 'Web Filter: Request File Name'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_REQUEST_FILE_EXTENSION',
            displayName: 'Web Filter: Request File Extension'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_RESPONSE_CONTENT_TYPE',
            displayName: 'Web Filter: Content Type'.t(),
            type: 'textfield'
        },{
            name:'WEB_FILTER_RESPONSE_FILE_NAME',
            displayName: 'Web Filter: Response File Name'.t(),
            type: 'textfield',
        },{
            name:'WEB_FILTER_RESPONSE_FILE_EXTENSION',
            displayName: 'Web Filter: Response File Extension'.t(),
            type: 'textfield'
        },{
            name:'DIRECTORY_CONNECTOR_GROUP',
            displayName: 'Directory Connector: User in Group'.t(),
            type: 'directorygroupfield'
        },{
            name:'DIRECTORY_CONNECTOR_DOMAIN',
            displayName: 'Directory Connector: User in Domain'.t(),
            type: 'directorydomainfield'
        },{
            name:'CLIENT_COUNTRY',
            displayName: 'Client Country'.t(),
            type: 'countryfield'
        },{
            name:'SERVER_COUNTRY',
            displayName: 'Server Country'.t(),
            type: 'countryfield'
        },{
            name:'SSL_INSPECTOR_SNI_HOSTNAME',
            displayName: 'SSL Inspector: SNI Host Name'.t(),
            type: 'textfield'
        },{
            name:'SSL_INSPECTOR_SUBJECT_DN',
            displayName: 'SSL Inspector: Certificate Subject'.t(),
            type: 'textfield'
        },{
            name:'SSL_INSPECTOR_ISSUER_DN',
            displayName: 'SSL Inspector: Certificate Issuer'.t(),
            type: 'textfield'
        }]
    }
});
