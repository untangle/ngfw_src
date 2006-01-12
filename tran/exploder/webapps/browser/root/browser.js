var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

function showDir(dir)
{
  var dirElem = $(dir);

  if (0 == dirElem.childNodes.length) {

    new Ajax.Request("ls",
                     { method: "get",
                       parameters: "url=" + dir,
                       onComplete: function(req)
                                   {
                                     var resp = parseDomFromString(req.responseText);
                                     var root = resp.getElementsByTagName('root')[0];
                                     alert("ROOT: " + root);
                                   }
                     });
  } else {
    var dirStyle = dirElem.style;

    if (dirElem.display == "block") {
      dirStyle.display = "none";
    } else {
      dirStyle.display = "block";
    }
    swapFolder('I' + dir);
  }
}

function swapFolder(img)
{
  objImg = $(img);

  if (objImg.src.indexOf('closed.gif') > -1) {
    objImg.src = openImg.src;
  } else {
    objImg.src = closedImg.src;
  }
}

function parseDomFromString(text)
{
  return Try.these(function()
                   {
                     return new DOMParser().parseFromString(text, 'text/xml');
                   },
                   function()
                   {
                     var xmlDom = new ActiveXObject("Microsoft.XMLDOM")
                     xmlDom.async = "false"
                     xmlDom.loadXML(text)
                     return xmlDom;
                   });
}