/**
 * @class Ext.ux.form.field.DateTime
 * @extends Ext.form.FieldContainer
 *
 * DateTime field, combination of DateField and TimeField
 * @author atian25 (http://www.sencha.com/forum/member.php?51682-atian25)
 * @author Jojo79 (http://www.sencha.com/forum/member.php?321626-Jojo79)
 */
Ext.ns('Ext.ux.form');

Ext.define('Ext.ux.form.field.DateTime', {
    extend:'Ext.form.FieldContainer',
    mixins: {
        field: 'Ext.form.field.Field'
    },
    alias: ['widget.xdatetime'],
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    
    dateFormat:'m/d/y',
    timeFormat:'g:i A',
    
    height: 22,
    style: {
        width : "100%"
    },
    dateCfg:{},
    timeCfg:{},
    valueCfg:{},
    
    initComponent: function() {
        var me = this;
        me.buildField();
        me.callParent();
        this.dateField = this.down('datefield');
        this.timeField = this.down('timefield');
        this.valueField = this.down('hiddenfield');
        me.initField();
    },

    //@private
    buildField: function(){
        this.items = [
            Ext.apply({
                xtype: 'datefield',
                id: this.id + "-date",
                name : this.name + "-date",
                format: this.dateFormat || Ext.form.DateField.prototype.format,
                flex: .55,
                margin: '0 1 0 0',
                submitValue: false,
                listeners : {     
                    'change':function ( dateObj, newValue, oldValue, eOpts ){            
                                this.ownerCt.onUpdateDate();
                             }
                }
            },this.dateCfg),
            Ext.apply({
                xtype: 'timefield',
                id: this.id + "-time",
                name : this.name + "-time",
                format: this.timeFormat || Ext.form.TimeField.prototype.format,
                flex: .45,
                margin: '0 0 0 1',
                submitValue: false,
                listeners : {     
                    'change':function ( dateObj, newValue, oldValue, eOpts ){            
                                this.ownerCt.onUpdateTime();
                             }
                }
            },this.timeCfg),
            Ext.apply({
                xtype: 'hiddenfield',
                id: this.id + "-datetimevalue",
                name: this.name + "-datetimevalue",
                value: '',
                getValue: function(){
            if(this.ownerCt) {
                return this.ownerCt.getValue();
            }else{
                return;
            } 
        },
        getSubmitData: function(){
            data = {};
            if(this.ownerCt){
                data[this.ownerCt.name] = '' + this.ownerCt.getRawValue()
            }
            return data;
        }
            }, this.valueCfg)
        ]
    },

    getValue: function() {
        var value = '',date = this.dateField.getSubmitValue(),time = this.timeField.getSubmitValue();
        if(date){
            if(time){
                var format = this.getFormat();
                value = Ext.Date.parse(date + ' ' + time,format);
            }else{
                value = this.dateField.getValue();
            }
        }
        return value ? value.getTime():0;
    },

    setValue: function(value){
        var ts = 0;
        if ( value) {
            ts = new Date(value);
        }
        this.dateField.setValue(ts);
        this.timeField.setValue(ts);
        this.valueField.value = this.getRawValue();
    },

    getSubmitData: function(){
        data = {};
        return  data[this.name] = '' + this.getRawValue();
    },

    getFormat: function(){
        return (this.dateField.submitFormat || this.dateField.format) + " " + (this.timeField.submitFormat || this.timeField.format)
    },
    
    getRawValue: function() {
        if(this.getValue()){
            return this.getValue();
        }
        return '';
    },
    
    getTime: function() {
        if(!Ext.isEmpty(this.getValue())){
            return this.getValue().getTime();
        }
    },
    
    initDateValue:function() {
        return new Date();
    },

    onUpdateDate:function() {
        var d = this.dateField.getValue();
        if(!d){
            this.timeField.setValue('');
        }
        if(d && !(d instanceof Date)) {
            d = Ext.Date.parse(d, this.dateField.format);
        }
        if(d && !this.timeField.getValue()) {
            this.timeField.setValue(this.initDateValue());
        }
        this.valueField.value = this.getRawValue(); 
    },
    
    onUpdateTime:function() {
        var t = this.timeField.getValue();
        if(t && !(t instanceof Date)) {
            t = Ext.Date.parse(t, this.timeField.format);
        }
        if(t && !this.dateField.getValue()) {
            this.dateField.setValue(this.initDateValue());
        }
        this.valueField.value = this.getRawValue();
    }
    
})