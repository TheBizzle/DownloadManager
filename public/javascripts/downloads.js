var onLoad = function() {
  $('#start-day').val("01/01/2001");
  $('#end-day').val(getTodaysDateString());
};

var funcs = new Array();
if(window.onload != null && typeof window.onload == 'function') funcs.push(window.onload);
funcs.push(onLoad);

window.onload = function() {
  for(var i = 0; i < funcs.length; i++)
    funcs[i]();
};

var submitQuery = function(url) {

  var generateOSStr = function() {

    var maybeWithSep = function(str) {
      if (str) {
        return str + "|";
      }
      else {
        return "";
      }
    };

    var append = function(toBeAdded, was) {
      return maybeWithSep(was) + toBeAdded;
    };

    var maybeAppendStr = function(elemID, osName, str) {
      if($("#" + elemID).is(':checked')) {
        return append(osName, str);
      }
      else {
        return str;
      }
    };

    var maybeWithWindowsStr = maybeAppendStr("check-windows", "Windows", "");
    var maybeWithMacStr     = maybeAppendStr("check-mac",     "Mac",     maybeWithWindowsStr);
    var maybeWithLinuxStr   = maybeAppendStr("check-linux",   "Linux",   maybeWithMacStr);

    return maybeWithLinuxStr;

  };

  var osStr = generateOSStr();

  var data = {
    os:        osStr,
    start_day: $("#start-day").val(),
    end_day:   $("#end-day").val()
  };

  $.ajax({
    type: "POST",
    url:  url,
    data: data,
    success: function(result) {
      $("#query-graph").attr('src', '/assets/graphs/' + result);
    }
  });

};

// Heavily based on code from Samuel Meddows (http://stackoverflow.com/a/4929629/1116979)
var getTodaysDateString = function() {

  var padDatePortion = function(p) {
    if (p < 10)
      return "0" + p;
    else
      return p;
  };

  var today = new Date();
  var dd  = padDatePortion(today.getDate());
  var mm  = padDatePortion(today.getMonth() + 1); //January is 0!
  var yyyy  = today.getFullYear();

  return mm + "/" + dd + "/" + yyyy;

};
