var fromIndex = 0;
var transferId = null;
var state = null;

$(document).ready(function () {
    transferId = $('meta[name=transferId]').attr("data-transferId");
    state = $('meta[name=state]').attr("data-state");
    console.log("Transfer id: " + transferId);
    refreshStatus();
    refreshLog();
});

function refreshStatus() {
    $.get("/api/transfers/" + transferId, function (data) {
        console.log(data);
        state = data.state;
        var failCause = data.failCause;
        $("#transfer-state").text(state);
        $("#exceptionSpan").text(failCause != null ? failCause.replace(":", ":\n    ") : "");
        if (state !== 'COMPLETED' && state !== 'FAILED' && state !== 'ABORTED') {
            setTimeout(refreshStatus, 500);
        }
    });
}

function refreshLog() {
    $.get("/api/transfers/" + transferId + "/log?from=" + fromIndex, function (data) {
        console.log(data);
        var hasData = data.fromIndex < data.toIndex;
        if (hasData) {
            appendLogEvents(data.list);
            fromIndex = data.toIndex;
            refreshLog();
        } else {
            if (state !== 'COMPLETED' && state !== 'FAILED' && state !== 'ABORTED') {
                setTimeout(refreshLog, 500);
            }
        }
    });
}

function pad(num) {
    return (num < 10 ? '0' : '') + num;
}

function formatDate(timestampMillis) {
    var date = new Date(timestampMillis);
    var year = date.getFullYear();
    var month = date.getMonth();
    var day = date.getDate();
    var hours = date.getHours();
    var minutes = date.getMinutes();
    var seconds = date.getSeconds();
    return pad(year % 100) + "-" + pad(month) + "-" + pad(day) + " "
        + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds);
}

function appendLogEvents(logEvents) {
    var log = $("#log");
    logEvents.forEach(function (logEvent) {
        log.append("<span class='DATE'>" + formatDate(logEvent.date) + "</span> ");
        log.append("<span class='NONE'>[</span><span class='THREAD'>" + logEvent.thread + "</span><span class='NONE'>]</span> ");
        logEvent.msg.flatParts.forEach(function (msgPart) {
            log.append("<span class='" + msgPart.format + "'>" + msgPart.value + "</span>");
        });
        log.append("\n");
    });
    if ($("input[name=followLog]").is(':checked')) {
        log.scrollTop(log[0].scrollHeight);
    }
}