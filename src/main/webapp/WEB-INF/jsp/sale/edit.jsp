<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="売上編集" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>売上編集</h1>
    <table class="detail-table">
        <tr><th>売上日</th><td><c:out value="${sale.saleDate}" /></td></tr>
        <tr><th>商品</th><td><c:out value="${sale.productName}" /></td></tr>
        <tr><th>数量</th><td><c:out value="${sale.quantity}" /></td></tr>
        <tr><th>販売時単価</th><td><c:out value="${sale.unitPrice}" />円</td></tr>
        <tr><th>合計金額</th><td><c:out value="${sale.totalAmount}" />円</td></tr>
        <tr><th>登録者</th><td><c:out value="${sale.registeredStaffName}" /></td></tr>
    </table>
    <form method="post" action="${pageContext.request.contextPath}/sales/edit" class="form-grid">
        <input type="hidden" name="id" value="${sale.id}">
        <div>
            <label for="memo">メモ</label>
            <textarea id="memo" name="memo" maxlength="500"><c:out value="${sale.memo}" /></textarea>
            <c:if test="${not empty errors.memo}"><div class="error"><c:out value="${errors.memo}" /></div></c:if>
        </div>
        <div class="actions">
            <button type="submit">保存</button>
            <a class="button secondary" href="${pageContext.request.contextPath}/sales">戻る</a>
        </div>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
