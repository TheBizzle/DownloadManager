var onLoad = function() {
  $('#start-day').val("01/01/2001");
  $('#end-day').val(getTodaysDateString());
  populateVersionList();
};

var funcs = new Array();
if(window.onload != null && typeof window.onload == 'function') funcs.push(window.onload);
funcs.push(onLoad);

window.onload = function() {
  for(var i = 0; i < funcs.length; i++)
    funcs[i]();
};

var submitQuery = function(url) {

  var maybeWithSep = function(str) {
    if (str)
      return str + "|";
    else
      return "";
  };

  var append = function(toBeAdded, was) {
    return maybeWithSep(was) + toBeAdded;
  };

  var generateOSStr = function() {

    var maybeAppendStr = function(elemID, osName, str) {
      if($("#" + elemID).is(':checked'))
        return append(osName, str);
      else
        return str;
    };

    var maybeWithWindowsStr = maybeAppendStr("check-windows", "Windows", "");
    var maybeWithMacStr     = maybeAppendStr("check-mac",     "Mac",     maybeWithWindowsStr);
    var maybeWithLinuxStr   = maybeAppendStr("check-linux",   "Linux",   maybeWithMacStr);

    return maybeWithLinuxStr;

  };

  var determineQuantum = function() {
    if ($("#radio-days").is(':checked'))
      return "Day";
    else if ($("#radio-months").is(':checked'))
      return "Month";
    else if ($("#radio-years").is(':checked'))
      return "Year";
    else
      return "Error";
  };

  var determineGraphType = function() {
    if ($("#radio-discrete").is(':checked'))
      return "Discrete";
    else if ($("#radio-cumulative").is(':checked'))
      return "Cumulative";
    else
      return "Error";
  };

  var generateVersionStr = function() {

    if ($('#check-all').is(':checked'))
      return "all";
    else {

      var s = '';

      $('#version-holder > .check-label').each(function() {
        var label  = $(this);
        var button = $("#" + label.attr("for"));
        if (button.is(':checked')) {
          var labelText = label.text();
          s = append(labelText, s);
        }
      });

      return s;

    }

  };

  var startDate  = $("#start-day").val();
  var endDate    = $("#end-day").val();
  var quantumStr = determineQuantum();
  var graphType  = determineGraphType();
  var osStr      = generateOSStr();
  var versionStr = generateVersionStr();

  var data = {
    start_day:  startDate,
    end_day:    endDate,
    quantum:    quantumStr,
    graph_type: graphType,
    os:         osStr,
    versions:   versionStr
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

var populateVersionList = function() {
  $.get("/versions", function(x) {

    var versionArr = JSON.parse(x);
    var elem = $("#version-holder");

    for (var i = 0; i < versionArr.length; i++) {
      elem.append('<input type="checkbox" id="check-' + i + '" name="version" class="check-button version-button dynamic-check-button" /><label for="check-' + i + '" class="unselectable check-label dynamic-check-label">' + versionArr[i] + '</label>');
    }

    $('#version-holder > .dynamic-check-button').each(function() {
      var elem = $(this);
      elem.button();
    });

    $('#version-holder > .dynamic-check-label').each(function() {
      var elem = $(this);
      elem.click(function() {

        var btn = $("#" + elem.attr("for"));
        btn[0].checked = !btn[0].checked;
        btn.button("refresh");
        btn.change();

        var checkAll = $('#check-all');
        checkAll.attr('checked', false);
        checkAll.button("refresh");
        checkAll.change();

        return false;

      });
    });

    var checkAll = $("#check-all");
    checkAll.button();
    $("#check-all-label").click(function () {

      checkAll.attr('checked', !checkAll.is(":checked"));
      checkAll.button("refresh");
      checkAll.change();

      $('#version-holder > .dynamic-check-button').each(function() {
        var otherBtn = $(this);
        otherBtn.attr('checked', false);
        otherBtn.button("refresh");
        otherBtn.change();
      });

      return false;

    });

  });
};

