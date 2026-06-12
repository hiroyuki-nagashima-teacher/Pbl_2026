<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${edit ? '商品編集' : '商品追加'}" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1><c:out value="${pageTitle}" /></h1>
    <form method="post" action="${pageContext.request.contextPath}${edit ? '/products/edit' : '/products/confirm'}" class="form-grid">
        <c:if test="${edit}"><input type="hidden" name="id" value="${product.id}"></c:if>
        <div>
            <label for="name">商品名</label>
            <input id="name" name="name" maxlength="100" value="<c:out value='${product.name}' />" required>
            <c:if test="${not empty errors.name}"><div class="error"><c:out value="${errors.name}" /></div></c:if>
        </div>
        <div>
            <label for="categoryId">カテゴリー</label>
            <select id="categoryId" name="categoryId" required>
                <option value="">選択してください</option>
                <c:forEach var="c" items="${categories}">
                    <option value="${c.id}" ${product.categoryId == c.id ? 'selected' : ''}><c:out value="${c.name}" /></option>
                </c:forEach>
            </select>
            <c:if test="${not empty errors.categoryId}"><div class="error"><c:out value="${errors.categoryId}" /></div></c:if>
        </div>
        <div>
            <label for="price">価格</label>
            <input id="price" type="number" min="0" name="price" value="${product.price >= 0 ? product.price : ''}" required>
            <c:if test="${not empty errors.price}"><div class="error"><c:out value="${errors.price}" /></div></c:if>
        </div>
        <div>
            <label for="onSale">販売状態</label>
            <select id="onSale" name="onSale">
                <option value="true" ${product.onSale ? 'selected' : ''}>販売中</option>
                <option value="false" ${!product.onSale ? 'selected' : ''}>販売停止</option>
            </select>
        </div>
        <div class="actions">
            <button type="submit">${edit ? '保存' : '確認'}</button>
            <a class="button secondary" href="${pageContext.request.contextPath}/products">戻る</a>
        </div>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
