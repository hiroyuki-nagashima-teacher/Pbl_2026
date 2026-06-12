<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="売上一覧" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>売上一覧</h1>
    <div class="actions">
        <a class="button secondary" href="${pageContext.request.contextPath}/sales/search">売上検索</a>
        <a class="button" href="${pageContext.request.contextPath}/sales/new">売上追加</a>
    </div>
    <table>
        <thead>
            <tr>
                <th>ID</th><th>売上日</th><th>商品名</th><th>数量</th><th>販売時単価</th><th>合計金額</th><th>登録者</th><th>メモ</th><th>更新日時</th><th>操作</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="s" items="${sales}">
            <c:set var="editable" value="${sessionScope.loginAccount.manager || sessionScope.loginAccount.id == s.registeredAccountId}" />
            <tr>
                <td><c:out value="${s.id}" /></td>
                <td><c:out value="${s.saleDate}" /></td>
                <td><c:out value="${s.productName}" /></td>
                <td><c:out value="${s.quantity}" /></td>
                <td><c:out value="${s.unitPrice}" />円</td>
                <td><c:out value="${s.totalAmount}" />円</td>
                <td><c:out value="${s.registeredStaffName}" /></td>
                <td><c:out value="${s.memo}" /></td>
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
</section>
<%@ include file="../common/footer.jspf" %>
