Ext.define('Ung.config.network.ConditionWidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.condwidget',

    control: {
        '#': {
            // afterrender: 'onAfterRender',
            beforerender: 'onBeforeRender',
            close: 'onClose',
        },
        '#conditionsBtn': {
            // click: 'addCondition'
        }
    },

    onClose: function (view) {
        view.destroy();
    },

    onWidgetAttach: function (column, container, record) {
        console.log(record);
        var condition = this.getView().conditionsMap[record.get('conditionType')], i, ckItems = [];

        switch (condition.type) {
        case 'textfield':
            container.add({
                xtype: 'textfield',
                bind: {
                    value: '{record.value}'
                }
            });
            break;
        case 'checkboxgroup':
            console.log(condition.values);
            // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    inputValue: condition.values[i][0],
                    boxLabel: condition.values[i][1]
                });
            }
            container.add({
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}'
                },
                items: ckItems
            });
        }


    },

    // view.down('#conditionsBtn').setMenu({
    //     showSeparator: false,
    //     plain: true,
    //     items: menuConditions,
    //     mouseLeaveDelay: 0,
    //     listeners: {
    //         click: 'addRuleCondition'
    //     }
    // });

    onBeforeRender: function (view) {
        view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        // console.log(view.getViewModel().get('rule'));
        console.log('here');
        // view.ruleConditions = view.rule.get('conditions').list;
        // view.ruleConditionsMap = Ext.Array.toValueMap(view.ruleConditions, 'conditionType');

        // console.log(view.rule);
    },

    addCondition: function () {
        // console.log(this.getViewModel().get('record.conditions.list'));
        // var list = this.getViewModel().get('record.conditions.list');

        // list.push({
        //     conditionType: 'SRC_INTF',
        //     invert: false,
        //     javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
        //     value: 'a'
        // });

        // this.getViewModel().set('record.conditions.list', list);

        // this.getViewModel().set('record.conditions.list', ['a']);

        var rec = this.getView().getStore().insert(0, {
            conditionType: 'SRC_INTF',
            invert: false,
            javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
            value: 'a'
        })[0];
        this.getView().getStore().commitChanges();
        rec.commit();
        this.getView().getStore().reload();



        // var newCond = {
        //     conditionType: item.condName,
        //     invert: false,
        //     // javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
        //     value: ''
        // };
    },


    onAfterRender: function (view) {
        // console.log(view.getViewModel().get('record.conditions'));
        // conds = view.getViewModel().get('record.conditions.list');

        // conds[0].value = 'aaaaaaa';

        // var menuConditions = [], i;
        // // for (i = 0; i < view.ruleConditions.length; i += 1) {
        // //     this.addRowView(view.ruleConditions[i]);
        // // };

        // for (i = 0; i < view.conditions.length; i += 1) {
        //     menuConditions.push({
        //         text: view.conditions[i].displayName,
        //         condName: view.conditions[i].name,
        //         disabled: view.ruleConditionsMap[view.conditions[i].name]
        //     });
        // }

        // view.down('#conditionsBtn').setMenu({
        //     showSeparator: false,
        //     plain: true,
        //     items: menuConditions,
        //     mouseLeaveDelay: 0,
        //     listeners: {
        //         click: 'addRuleCondition'
        //     }
        // });

    },

    // addRuleCondition: function (menu, item) {
    //     item.setDisabled(true);
    //     var newCond = {
    //         conditionType: item.condName,
    //         invert: false,
    //         // javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
    //         value: ''
    //     };
    //     this.getView().ruleConditions.push(newCond);
    //     this.addRowView(newCond);
    // },


    addRowView: function (cond) {
        var a = this.getView().conditionsMap[cond.conditionType];
        var row = {
            xtype: 'container',
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
                value: cond.value
            });
        }

        if (a.type === 'boolean') {
            row.items.push({
                xtype: 'displayfield',
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


    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    groupCheckChange: function (el, newVal) {
        console.log(el);
        console.log(this.getViewModel());
    }
});
