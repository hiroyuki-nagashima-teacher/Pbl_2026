<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="売上検索" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>売上検索</h1>
    <form method="get" action="${pageContext.request.contextPath}/sales" class="form-grid">
        <div>
            <label for="dateFrom">売上日の開始日</label>
            <input id="dateFrom" type="date" name="dateFrom" value="${condition.dateFrom}">
            <c:if test="${not empty errors.dateFrom}"><div class="error"><c:out value="${errors.dateFrom}" /></div></c:if>
        </div>
        <div>
            <label for="dateTo">売上日の終了日</label>
            <input id="dateTo" type="date" name="dateTo" value="${condition.dateTo}">
        </div>
        <div>
            <label for="productId">商品</label>
            <select id="productId" name="productId">
                <option value="">すべての商品</option>
                <c:forEach var="prod" items="${products}">
                    <option value="${prod.id}" ${condition.productId == prod.id ? 'selected' : ''}>
                        <c:out value="${prod.name}" /> (¥<c:out value="${prod.price}" />)
                    </option>
                </c:forEach>
            </select>
        </div>
        <div>
            <label for="staffName">スタッフ名</label>
            <input id="staffName" name="staffName" value="<c:out value='${condition.staffName}' />" placeholder="スタッフ名を入力" autocomplete="off" data-staff-input>
        </div>
        <div>
            <label for="amountFrom">合計金額の下限</label>
            <input id="amountFrom" type="number" min="0" name="amountFrom" value="${condition.amountFrom}">
            <c:if test="${not empty errors.amountFrom}"><div class="error"><c:out value="${errors.amountFrom}" /></div></c:if>
        </div>
        <div>
            <label for="amountTo">合計金額の上限</label>
            <input id="amountTo" type="number" min="0" name="amountTo" value="${condition.amountTo}">
        </div>
        <div class="actions">
            <button type="submit">検索</button>
            <a class="button secondary" href="${pageContext.request.contextPath}/sales">売上一覧へ戻る</a>
        </div>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
