<#-- @ftlvariable name="transferInfos" type="java.util.List<com.mediatoolkit.pareco.service.TransferRunner.TransferInfo>" -->
<style>
    table, tr, td, th {
        border: 1px solid gray;
        border-collapse: collapse;
    }
</style>
<h1>All Transfers</h1>
<table>
    <thead>
        <tr>
            <th>Action</th>
            <th>State</th>
            <th>Fail cause</th>
        </tr>
    </thead>
    <#list transferInfos as transferInfo>
        <tr>
            <td><a href="/transfers/${transferInfo.id}"><button>Show</button></a></td>
            <td>${transferInfo.state}</td>
            <td>${transferInfo.failCause!'---'}</td>
        </tr>
    </#list>
    <#if transferInfos?size == 0>
        <tr>
            <td colspan="3"><i>(no transfers)</i></td>
        </tr>
    </#if>
</table>
<hr>
<a href="/transfers/createNew"><button>Create new...</button></a>