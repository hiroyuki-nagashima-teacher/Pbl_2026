<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="商品一覧" />
<%@ include file="../common/header.jspf" %>

<section class="panel">
    <h1>商品一覧</h1>
    <div class="actions">
        <c:if test="${sessionScope.loginAccount.manager}">
            <a class="button" href="${pageContext.request.contextPath}/products/new">商品追加</a>
        </c:if>
        <a class="button secondary" href="${pageContext.request.contextPath}/products/csv">
            <svg style="width:16px;height:16px;margin-right:6px;vertical-align:middle" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
            </svg>CSVエクスポート
        </a>
    </div>
    
    <div class="table-container">
        <table>
            <thead>
                <c:set var="nextOrder" value="${order == 'asc' ? 'desc' : 'asc'}" />
                <tr>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=product_id&order=${sortBy == 'product_id' ? nextOrder : 'asc'}">
                            ID <c:choose>
                                <c:when test="${sortBy == 'product_id' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'product_id' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=product_name&order=${sortBy == 'product_name' ? nextOrder : 'asc'}">
                            商品名 <c:choose>
                                <c:when test="${sortBy == 'product_name' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'product_name' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=category_name&order=${sortBy == 'category_name' ? nextOrder : 'asc'}">
                            カテゴリー <c:choose>
                                <c:when test="${sortBy == 'category_name' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'category_name' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=price&order=${sortBy == 'price' ? nextOrder : 'asc'}">
                            価格 <c:choose>
                                <c:when test="${sortBy == 'price' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'price' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=on_sale&order=${sortBy == 'on_sale' ? nextOrder : 'asc'}">
                            販売状態 <c:choose>
                                <c:when test="${sortBy == 'on_sale' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'on_sale' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?sortBy=updated_at&order=${sortBy == 'updated_at' ? nextOrder : 'asc'}">
                            更新日時 <c:choose>
                                <c:when test="${sortBy == 'updated_at' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'updated_at' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="p" items="${products}">
                <tr>
                    <td><c:out value="${p.id}" /></td>
                    <td><c:out value="${p.name}" /></td>
                    <td><c:out value="${p.categoryName}" /></td>
                    <td><fmt:formatNumber value="${p.price}" pattern="#,##0" />円</td>
                    <td><c:out value="${p.onSale ? '販売中' : '販売停止'}" /></td>
                    <td><c:out value="${p.formattedUpdatedAt}" /></td>
                    <td>
                        <c:if test="${sessionScope.loginAccount.manager}">
                            <a class="button secondary" href="${pageContext.request.contextPath}/products/edit?id=${p.id}">編集</a>
                            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/products/delete" onsubmit="return confirm('商品を削除しますか？');">
                                <input type="hidden" name="id" value="${p.id}">
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
