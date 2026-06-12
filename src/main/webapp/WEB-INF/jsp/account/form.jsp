<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${edit ? 'アカウント編集' : 'アカウント追加'}" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1><c:out value="${pageTitle}" /></h1>
    <form method="post" action="${pageContext.request.contextPath}${edit ? '/accounts/edit' : '/accounts/new'}" class="form-grid">
        <c:if test="${edit}"><input type="hidden" name="id" value="${account.id}"></c:if>
        <div>
            <label for="loginId">ログインID</label>
            <input id="loginId" name="loginId" maxlength="50" value="<c:out value='${account.loginId}' />" required>
            <c:if test="${not empty errors.loginId}"><div class="error"><c:out value="${errors.loginId}" /></div></c:if>
        </div>
        <div>
            <label for="staffName">スタッフ名</label>
            <input id="staffName" name="staffName" maxlength="50" value="<c:out value='${account.staffName}' />" required>
            <c:if test="${not empty errors.staffName}"><div class="error"><c:out value="${errors.staffName}" /></div></c:if>
        </div>
        <div>
            <label for="password">パスワード<c:if test="${edit}">（変更する場合のみ入力）</c:if></label>
            <input id="password" type="password" name="password" minlength="8" ${edit ? '' : 'required'}>
            <c:if test="${not empty errors.password}"><div class="error"><c:out value="${errors.password}" /></div></c:if>
        </div>
        <div>
            <label for="role">ロール</label>
            <select id="role" name="role" required>
                <option value="">選択してください</option>
                <option value="MANAGER" ${account.role == 'MANAGER' ? 'selected' : ''}>店長</option>
                <option value="STAFF" ${account.role == 'STAFF' ? 'selected' : ''}>売上登録専用</option>
            </select>
            <c:if test="${not empty errors.role}"><div class="error"><c:out value="${errors.role}" /></div></c:if>
        </div>
        <div class="actions">
            <button type="submit">保存</button>
            <a class="button secondary" href="${pageContext.request.contextPath}/accounts">戻る</a>
        </div>
    </form>
</section>
<%@ include file="../common/footer.jspf" %>
