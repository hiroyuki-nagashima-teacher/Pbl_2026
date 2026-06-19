<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="売上一覧" />
<%@ include file="../common/header.jspf" %>

<%
    // クエリパラメータの再構築用（sortBy, order を除外して他の検索条件を引き継ぐ）
    StringBuilder sb = new StringBuilder();
    java.util.Map<String, String[]> params = request.getParameterMap();
    for (java.util.Map.Entry<String, String[]> entry : params.entrySet()) {
        String key = entry.getKey();
        if (!"sortBy".equals(key) && !"order".equals(key)) {
            for (String val : entry.getValue()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(java.net.URLEncoder.encode(key, "UTF-8"))
                  .append("=")
                  .append(java.net.URLEncoder.encode(val, "UTF-8"));
            }
        }
    }
    String baseQuery = sb.toString();
    request.setAttribute("baseQuery", baseQuery.isEmpty() ? "" : baseQuery + "&");
%>

<section class="panel">
    <h1>売上一覧</h1>
    <div class="actions">
        <a class="button secondary" href="${pageContext.request.contextPath}/sales/search">売上検索</a>
        <a class="button" href="${pageContext.request.contextPath}/sales/new">売上追加</a>
        
        <!-- 現在の検索条件を引き継いだCSVエクスポート -->
        <a class="button secondary" href="${pageContext.request.contextPath}/sales/csv?${pageContext.request.queryString}">
            <svg style="width:16px;height:16px;margin-right:6px;vertical-align:middle" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
            </svg>CSVエクスポート
        </a>
    </div>
    
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <c:set var="nextOrder" value="${condition.order == 'asc' ? 'desc' : 'asc'}" />
                    <th>
                        <a href="${pageContext.request.contextPath}/sales?${baseQuery}sortBy=sale_date&order=${condition.sortBy == 'sale_date' ? nextOrder : 'asc'}">
                            売上日 <c:choose>
                                <c:when test="${condition.sortBy == 'sale_date' && condition.order == 'asc'}">▲</c:when>
                                <c:when test="${condition.sortBy == 'sale_date' && condition.order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/sales?${baseQuery}sortBy=product_name&order=${condition.sortBy == 'product_name' ? nextOrder : 'asc'}">
                            商品名 <c:choose>
                                <c:when test="${condition.sortBy == 'product_name' && condition.order == 'asc'}">▲</c:when>
                                <c:when test="${condition.sortBy == 'product_name' && condition.order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/sales?${baseQuery}sortBy=quantity&order=${condition.sortBy == 'quantity' ? nextOrder : 'asc'}">
                            数量 <c:choose>
                                <c:when test="${condition.sortBy == 'quantity' && condition.order == 'asc'}">▲</c:when>
                                <c:when test="${condition.sortBy == 'quantity' && condition.order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>販売時単価</th>
                    <th>
                        <a href="${pageContext.request.contextPath}/sales?${baseQuery}sortBy=amount&order=${condition.sortBy == 'amount' ? nextOrder : 'asc'}">
                            合計金額 <c:choose>
                                <c:when test="${condition.sortBy == 'amount' && condition.order == 'asc'}">▲</c:when>
                                <c:when test="${condition.sortBy == 'amount' && condition.order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/sales?${baseQuery}sortBy=staff_name&order=${condition.sortBy == 'staff_name' ? nextOrder : 'asc'}">
                            登録者 <c:choose>
                                <c:when test="${condition.sortBy == 'staff_name' && condition.order == 'asc'}">▲</c:when>
                                <c:when test="${condition.sortBy == 'staff_name' && condition.order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>更新日時</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${sales}">
                <c:set var="editable" value="${sessionScope.loginAccount.manager || sessionScope.loginAccount.id == s.registeredAccountId}" />
                <tr>
                    <td><c:out value="${s.saleDate}" /></td>
                    <td><c:out value="${s.productName}" /></td>
                    <td><c:out value="${s.quantity}" /></td>
                    <td><fmt:formatNumber value="${s.unitPrice}" pattern="#,##0" />円</td>
                    <td><fmt:formatNumber value="${s.totalAmount}" pattern="#,##0" />円</td>
                    <td><c:out value="${s.registeredStaffName}" /></td>
                    <td><c:out value="${s.updatedAt}" /></td>
                    <td>
                        <c:if test="${editable}">
                            <a class="button secondary" href="${pageContext.request.contextPath}/sales/edit?id=${s.id}">編集</a>
                            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/sales/delete" onsubmit="return confirm('売上を削除しますか？');">
                                <input type="hidden" name="id" value="${s.id}">
                                <button class="danger" type="submit">削除</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</section>
<%@ include file="../common/footer.jspf" %>
