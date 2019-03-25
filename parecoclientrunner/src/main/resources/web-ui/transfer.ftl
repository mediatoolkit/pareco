<#-- @ftlvariable name="transferInfo" type="com.mediatoolkit.pareco.service.TransferRunner.TransferInfo" -->
<head>
    <meta name="transferId" data-transferId="${transferInfo.id}">
    <meta name="state" data-state="${transferInfo.state.name()}">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
    <script src="/static/taskRefresher.js"></script>
    <style>
        #log, #exceptionLog {
            background-color: #313131;
            padding: 3px;
        }
        #log {
            max-height: 80%;
            overflow-y: scroll;
        }
        #exceptionLog {
            font-weight: bold;
            overflow-x: auto;
        }
        .NONE, .DATE {
            color: #adadad;
        }
        .DELETE {
            color: #c9b300;
        }
        .SPEED {
            color: #1aa700;
        }
        .HIGHLIGHT {
            color: #f64a38;
        }
        .LITERAL, .THREAD {
            color: #a77af8;
        }
        .CHUNK {
            color: #ffffff;
        }
        .FILE {
            color: #00b5d7;
        }
        form {
            margin: 0;
        }
    </style>
</head>
<a href="/transfers">Back to all transfers</a>
<hr>
<table>
    <tr>
        <td>State: </td>
        <td id="transfer-state">${transferInfo.state.name()}</td>
        <td>
            <form action="/transfers/${transferInfo.id}/abort" method="post">
                <input type="submit" value="Abort">
            </form>
        </td>
        <td>
            <form action="/transfers/${transferInfo.id}/reCreate" method="post">
                <input type="submit" value="Re-Run">
            </form>
        </td>
        <td>
            <form action="/transfers/${transferInfo.id}/createNew" method="post">
                <input type="submit" value="Create new from...">
            </form>
        </td>
        <td>
            <label>
                <input type="checkbox" name="followLog" checked> Follow log
            </label>
        </td>
    </tr>
</table>
<pre id="log"></pre>
<pre id="exceptionLog"><span class="HIGHLIGHT" id="exceptionSpan">${(transferInfo.failCause?replace(":", ":\n  "))!''}</span></pre>


