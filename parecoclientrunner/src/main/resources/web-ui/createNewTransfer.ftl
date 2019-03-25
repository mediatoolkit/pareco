<#-- @ftlvariable name="job" type="com.mediatoolkit.pareco.transfer.model.TransferJob" -->
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
    </style>
</head>
<h1>Create new transfer</h1>
<form action="/transfers/submitCreateNew" method="post">
    <table>
        <tr>
            <td>Mode</td>
            <td>
                <select name="mode">
                    <option value="download" <#if job?? && job.transferMode.name() == "download">checked</#if>>Download</option>
                    <option value="upload" <#if job?? && job.transferMode.name() == "upload">checked</#if>>Upload</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>Server url</td>
            <td><input type="text" name="server" value="<#if job??>${job.transferTask.serverInfo.toString()}</#if>"></td>
        </tr>
        <tr>
            <td>Remote dir</td>
            <td><input type="text" name="remoteDir" value="<#if job??>${job.transferTask.remoteRootDirectory}</#if>"></td>
        </tr>
        <tr>
            <td>Local dir</td>
            <td><input type="text" name="localDir" value="<#if job??>${job.transferTask.localRootDirectory}</#if>"></td>
        </tr>
    </table>
    <label>
        <input type="checkbox" name="advanced" onchange="handleAdvancedChange(this);">
        Show advanced
    </label>
    <table class="advanced hidden">
        <tr>
            <td>Include pattern</td>
            <td><input type="text" name="include"></td>
        </tr>
        <tr>
            <td>Exclude pattern</td>
            <td><input type="text" name="exclude"></td>
        </tr>
        <tr>
            <td>Num transfer connections</td>
            <td><input type="text" name="numConnections" value="10"></td>
        </tr>
        <tr>
            <td>TCP read timeout (millis)</td>
            <td><input type="text" name="timeout" value="120000"></td>
        </tr>
        <tr>
            <td>TCP connect timeout (millis)</td>
            <td><input type="text" name="connectTimeout" value="5000"></td>
        </tr>
        <tr>
            <td>Delete unexpected files</td>
            <td><input type="checkbox" name="deleteUnexpected"></td>
        </tr>
        <tr>
            <td>Chunk size</td>
            <td><input type="text" name="chunkSize" value="1M"></td>
        </tr>
        <tr>
            <td>Digest type</td>
            <td><input type="text" name="digestType" value="CRC_32"></td>
        </tr>
        <tr>
            <td>Skip digest check<br><strong>Note</strong>: will cause to full <br>re-transfer of existing files <br>if sizes do not match</td>
            <td><input type="checkbox" name="skipDigest"></td>
        </tr>
        <tr>
            <td>Remote server auth token</td>
            <td><input type="text" name="authToken"></td>
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
