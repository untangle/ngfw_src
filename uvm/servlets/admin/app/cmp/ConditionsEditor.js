Ext.define('Ung.cmp.ConditionsEditor', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.conditionseditor',

    controller: 'conditionseditorcontroller',

    model: 'Ung.model.Condition',

    /**
     * The fields object must always be defined so the grid
     * can access it.
     */
    fields: {
        type: 'conditionType',
        comparator: 'invert',
        value: 'value',
    },
    javaClassValue: '',

    conditions: [],
    conditionsOrder: [],

    allowEmpty: true,

    margin: '0 -10 0 -10',

    actions: {
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Condition'.t(),
            iconCls: 'fa fa-plus'
        }
    },

    isFormField: true,

    text:{
        condition: 'If all of the following conditions are met:'.t(),
        action: 'Perform the following action(s):'.t(),
    },

    border: 0,
    items:[{
        xtype: 'component',
        name: 'conditionLabel',
        padding: '10 0 0 0',
        tpl: '<strong>{text.condition}</strong>'
    },{
        xtype: 'grid',
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        layout: 'fit',
        padding: '10 0',
        tbar: ['@addCondition'],
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa {emptyIcon} fa-2x"></i> <br/>{emptyText}</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Type'.t(),
            widgetType: 'type',
            menuDisabled: true,
            align: 'right',
            width: Renderer.uriWidth,
            renderer: 'conditionRenderer'
        }, {
            xtype: 'widgetcolumn',
            widgetType: 'comparator',
            width: Renderer.comparatorWidth,
            menuDisabled: true,
            resizable: false,
            cellWrap: true,
            sortable: false,
            widget: {
                xtype: 'container',
                layout: 'fit'
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
                layout: 'fit'
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
        tpl: '<strong>{text.action}</strong>'
    }],

    constructor: function(config) {
        var me = this;

        var bind = config.bind || me.bind;
        var model = config.model || me.model;
        var allowEmpty = config.allowEmpty || me.allowEmpty;
        var fields = config.fields || me.fields;

        if(bind){
            me.recordName = bind.substring(1,bind.length - 1);
        }
        me.items.forEach(function(item){
            if(item.xtype == 'grid'){
                if(bind){
                    Ext.apply(item, {
                        bind: {
                            store: {
                                model: model,
                                data: '{' + me.recordName +'.list}'
                            }
                        }
                    });
                }
                if(model && item.columns){
                    item.columns.forEach( function(column){
                        if(column.widgetType == 'type'){
                            column.dataIndex = fields.type;
                        }else if(column.widgetType == 'comparator'){
                            column.widget.bind = '{record.' + fields.comparator + '}';
                        }else if(column.widgetType == 'value'){
                            column.widget.bind = '{record.' + fields.value + '}';
                        }
                    });
                }
                item.viewConfig.emptyText = new Ext.Template(item.viewConfig.emptyText).apply({
                    emptyIcon: allowEmpty ? 'fa-exclamation-triangle' : 'fa-exclamation-circle',
                    emptyText: 'No Conditions! Add from the menu...'.t()
                });
            }
        });
        if(bind){
            delete config.bind;
        }
        me.callParent(arguments);
    },

    validate: function(){
        return this.getController().validate();
    },
    isValid: function(){
        return this.getController().validate();
    },

    statics:{
        rendererConditionTemplate: '{type} <strong>{comparator} <span class="cond-val"> {value}</span></strong>'.t(),
        renderer: function(value, meta){
            if (!value) {
                // No value to process.
                return '';
            }

            var me = this,
                view = me.getView().up('grid'),
                conditions = value.list,
                resp = [], i, valueRenderer = [];

            var conditionsEditorField = Ext.Array.findBy(view.editorFields, function(item){
                if(item.xtype.indexOf('conditionseditor') > -1){
                    return true;
                }
            });

            var conditionsMap = conditionsEditorField.conditions;
            if(conditionsMap == null){
                return '';
            }
            conditions.forEach(function(condition){
                var type = condition[conditionsEditorField.fields.type];
                var comparator = condition[conditionsEditorField.fields.comparator];
                var value = condition[conditionsEditorField.fields.value];

                if(!conditionsMap[type]){
                    return;
                }

                comparatorRender = '';
                valueRenderer = [];

                switch (type) {
                    case 'SRC_INTF':
                    case 'DST_INTF':
                        value.toString().split(',').forEach(function (intfff) {
                                Util.getInterfaceList(true, true).forEach(function(interface){
                                if(interface[0] == intfff){
                                    valueRenderer.push(interface[1]);
                                }
                            });
                        });
                    break;
                    case 'DST_LOCAL':
                    case 'WEB_FILTER_FLAGGED':
                        valueRenderer.push('true'.t());
                        break;
                    case 'DAY_OF_WEEK':
                        value.toString().split(',').forEach(function (day) {
                            valueRenderer.push(Util.weekdaysMap[day]);
                        });
                        break;
                    default:
                        // to avoid exceptions, in some situations condition value is null
                        if (value !== null) {
                            valueRenderer = value.toString().split(',');
                        } else {
                            valueRenderer = [];
                        }
                }

                switch(conditionsMap[type].type){
                    case 'sizefield':
                        valueRenderer = [Renderer.datasize(value)];
                        break;
                    case 'boolean':
                        if(conditionsMap[type].values){
                            conditionsMap[type].values.forEach(function(field){
                                if(field[0].toString() == value.toString().toLowerCase()){
                                    valueRenderer = [field[1]];
                                }
                            });
                        }else{
                            valueRenderer = ['True'.t()];
                        }
                        break;
                }

                if(conditionsEditorField.fields.comparator == 'invert'){
                    comparatorRender = (comparator == true ? '&nrArr;' : '&rArr;' );
                }else{
                    switch(conditionsMap[type].comparator){
                        case 'boolean':
                            comparatorRender = (comparator == false ? '&nrArr;' : '&rArr;' );
                            break;
                        case 'numeric':
                            switch(comparator){
                                case '<=':
                                    comparatorRender = '&le;';
                                    break;
                                case '>=':
                                    comparatorRender = '&ge;';
                                    break;
                                default:
                                    comparatorRender = comparator;
                            }
                            break;
                        default:
                            switch(comparator){
                                case 'substr':
                                    comparatorRender = '&sub;';
                                    break;
                                case '!substr':
                                    comparatorRender = '&nsub;';
                                    break;
                                default:
                                    comparatorRender = comparator;
                            }
                            break;
                    }
                }
                resp.push(new Ext.Template(
                    Ung.cmp.ConditionsEditor.rendererConditionTemplate
                ).apply({
                    type: ( conditionsMap[type] != undefined ? conditionsMap[type].displayName : type ),
                    comparator: comparatorRender,
                    value: valueRenderer.join(', ')
                }));
            });
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( resp.join(' &nbsp;&bull;&nbsp; ') ) + '"';
            return resp.length > 0 ? resp.join(' &nbsp;&bull;&nbsp; ') : '<em>' + 'No conditions' + '</em>';
        },
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
        defaultCondition: {
            name: 'name',
            displayName: 'name',
            type: 'textfield',
            comparator: 'invert',
            visible: true,
            allowMultiple: false

        },
        build: function(config){
            var comparators = Ext.clone(Ung.cmp.ConditionsEditor.comparators);
            var found;
            for(var key in config){
                if(key == 'comparators'){
                    config[key].forEach( function(comparator){
                        found = false;
                        comparators.forEach(function(existingComparator, index){
                            if(existingComparator['name'] == comparator['name']){
                                found = true;
                                comparators[index] = comparator;
                            }
                        });
                        if(found == false){
                            comparators.push(comparator);
                        }
                    });
                }else if(key == 'conditions'){
                    var conditionsOrder = [];
                    var conditions = [];
                    found = false;
                    config[key].forEach( function(configCondition){
                        configCondition = typeof(configCondition) == 'object' ? configCondition : {name: configCondition};

                        var newCondition = Ext.clone(Ung.cmp.ConditionsEditor.defaultCondition);

                        found = false;

                        // Lookup in existing conditions.
                        Ung.cmp.ConditionsEditor.conditions.forEach( function(condition){
                            if(condition.name == configCondition.name){
                                Ext.merge(newCondition, condition);
                                Ext.merge(newCondition, configCondition);
                                conditions.push(newCondition);
                                conditionsOrder.push(newCondition.name);
                                found = true;
                            }
                        });
                        if(!found){
                            // Add new condition
                            if('name' in configCondition && 'displayName' in configCondition && 'type' in configCondition ){
                                Ext.merge(newCondition, configCondition);
                                conditions.push(newCondition);
                                conditionsOrder.push(newCondition.name);
                            }else{
                                console.log("not found");
                                console.log(configCondition);
                            }
                        }
                    } );
                    config[key] = Ext.Array.toValueMap(conditions, 'name');
                    if( !config['conditionsOrder'] ){
                        config['conditionsOrder'] = conditionsOrder;
                    }
                }
            }
            config['comparators'] = comparators;
            return Ext.apply({}, config);
        },

        comparators: [{
            name: 'invert',
            defaultValue: 'is',
            store: [[
                true, 'is NOT'.t()
            ],[
                false, 'is'.t()
            ]]
        },{
            name: 'numeric',
            defaultValue: '=',
            store:  [[
                '<', '<'.t()
            ],[
                '<=', '<='.t()
            ],[
                '=', '='.t()
            ],[
                '!=', '!='.t()
            ],[
                '>=', '>='.t()
            ],[
                '>', '>'.t()
            ]]
        },{
            name: 'text',
            defaultValue: 'substr',
            store: [[
                '=', '='.t()
            ],[
                '!=', '!='.t()
            ],[
                'substr', 'Contains'.t()
            ],[
                '!substr', 'Does not contain'.t()
            ]]
        },{
            // More accurately a "boolean in string" where is:= and is not:!=
            name: 'boolean',
            defaultValue: '=',
            store: [[
                '=', 'is'.t()
            ],[
                '!=', 'is NOT'.t()
            ]]
        }],

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
            displayName: "Client MAC Address".t(), 
            type: 'textfield'
        },{
            name:'DST_MAC',
            displayName: 'Server MAC Address'.t(),
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
