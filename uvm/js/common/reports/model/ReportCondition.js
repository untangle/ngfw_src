Ext.define ('Ung.model.ReportCondition', {
    extend: 'Ext.data.Model' ,
    fields: [{
        name: 'column',
        type: 'string'
    },{
        name: 'operator',
        type: 'string'
    },{
        name: 'value',
        type: 'string'
    },{
        name: 'autoFormatValue',
        type: 'boolean'
    },{
        name: 'table',
        type: 'string'
    },{
        name: 'javaClass',
        type: 'string',
        defaultValue: 'com.untangle.app.reports.SqlCondition'
    }],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },

    getQuery: function(){
        return encodeURIComponent(this.get('column')) + ':' + encodeURIComponent(this.get('operator')) +  ':' + (this.get('autoFormatValue') === true ? 1 : 0) + ':' + encodeURIComponent(this.get('table'))  + ':' + encodeURIComponent(this.get('value'));
    },

    statics:{
        collect: function(conditions){
            return conditions ? Ext.Array.map(conditions, function(condition){
                return condition.getData();
            }) : null;
        },
        getAllQueries: function(conditions, prefix){
            return conditions ? ( conditions.length ? ( prefix !== undefined? prefix : '&') + Ext.Array.map(conditions, function(condition){
                                return condition.getQuery();
                                }).join("&") : '' ) : '';
        }
    }
});
