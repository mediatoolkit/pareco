var fromIndex = 0;
var transferId = null;
var state = null;
var log = null;
var followLog = null;

$(document).ready(function () {
    transferId = $('meta[name=transferId]').attr("data-transferId");
    state = $('meta[name=state]').attr("data-state");
    log = $("#log");
    followLog = $("input[name=followLog]");
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
            if (!isTransferDone()) {
                setTimeout(refreshLog, 500);
            }
        }
    });
}

function isTransferDone() {
    return state === 'COMPLETED' || state === 'FAILED' || state === 'ABORTED';
}

function pad(num) {
    return (num < 10 ? '0' : '') + num;
}

function pad2(num) {
    return (num < 10 ? '00' : (num < 100 ? '0' : '')) + num;
}

function formatDate(timestampMillis) {
    var date = new Date(timestampMillis);
    var year = date.getFullYear();
    var month = date.getMonth();
    var day = date.getDate();
    var hours = date.getHours();
    var minutes = date.getMinutes();
    var seconds = date.getSeconds();
    var millis = date.getMilliseconds()
    return pad(year % 100) + "-" + pad(month) + "-" + pad(day) + " "
        + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds) + "." + pad2(millis % 1000);
}

function appendLogEvents(logEvents) {
    logEvents.forEach(function (logEvent) {
        log.append("<span class='DATE'>" + formatDate(logEvent.date) + "</span> ");
        log.append("<span class='NONE'>[</span><span class='THREAD'>" + logEvent.thread + "</span><span class='NONE'>]</span> ");
        logEvent.msg.flatParts.forEach(function (msgPart) {
            log.append("<span class='" + msgPart.format + "'>" + msgPart.value + "</span>");
        });
        log.append("\n");
    });
    if (followLog.is(':checked')) {
        scrollLogToBottom();
    }
}

function scrollLogToBottom() {
    log.scrollTop(log[0].scrollHeight);
}