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
        return encodeURIComponent(this.get('column')) + ':' + encodeURIComponent(this.get('operator')) + ':' + encodeURIComponent(this.get('value')) + ':' + (this.get('autoFormatValue') === true ? 1 : 0) + ( this.get('table') ? ':' + encodeURIComponent(this.get('table')) : '');
    },

    statics:{
        collect: function(conditions){
            return Ext.Array.map(conditions, function(condition){
                return condition.getData();
            });
        },
        getAllQueries: function(conditions, prefix){
            return ( conditions.length ? ( prefix !== undefined? prefix : '&') + Ext.Array.map(conditions, function(condition){
                                return condition.getQuery();
                                }).join("&") : '' );
        }
    }
});
