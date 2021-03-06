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
    $("#checkType").text("Contents of LOCAL dir: '" + localDir + "' include: '" + include + "' exclude: '" + exclude + "'");
    $("#dirContents").text("Loading...");
    $("#checkInfo").text("Loading...");
    $.ajax({
        url: "/api/checkLocalDir",
        type: "get",
        data: {
            localDir: localDir,
            include: include,
            exclude: exclude
        },
        success: handleOkDirStructure,
        error: handleFailedRequest
    });
}

function checkRemoteDir() {
    var remoteDir = $("input[name=remoteDir]").val();
    var include = $("input[name=include]").val();
    var exclude = $("input[name=exclude]").val();
    var server = $("input[name=server]").val();
    var authToken = $("input[name=authToken]").val();
    $("#checkType").text("Contents of REMOTE dir: '" + remoteDir + "' include: '" + include + "' exclude: '" + exclude + "' server: " + server);
    $("#dirContents").text("Loading...");
    $("#checkInfo").text("Loading...");
    $.ajax({
        url: "/api/checkRemoteDir",
        type: "get",
        data: {
            remoteDir: remoteDir,
            include: include,
            exclude: exclude,
            server: server,
            authToken: authToken
        },
        success: handleOkDirStructure,
        error: handleFailedRequest
    });
}

function humanFileSize(bytes) {
    var step = 1024;
    if (Math.abs(bytes) < step) {
        return bytes + 'B';
    }
    var units = ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    var u = -1;
    do {
        bytes /= step;
        ++u;
    } while (Math.abs(bytes) >= step && u < units.length - 1);
    return bytes.toFixed(1) + units[u];
}

function handleOkDirStructure(data) {
    var contents = formatDirStructure(data);
    $("#dirContents").text(contents);
    $("#checkInfo")
        .text("Directories: " + data.directories.length + " Files: " + data.files.length + " Total size: " + humanFileSize(data.totalSizeBytes))
        .removeClass("error");
}

function handleFailedRequest(error) {
    var msg;
    if (error.status === 0) {
        msg = "can't perform request to server";
    } else {
        msg = error.responseJSON.message;
    }
    $("#checkInfo").text("Failed, error: " + msg).addClass("error");
    $("#dirContents").text("");
}

function formatDirStructure(dirStructure) {
    var elements = [];
    dirStructure.directories.forEach(function (dir) {
        var relativeDir = dir.filePath.relativeDirectory;
        elements.push({
            info: "".padEnd(7, '-'),
            name: relativeDir + (relativeDir === "" ? "" : "/") + dir.filePath.fileName + "/"
        });
    });
    dirStructure.files.forEach(function (file) {
        var relativeDir = file.filePath.relativeDirectory;
        var fileSize = humanFileSize(file.fileSizeBytes);
        elements.push({
            info: fileSize.padStart(7, ' '),
            name: relativeDir + (relativeDir === "" ? "" : "/") + file.filePath.fileName
        });
    });
    return elements
        .sort(function(e1, e2) {
            return e1.name.localeCompare(e2.name);
        })
        .map(function (element) {
            return element.info + " | " + element.name;
        })
        .join("\n");
}
