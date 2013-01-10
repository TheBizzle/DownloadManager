window.onload = function() {

  $('.ui-button').each(function() {
    var elem = $(this);
    elem.button();
  });

  $('.ui-tabs').each(function() {
    var elem = $(this);
    elem.tabs();
  });

  $('.checkboxes').each(function() {
    var elem = $(this);
    elem.buttonset();
  });

  $('.check-set').each(function() {
    var elem = $(this);
    elem.buttonset();
  });

  $('.radio-set').each(function() {
    var elem = $(this);
    elem.buttonset();
  });

  $('.check-button').each(function() {
    var elem = $(this);
    elem.button();
  });

  $('.check-label').each(function() {
    var elem = $(this);
    elem.click(function() {
      var btn = $("#" + elem.attr("for"));
      btn[0].checked = !btn[0].checked;
      btn.button("refresh");
      btn.change();
      return false;
    });
  });

  $('.os-button').each(function() {
    var elem = $(this);
    elem.click();
  });

  $('.date-picker').each(function() {
    var elem = $(this);
    elem.datepicker();
  });

  $('#start-date-picker').val("01/01/2001");

  $('#end-date-picker').val(getTodaysDateString());

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
  var dd    = padDatePortion(today.getDate());
  var mm    = padDatePortion(today.getMonth() + 1); //January is 0!
  var yyyy  = today.getFullYear();

  return mm + "/" + dd + "/" + yyyy;

};
