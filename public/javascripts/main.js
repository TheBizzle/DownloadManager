window.onload = function() {

  $('.collapsible-accordion').accordion({
    collapsible: true,
    active: false,
    heightStyle: "content"
  });

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
    if (!elem[0].checked) elem.click();
  });

  $('.date-chooser').each(function() {
    var elem = $(this);
    elem.datepicker();
  });

};

