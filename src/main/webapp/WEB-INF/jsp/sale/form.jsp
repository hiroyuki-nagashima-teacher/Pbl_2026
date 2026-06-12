<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="売上追加" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>売上追加</h1>
    <form method="post" action="${pageContext.request.contextPath}/sales/confirm" class="form-grid">
        <div>
            <label for="saleDate">売上日</label>
            <input id="saleDate" type="date" name="saleDate" value="${sale.saleDate}" required>
            <c:if test="${not empty errors.saleDate}"><div class="error"><c:out value="${errors.saleDate}" /></div></c:if>
        </div>
        <div>
            <label for="productId">商品</label>
            <select id="productId" name="productId" required>
                <option value="">選択してください</option>
                <c:forEach var="p" items="${products}">
                    <option value="${p.id}" ${sale.productId == p.id ? 'selected' : ''}><c:out value="${p.name}" />（${p.price}円）</option>
                </c:forEach>
            </select>
            <c:if test="${not empty errors.productId}"><div class="error"><c:out value="${errors.productId}" /></div></c:if>
        </div>
        <div>
            <label for="quantity">数量</label>
            <input id="quantity" type="number" min="1" name="quantity" value="${sale.quantity > 0 ? sale.quantity : ''}" required>
            <c:if test="${not empty errors.quantity}"><div class="error"><c:out value="${errors.quantity}" /></div></c:if>
        </div>
        <div>
            <label for="memo">メモ</label>
            <textarea id="memo" name="memo" maxlength="500"><c:out value="${sale.memo}" /></textarea>
            <c:if test="${not empty errors.memo}"><div class="error"><c:out value="${errors.memo}" /></div></c:if>
        </div>
        <div class="actions">
            <button type="submit">確認</button>
            <a class="button secondary" href="${pageContext.request.contextPath}/sales/search">戻る</a>
        </div>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
