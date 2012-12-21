window.onload = function() {

  var setHover = function(elem) {
    elem.hover(
      function(){ elem.addClass('ui-state-hover'); },
      function(){ elem.removeClass('ui-state-hover'); }
    );
  };

  var setMouseDown = function(elem) {
    elem.mousedown(function(){ elem.addClass('ui-state-active'); });
  };

  var setMouseUp = function(elem) {
    elem.mouseup(function(){ elem.removeClass('ui-state-active'); });
  };

  $('.ui-button').each(function() {
    var elem = $(this);
    setHover(elem);
    setMouseDown(elem);
    setMouseUp(elem);
  });

};
