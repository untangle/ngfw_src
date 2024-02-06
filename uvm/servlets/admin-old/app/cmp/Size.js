Ext.define('Ung.cmp.SizeField', {
    extend: 'Ext.form.field.Text',
    alias: 'widget.sizefield',

    // compound compoent with text and combo
    // getValue() to record
    // setValue() from record

//    regex: /^(\d+)\s*(|B|MB|KB|GB|TB|PB)$/,

    // getValue: function(){
    //     var value = this.callParent();
    //     console.log('getValue=' + value);
    //     return value;
    // },

    // getRawValue: function(){
    //     var value = this.callParent();
    //     console.log('getRawValue=' + value);

    //     // var matches = this.regex.exec( value);
    //     // if(matches && matches.length == 3){
    //     //     if(matches[2] != ''){
    //     //         Renderer.datasizeMap.forEach(function(size){
    //     //             if(size[1] == matches[2]){
    //     //                 value = parseInt(matches[1],10) * size[0];
    //     //             }
    //     //         });
    //     //     }
    //     // }
    //     // console.log(value);
    //     return value;
    // },

    // setRawValue: function(value){
    //     console.log('setRawValue=' + value);

    //     if(this.regex.test(value)){
    //         console.log('change: set raw value');
    //         value = Renderer.datasize(value);
    //     }

    //     var value = this.callParent([value]);

    // }

    // listeners: {
        // change: function( field, newValue, oldValue){
        //     if(this.regex.test(newValue)){
        //         console.log('change: set raw value');
        //         field.setRawValue(Renderer.datasize(newValue));
        //     }
        // }
    // }
});
