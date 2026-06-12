<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="商品一覧" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>商品一覧</h1>
    <c:if test="${sessionScope.loginAccount.manager}">
        <div class="actions">
            <a class="button" href="${pageContext.request.contextPath}/products/new">商品追加</a>
        </div>
    </c:if>
    <table>
        <thead>
            <tr>
                <th>ID</th><th>商品名</th><th>カテゴリー</th><th>価格</th><th>販売状態</th><th>更新日時</th><th>操作</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${products}">
            <tr>
                <td><c:out value="${p.id}" /></td>
                <td><c:out value="${p.name}" /></td>
                <td><c:out value="${p.categoryName}" /></td>
                <td><c:out value="${p.price}" />円</td>
                <td><c:out value="${p.onSale ? '販売中' : '販売停止'}" /></td>
                <td><c:out value="${p.updatedAt}" /></td>
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
</section>
<%@ include file="../common/footer.jspf" %>
