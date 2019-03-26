<#-- @ftlvariable name="job" type="com.mediatoolkit.pareco.transfer.model.TransferJob" -->
<#-- @ftlvariable name="digestTypes" type="com.mediatoolkit.pareco.model.DigestType[]" -->
<#-- @ftlvariable name="logLevels" type="com.mediatoolkit.pareco.progress.TransferLoggingLevel[]" -->
<#-- @ftlvariable name="parameters" type="com.mediatoolkit.pareco.service.StartupParameters" -->

<#function chunkSize(chunkSizeBytes)>
<#-- @ftlvariable name="chunkSizeBytes" type="java.lang.Long" -->
    <#if chunkSizeBytes lt 1024>
        <#return chunkSizeBytes?c+"">
    </#if>
    <#assign chunkSizeKB = chunkSizeBytes / 1024>
    <#if chunkSizeKB lt 1024>
        <#return chunkSizeKB?c+"K">
    </#if>
    <#assign chunkSizeMB = chunkSizeKB / 1024>
    <#return chunkSizeMB?c+"M">
</#function>
<head>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
    <script src="/static/createForm.js"></script>
    <style>
        .hidden {
            display: none;
        }
        tr, td, table {
            border-top: 1px solid black;
            border-bottom: 1px solid black;
            border-collapse: collapse;
        }
        #dirContents {
            background-color: #d3d3d3;
            padding: 3px;
        }
        input[type=text] {
            width: 300px;
        }
        .error {
            color: #f64a38;
        }
        input[readonly=readonly] {
            background-color: #d1d1d1;
        }
    </style>
</head>
<a href="/transfers">Back to all transfers</a>
<hr>
<h1>Create new transfer</h1>
<form action="/transfers/submitCreateNew" method="post">
    <table>
        <tr>
            <td>Mode</td>
            <td>
                <select name="mode">
                    <option value="download" <#if job?? && job.transferMode.name() == "download">checked</#if>>
                        Download
                    </option>
                    <option value="upload" <#if job?? && job.transferMode.name() == "upload">checked</#if>>Upload
                    </option>
                </select>
            </td>
        </tr>
        <tr>
            <td>Server url</td>
            <td>
                <#assign server = parameters.isServerSet()?then(
                parameters.getServer().toUrl(),
                (job??)?then(job.transferTask.serverInfo.toUrl(), "")
                )>
                <input type="text" name="server" value="${server}" <#if parameters.isServerSet()>readonly="readonly"</#if>>
            </td>
        </tr>
        <tr>
            <td>Remote dir</td>
            <td>
                <#assign remoteDir = parameters.isRemoteDirSet()?then(
                parameters.getRemoteDir(), (job??)?then(job.transferTask.remoteRootDirectory, "")
                )>
                <input type="text" name="remoteDir" value="${remoteDir}" <#if parameters.isRemoteDirSet()>readonly="readonly"</#if>>
            </td>
        </tr>
        <tr>
            <td>Local dir</td>
            <td>
                <#assign localDir = parameters.isLocalDirSet()?then(
                parameters.getLocalDir(), (job??)?then(job.transferTask.localRootDirectory, "")
                )>
                <input type="text" name="localDir" value="${localDir}" <#if parameters.isLocalDirSet()>readonly="readonly"</#if>>
            </td>
        </tr>
        <tr>
            <td>Include glob pattern</td>
            <td><input type="text" name="include" value="<#if job??>${job.transferTask.include!''}</#if>"></td>
        </tr>
        <tr>
            <td>Exclude glob pattern</td>
            <td><input type="text" name="exclude" value="<#if job??>${job.transferTask.exclude!''}</#if>"></td>
        </tr>
        <tr>
            <td>Num transfer connections</td>
            <td><input type="text" name="numConnections"
                       value="${(job??)?then(job.transferTask.options.numTransferConnections, 10)?c}"></td>
        </tr>
        <tr>
            <td>TCP read timeout (millis)</td>
            <td><input type="text" name="timeout" value="${(job??)?then(job.transferTask.options.timeout, 120000)?c}">
            </td>
        </tr>
        <tr>
            <td>TCP connect timeout (millis)</td>
            <td><input type="text" name="connectTimeout"
                       value="${(job??)?then(job.transferTask.options.connectTimeout, 5000)?c}"></td>
        </tr>
        <tr>
            <td>Delete unexpected files</td>
            <td><input type="checkbox" name="deleteUnexpected"
                       <#if job?? && job.transferTask.options.deleteUnexpected>checked</#if>></td>
        </tr>
        <tr>
            <td>Chunk size</td>
            <td><input type="text" name="chunkSize"
                       value="${(job??)?then(chunkSize(job.transferTask.options.chunkSizeBytes), "1M")}"></td>
        </tr>
        <tr>
            <td>Digest type</td>
            <td>
                <#assign selectedDigestType = (job??)?then(job.transferTask.options.fileIntegrityOptions.digestType.name(), "CRC_32")>
                <select name="digestType">
                    <#list digestTypes as digestType>
                        <option <#if digestType.name()==selectedDigestType>selected</#if>>
                            ${digestType}
                        </option>
                    </#list>
                </select>
            </td>
        </tr>
        <tr>
            <td>Skip digest check<br><strong>Note</strong>: allows skip if file metadata is ok</td>
            <td><input type="checkbox" name="skipDigest"
                       <#if job?? && job.transferTask.options.fileIntegrityOptions.integrityCheckType.name() == "ONLY_METADATA">checked</#if>>
            </td>
        </tr>
        <tr>
            <td>Logging level</td>
            <td>
                <#assign selectedLogLevel = (job??)?then(job.loggingLevel.name(), "FILES")>
                <select name="logLevel">
                    <#list logLevels as logLevel>
                        <option <#if logLevel.name()==selectedLogLevel>selected</#if>>
                            ${logLevel}
                        </option>
                    </#list>
                </select>
            </td>
        </tr>
        <tr>
            <td>Remote server auth token</td>
            <td>
                <#assign authToken = parameters.isAuthTokenSet()?then(
                parameters.getAuthToken(), (job??)?then(job.transferTask.getAuthToken(), "")
                )>
                <input type="text" name="authToken" value="${authToken}" <#if parameters.isAuthTokenSet()>readonly="readonly"</#if>>
            </td>
        </tr>
    </table>
    <p>
        <input type="submit" value="Create">
    </p>
</form>

<button onclick="checkLocalDir();">Check local directory</button>
<button onclick="checkRemoteDir();">Check remote directory</button>

<p id="checkType"></p>
<p id="checkInfo"></p>
<pre id="dirContents"></pre>
