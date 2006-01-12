var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

function showDir(dir)
{
  var dirElem = $(dir);

  alert(dirElem + " id: " + dirElem.id);

  if (0 == dirElem.childNodes.length) {
    alert("SENDING AJAX REQUEST");
    // XXX XXX wrong path!!
    new Ajax.Request("http://localhost/browser/ls",
                     { method: "get",
                       parameters: "url=" + dir,
                       onComplete: function(req) {
                                     alert("GOT RESPONSE: " + req.responseText);
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
