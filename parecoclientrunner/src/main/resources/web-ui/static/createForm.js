function handleAdvancedChange(checkbox) {
    if (checkbox.checked) {
        $(".advanced").removeClass("hidden");
    } else {
        $(".advanced").addClass("hidden");
    }
}

function checkLocalDir() {
    var localDir = $("input[name=localDir]").val();
    var include = $("input[name=include]").val();
    var exclude = $("input[name=exclude]").val();
    $("#checkType").text("Local dir contents: " + localDir + " include: " + include + " exclude: " + exclude);
    $("#dirContents").text("");
    $("#checkInfo").text("");
    $.get("/api/checkLocalDir?localDir=" + localDir + "&include=" + include + "&exclude=" + exclude)
        .done(handleOkDirStructure)
        .fail(handleFailedRequest);
}

function checkRemoteDir() {
    var remoteDir = $("input[name=remoteDir]").val();
    var include = $("input[name=include]").val();
    var exclude = $("input[name=exclude]").val();
    var server = $("input[name=server]").val();
    var authToken = $("input[name=authToken]").val();
    $("#checkType").text("Remote dir contents: " + remoteDir + " include: " + include + " exclude: " + exclude + " server: " + server);
    $("#dirContents").text("");
    $("#checkInfo").text("");
    $.get("/api/checkRemoteDir?remoteDir=" + remoteDir + "&include=" + include + "&exclude=" + exclude + "&server=" + server + "&authToken=" + authToken)
        .done(handleOkDirStructure)
        .fail(handleFailedRequest);
}

function handleOkDirStructure(data) {
    var contents = formatDirStructure(data);
    $("#dirContents").text(contents);
    $("#checkInfo").text("Num dirs: " + data.directories.length + " num files: " + data.files.length);
}

function handleFailedRequest(error) {
    var msg;
    if (error.status === 0) {
        msg = "can't perform request to server";
    } else {
        msg = error.responseJSON.message;
    }
    $("#checkInfo").text("Failed, error: " + msg);
}

function formatDirStructure(dirStructure) {
    var result = "";
    dirStructure.files.forEach(function (file) {
        var relativeDir = file.filePath.relativeDirectory;
        result += relativeDir + (relativeDir === "" ? "" : "/") + file.filePath.fileName + "\n";
    });
    return result;
}
