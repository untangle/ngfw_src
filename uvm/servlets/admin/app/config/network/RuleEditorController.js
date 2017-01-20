Ext.define('Ung.config.network.RuleEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ruleeditor',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            beforerender: 'onBeforeRender',
            close: 'onClose'
        },
        '#applyBtn': {
            click: 'setRuleConditions'
        }
    },

    onClose: function (view) {
        view.destroy();
    },

    onBeforeRender: function (view) {
        view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        view.ruleConditions = view.rule.get('conditions').list;
        view.ruleConditionsMap = Ext.Array.toValueMap(view.ruleConditions, 'conditionType');

        // console.log(view.rule);
    },

    onAfterRender: function (view) {
        var menuConditions = [], i;
        for (i = 0; i < view.ruleConditions.length; i += 1) {
            this.addRowView(view.ruleConditions[i]);
        }

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
            name: 'rule',
            conditionType: a.name,
            layout: {
                type: 'hbox',
                pack: 'justify'
            },
            padding: '3 3 0 3',
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
                margin: '0 3',
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
                name: 'conditionValue',
                value: cond.value
            });
        }

        if (a.type === 'boolean') {
            row.items.push({
                xtype: 'displayfield',
                name: 'conditionValue',
                value: 'True'.t()
            });
        }

        if (a.type === 'checkgroup') {
            var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < a.values.length; i += 1) {
                ckItems.push({inputValue: a.values[i][0], boxLabel: a.values[i][1], name: 'ck'});
            }
            row.items.push({
                xtype: 'checkboxgroup',
                name: 'conditionValue',
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
            xtype: 'component',
            flex: 1
        }, {
            xtype: 'component',
            html: cond.value,
            width: 100
        }, {
            xtype: 'button',
            text: 'Remove',
            iconCls: 'fa fa-times fa-lg'
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

    getConditionValue: function(item) {
        var value = '', view = this.getView();
        // var rule = view.conditionsMap[item.down("[ruleDataIndex=conditionType]").getValue()];
        // if (!rule) {
        //     return value;
        // }
        var valueContainer = item.down("[name=conditionValue]");

        console.log(valueContainer.getXType());

        switch (valueContainer.getXType()) {
        case 'textfield':
            value = valueContainer.getValue();
            break;
        case "boolean":
            value = 'true';
            break;
        case "editor":
            value = valueContainer.down("button").getValue();
            break;
        case 'checkboxgroup':
            if (Ext.isArray(valueContainer.getValue().ck)) {
                value = valueContainer.getValue().ck.join(',');
            } else {
                value = valueContainer.getValue().ck;
            }
            break;
        }
        return value;
    },

    setRuleConditions: function() {
        var list = [], conditionType, view = this.getView(), me = this;
        var ruleConditions = view.query('container[name=rule]');

        Ext.Array.each(ruleConditions, function (item, index, len) {
            // console.log(item.conditionType);
            // console.log(me.getConditionValue(item));

            list.push({
                javaClass: 'aa',
                conditionType: item.conditionType,
                invert: false,
                value: me.getConditionValue(item)
            });

        });
        console.log(list);

        view.rule.set('conditions.list', list);

        console.log(view.rule);

        // Ext.Array.each(this.query("container[name=rule]"), function(item, index, len) {
        //     conditionType = item.down("[ruleDataIndex=conditionType]").getValue();
        //     if(!Ext.isEmpty(conditionType)) {
        //         list.push({
        //             javaClass: me.javaClass,
        //             conditionType: conditionType,
        //             invert: item.down("[ruleDataIndex=invert]").getValue(),
        //             value: me.getRuleValue(item)
        //         });
        //     }
        // });

        // return {
        //     javaClass: "java.util.LinkedList",
        //     list: list,
        //     //must override toString in order for all objects not to appear the same
        //     toString: function() {
        //         return Ext.encode(this);
        //     }
        // };
    },


    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    groupCheckChange: function (el, newVal) {
        console.log(el);
        console.log(this.getViewModel());
    }
});
