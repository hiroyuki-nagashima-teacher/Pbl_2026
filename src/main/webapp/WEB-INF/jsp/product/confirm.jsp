<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="商品追加確認" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>商品追加確認</h1>
    <table class="detail-table">
        <tr><th>商品名</th><td><c:out value="${product.name}" /></td></tr>
        <tr><th>カテゴリー</th><td><c:out value="${categoryName}" /></td></tr>
        <tr><th>価格</th><td><c:out value="${product.price}" />円</td></tr>
        <tr><th>販売状態</th><td><c:out value="${product.onSale ? '販売中' : '販売停止'}" /></td></tr>
    </table>
    <form method="post" action="${pageContext.request.contextPath}/products/create" class="actions">
        <input type="hidden" name="name" value="<c:out value='${product.name}' />">
        <input type="hidden" name="categoryId" value="${product.categoryId}">
        <input type="hidden" name="price" value="${product.price}">
        <input type="hidden" name="onSale" value="${product.onSale}">
        <button type="submit">登録</button>
        <button class="secondary" type="button" onclick="history.back()">戻る</button>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
