window.onload = function() {

  $('.ui-button').each(function() {
    var elem = $(this);
    elem.button();
  });

  $('.ui-tabs').each(function() {
     var elem = $(this);
     elem.tabs();
  })

};
