window.addEventListener('load', ->
  $('#start-day').val("01/01/2011")
  $('#end-day').val(getYesterdaysDateString())
  populateVersionList()
)

# (URL) => Unit
submitQuery = (url) ->

  # (String) => String
  maybeWithSep = (str) ->
    if str?
      "#{str}|"
    else
      ""

  # (String, String) => String
  append = (toBeAdded, was) -> maybeWithSep(was) + toBeAdded

  # () => String
  generateOSStr = ->

    # (String, String, String) => String
    maybeAppendStr = (elemID, osName, str) ->
      if($("#" + elemID).is(':checked'))
        append(osName, str)
      else
        str

    maybeWithWindowsStr = maybeAppendStr("check-windows", "Windows", "")
    maybeWithMacStr     = maybeAppendStr("check-mac",     "Mac",     maybeWithWindowsStr)
    maybeWithLinuxStr   = maybeAppendStr("check-linux",   "Linux",   maybeWithMacStr)

    maybeWithLinuxStr

  # () => String
  determineQuantum = ->
    if ($("#radio-days").is(':checked'))
      "Day"
    else if ($("#radio-months").is(':checked'))
      "Month"
    else if ($("#radio-years").is(':checked'))
      "Year"
    else
      "Error"

  # () => String
  determineGraphType = ->
    if ($("#radio-discrete").is(':checked'))
      "Discrete"
    else if ($("#radio-cumulative").is(':checked'))
      "Cumulative"
    else
      "Error"

  # () => String
  generateVersionStr = ->

    if ($('#check-all').is(':checked'))
      "all"
    else
      s = ''
      $('#version-holder > .check-label').each(->
        label  = $(this)
        button = $("#" + label.attr("for"))
        if (button.is(':checked'))
          labelText = label.text()
          s = append(labelText, s)
      )
      s

  startDate  = $("#start-day").val()
  endDate    = $("#end-day").val()
  quantumStr = determineQuantum()
  graphType  = determineGraphType()
  osStr      = generateOSStr()
  versionStr = generateVersionStr()

  data = {
    start_day:  startDate,
    end_day:    endDate,
    quantum:    quantumStr,
    graph_type: graphType,
    os:         osStr,
    versions:   versionStr
  }

  $("#query-graph").attr('src', '/assets/images/querying.png')

  $.ajax({
    type:    "POST",
    url:     url,
    data:    data,
    success: (result) -> $("#query-graph").attr('src', "/assets/graphs/#{result}")
  })

  return

# Heavily based on code from Samuel Meddows (http://stackoverflow.com/a/4929629/1116979)
# () => String
getYesterdaysDateString = ->

  padDatePortion = (p) ->
    if (p < 10)
      "0" + p
    else
      p

  today     = new Date()
  yesterday = new Date(today.setDate(today.getDate() - 1))
  dd        = padDatePortion(yesterday.getDate())
  mm        = padDatePortion(yesterday.getMonth() + 1); # January is 0!
  yyyy      = yesterday.getFullYear()

  "#{mm}/#{dd}/#{yyyy}"

# () => Unit
populateVersionList = ->

  $.get("/versions", (x) ->

    versionArr = JSON.parse(x)
    e = $("#version-holder")

    for i in [0...versionArr.length]
      e.append("<input type='checkbox' id='check-#{i}' name='version' class='check-button version-button dynamic-check-button' /><label for='check-#{i}' class='unselectable check-label dynamic-check-label'>#{versionArr[i]}</label>")

    # Generate a proper button for each version
    $('#version-holder > .dynamic-check-button').each(->
      elem = $(this)
      elem.button()
    )

    # Generate a proper label for each version by making it nicely clickable
    $('#version-holder > .dynamic-check-label').each(->

      elem = $(this)
      elem.click(->

        btn = $("#" + elem.attr("for"))
        btn[0].checked = !btn[0].checked
        btn.button("refresh")
        btn.change()

        checkAll = $('#check-all')
        checkAll.attr('checked', false)
        checkAll.button("refresh")
        checkAll.change()

        false

      )

    )

    # Generate a proper "check all" button
    checkAll = $("#check-all")
    checkAll.button()

    # Check the "check all" button, and make it nicely clickable
    $("#check-all-label").click(->

      checkAll.attr('checked', !checkAll.is(":checked"))
      checkAll.button("refresh")
      checkAll.change()

      $('#version-holder > .dynamic-check-button').each(->
        otherBtn = $(this)
        otherBtn.attr('checked', false)
        otherBtn.button("refresh")
        otherBtn.change()
      )

      false

    )

    return

  )

exports.submitQuery = submitQuery
