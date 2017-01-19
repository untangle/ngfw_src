Ext.define('Ung.config.network.RuleEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ruleeditor',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            beforerender: 'onBeforeRender',
            close: 'onClose'
        }
    },

    onClose: function (view) {
        view.destroy();
    },

    onBeforeRender: function (view) {
        view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        view.ruleConditions = view.rule.get('conditions').list;
        view.ruleConditionsMap = Ext.Array.toValueMap(view.ruleConditions, 'conditionType');
        // this.getViewModel()bind
        console.log(view.rule);
    },

    onAfterRender: function (view) {
        var menuConditions = [], i;
        // for (i = 0; i < view.ruleConditions.length; i += 1) {
        //     this.addRowView(view.ruleConditions[i]);
        // };

        for (i = 0; i < view.conditions.length; i += 1) {
            menuConditions.push({
                text: view.conditions[i].displayName,
                condName: view.conditions[i].name,
                disabled: view.ruleConditionsMap[view.conditions[i].name]
            });
        }

        view.down('#conditionsBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menuConditions,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addRuleCondition'
            }
        });

    },

    addRuleCondition: function (menu, item) {
        item.setDisabled(true);
        var newCond = {
            conditionType: item.condName,
            invert: false,
            // javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
            value: ''
        };
        this.getView().ruleConditions.push(newCond);
        this.addRowView(newCond);
    },


    addRowView: function (cond) {
        var a = this.getView().conditionsMap[cond.conditionType];
        var row = {
            xtype: 'container',
            layout: {
                type: 'hbox',
                // pack: 'justify'
            },
            padding: '1 3 1 3',
            style: {
                borderBottom: '1px #EEE solid'
            },
            defaults: {
                // border: false
            },
            items: [{
                xtype: 'displayfield',
                value: a.displayName,
                width: 150,
            }, {
                xtype: 'segmentedbutton',
                value: cond.invert,
                width: 80,
                items: [{
                    text: '=',
                    value: false
                }, {
                    text: '&ne;',
                    value: true
                }]
            }]
        };

        if (a.type === 'text') {
            row.items.push({
                xtype: 'textfield',
                value: cond.value
            });
        }

        if (a.type === 'boolean') {
            console.log(typeof cond.value);
            row.items.push({
                xtype: 'radiogroup',
                // value: {
                //     rb: cond.value
                // },
                items: [
                    { boxLabel: 'True', name: 'rb', inputValue: true },
                    { boxLabel: 'False', name: 'rb', inputValue: false }
                ]
            });
        }

        if (a.type === 'checkgroup') {
            var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < a.values.length; i += 1) {
                ckItems.push({inputValue: a.values[i][0], boxLabel: a.values[i][1], name: 'ck'});
            }
            row.items.push({
                xtype: 'checkboxgroup',
                flex: 1,
                columns: 3,
                vertical: true,
                defaults: {
                    padding: '0 15 0 0'
                },
                value: {
                    ck: values_arr
                },
                items: ckItems,
                listeners: {
                    change: function (el, newValue) {
                        console.log(cond);
                        console.log(newValue);
                    }
                }
            });
        }


        row.items.push({
            xtype: 'button',
            text: 'Remove',
            iconCls: 'fa fa-trash-o'
        });

        this.getView().add(row);


        // if (a.type === 'textfield')  {
        //     this.getView().add({
        //         xtype: 'container',
        //         items: [{
        //             html: a.displayName
        //         }]
        //     });
        // }
    },


    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    groupCheckChange: function (el, newVal) {
        console.log(this.getViewModel());
    }
});
