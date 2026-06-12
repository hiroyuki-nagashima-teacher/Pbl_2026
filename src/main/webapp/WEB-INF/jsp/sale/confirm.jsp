<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="売上追加確認" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>売上追加確認</h1>
    <table class="detail-table">
        <tr><th>売上日</th><td><c:out value="${sale.saleDate}" /></td></tr>
        <tr><th>商品</th><td><c:out value="${sale.productName}" /></td></tr>
        <tr><th>数量</th><td><c:out value="${sale.quantity}" /></td></tr>
        <tr><th>販売時単価</th><td><c:out value="${sale.unitPrice}" />円</td></tr>
        <tr><th>合計金額</th><td><c:out value="${sale.totalAmount}" />円</td></tr>
        <tr><th>メモ</th><td><c:out value="${sale.memo}" /></td></tr>
    </table>
    <form method="post" action="${pageContext.request.contextPath}/sales/create" class="actions">
        <input type="hidden" name="saleDate" value="${sale.saleDate}">
        <input type="hidden" name="productId" value="${sale.productId}">
        <input type="hidden" name="quantity" value="${sale.quantity}">
        <input type="hidden" name="memo" value="<c:out value='${sale.memo}' />">
        <button type="submit">登録</button>
        <button class="secondary" type="button" onclick="history.back()">戻る</button>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
