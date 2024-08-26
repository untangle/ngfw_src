Ext.define('Ung.apps.intrusionprevention.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-rules',
    itemId: 'rules',
    title: 'Rules'.t(),
    scrollable: true,
    withValidation: false,
    editorFieldProtocolTcpUdpOnly: true,
    controller: 'unintrusionrulegrid',

    viewConfig: {
        listeners: {
            drop: 'onDragDrop'
        }
    },

    emptyText: 'No Rules Defined'.t(),
    name: 'rules',

    restrictedRecords: {
        keyMatch: 'id',
        valueMatch: /^reserved\_/,
        editableFields:[
            "enabled"
        ]
    },

    region: 'center',

    bind: '{rules}',

    bbar: [{
        xtype: 'tbtext',
        name: 'ruleStatus',
        bind: {
            html: Ext.String.format('{0}Signatures affected:{1} Log: {2}, Block: {3}, Disabled: {4}'.t(), '<b>', '</b>', '{signatureStatusLog}', '{signatureStatusBlock}', '{signatureStatusDisable}'),
            hidden: '{signatureStatusTotal == 0}'
        }
    }],

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete', 'reorder'],
    copyId: 'id',
    copyAppendField: 'description',
    recordModel: 'Ung.model.intrusionprevention.rule',

    emptyRow: {
        javaClass: 'com.untangle.app.intrusion_prevention.IntrusionPreventionRule',
        enabled: true,
        id: -1,
        description: '',
        conditions: {
            javaClass: "java.util.LinkedList",
            'list': []
        },
        action: 'default',
        sourceNetworks: 'recommended',
        destinationNetworks: 'recommended'
    },

    importValidationJavaClass: true,

    importValidationForComboBox: true,

    columns: [{
        xtype:'checkcolumn',
        header: "Enabled".t(),
        dataIndex: 'enabled',
        width: Renderer.booleanWidth
    },{
        header: "Description".t(),
        dataIndex: 'description',
        width: Renderer.messageWidth,
        flex:2,
        editor: {
            xtype: 'textfield',
            emptyText: '[no description]'.t(),
            allowBlank: false,
            blankText: 'The description cannot be blank.'.t(),
        }
    },{
        header: "Conditions".t(),
        dataIndex: 'conditions',
        width: Renderer.conditionsWidth,
        flex: 3,
        renderer: Ung.cmp.ConditionsEditor.renderer
    },{
        header: "Action".t(),
        dataIndex: 'action',
        width: Renderer.messageWidth,
        flex: 2,
        renderer: Ung.apps.intrusionprevention.MainController.ruleActionsRenderer,
        editor: {
            xtype: 'combo',
            editable: false,
            queryMode: 'local',
            store: Ung.apps.intrusionprevention.Main.ruleActions,
            displayField: 'description',
            valueField: 'value'
        }
    }],

    editorXtype: 'ung.cmp.unintrusionrulesrecordeditor',
    editorFields: [{
        xtype:'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t(),
    },{
        xtype:'textfield',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: "[enter description]".t(),
        allowBlank: false
    },
    Ung.cmp.ConditionsEditor.build({
        xtype: 'ipsrulesconditionseditor',
        text:{
            condition: 'For all signatures that match the following conditions:'.t(),
            action: 'Modify each signature as:'.t(),
        },
        bind: '{record.conditions}',
        flex: 1,
        model: 'Ung.apps.intrusionprevention.model.Condition',
        fields:{
            type: 'type',
            comparator: 'comparator',
            value: 'value',
        },
        javaClassValue: 'com.untangle.app.intrusion_prevention.IntrusionPreventionRuleCondition',

        comparators:[{
            name: 'numeric',
            defaultValue: '=',
            store: [[
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
                '>', '>'.t(),
            ],[
                'substr', 'Contains'.t()
            ],[
                '!substr', 'Does not contain'.t()
            ]]
        },{
            name: 'network',
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
            name: 'port',
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
        }],

        conditions: [{
            name: "SID",
            displayName: "Signature Identifier".t(),
            type: "textfield",
            comparator: 'numeric',
            validator: Ung.apps.intrusionprevention.MainController.conditionValidateNumeric
        },{
            name:"GID",
            displayName: "Group Identifier".t(),
            type: 'textfield',
            comparator: 'numeric',
            validator: Ung.apps.intrusionprevention.MainController.conditionValidateNumeric
        },{
            name:"CATEGORY",
            displayName: "Category".t(),
            type: 'checkboxgroup',
            store: Ung.apps.intrusionprevention.Main.categories,
            storeValue: 'value',
            storeLabel: 'value',
            storeTip: Ung.apps.intrusionprevention.Main.categoryRenderer,
            comparator: 'boolean'
        },{
            name:"CLASSTYPE",
            displayName: "Classtype".t(),
            type: 'checkboxgroup',
            store: Ung.apps.intrusionprevention.Main.classtypes,
            storeValue: 'value',
            storeLabel: 'value',
            storeTip: Ung.apps.intrusionprevention.Main.classtypeRenderer,
            comparator: 'boolean'
        },{
            name:"MSG",
            displayName: "Message".t(),
            type: 'textfield',
            comparator: 'text'
        },{
            name:"PROTOCOL",
            displayName: "Protocol".t(),
            type: 'checkboxgroup',
            values: [],
            comparator: 'boolean'
        },{
            name:"SRC_ADDR",
            displayName: "Source Address".t(),
            type: 'textfield',
            vtype: undefined,
            comparator: 'network'
        },{
            name:"SRC_PORT",
            displayName: "Source Port".t(),
            type: 'textfield',
            vtype: undefined,
            comparator: 'port'
        },{
            name:"DST_ADDR",
            displayName: "Destination Address".t(),
            type: 'textfield',
            vtype: undefined,
            comparator: 'network'
        },{
            name:"DST_PORT",
            displayName: "Destination Port".t(),
            type: 'textfield',
            vtype: undefined,
            comparator: 'port'
        },{
            name:"SIGNATURE",
            displayName: "Any part of signature".t(),
            type: 'textfield',
            comparator: 'text'
        },{
            name:"CUSTOM",
            displayName: "Custom signature".t(),
            type: 'boolean',
            comparator: 'boolean',
            values: [[
                "true","True".t()
            ],[
                "false","False".t()
            ]]
        },{
            name:"ACTION",
            displayName: "Recommended Action".t(),
            type: 'checkboxgroup',
            store: Ung.apps.intrusionprevention.Main.signatureActions,
            storeValue: 'value',
            storeLabel: 'description',
            storeTip: Ung.apps.intrusionprevention.Main.actionRenderer,
            comparator: 'boolean'
        },{
            name:"SYSTEM_MEMORY",
            displayName: "System Memory".t(),
            type: 'sizefield',
            comparator: 'numeric',
            validator: Ung.apps.intrusionprevention.MainController.conditionValidateNumeric
        }]
    }),
    {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        labelAlign: 'right',
        width: 400,
        store: Ung.apps.intrusionprevention.Main.ruleActions,
        bind: {
            value: '{record.action}'
        },
        queryMode: 'local',
        editable: false,
        typeField: 'type',
        displayField: 'description',
        valueField: 'value'
    },{
        fieldLabel: 'Match source networks'.t(),
        xtype: 'combo',
        labelAlign: 'right',
        width: 400,
        bind: {
            store: '{networkVariables}',
            value: '{record.sourceNetworks}',
            hidden: '{record.action != "whitelist"}'
        },
        listConfig:   {
            itemTpl: '<div data-qtip="{detail}">{description}</div>'
        },
        queryMode: 'local',
        editable: false,
        typeField: 'type',
        displayField: 'description',
        valueField: 'value',
        matchFieldWidth: false
    },{
        fieldLabel: 'Match destination networks'.t(),
        xtype: 'combo',
        labelAlign: 'right',
        width: 400,
        bind: {
            store: '{networkVariables}',
            value: '{record.destinationNetworks}',
            hidden: '{record.action != "whitelist"}'
        },
        listConfig:   {
            itemTpl: '<div data-qtip="{detail}">{description}</div>'
        },
        queryMode: 'local',
        editable: false,
        typeField: 'type',
        displayField: 'description',
        valueField: 'value',
        matchFieldWidth: false
    },{
        xtype: 'component',
        style: 'background-color: yellow;',
        padding: '10px 0px 10px 0px',
        bind:{
            html: Ext.String.format("{0}Warning:{1} No variables found with excluded like '!{2}'.".t(), '<b>', '</b>', '{defaultNetwork}'),
            hidden: '{record.action != "whitelist" || networkVariablesList.length > 1}'
        }
    },{
        xtype: 'component',
        bind:{
            html: Ext.String.format("{0}NOTE:{1} Affected signatures use Recommended action.".t(), '<b>', '</b>'),
            hidden: '{record.action != "whitelist"}'
        }
    }]
});
