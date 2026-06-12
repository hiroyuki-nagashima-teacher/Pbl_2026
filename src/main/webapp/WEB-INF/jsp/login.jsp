<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="ログイン" />
<%@ include file="common/header.jspf" %>
<section class="panel login-panel">
    <h1>ログイン</h1>
    <c:if test="${not empty error}">
        <div class="alert"><c:out value="${error}" /></div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/login" class="form-grid">
        <div>
            <label for="loginId">ログインID</label>
            <input id="loginId" name="loginId" value="<c:out value='${loginId}' />" required>
        </div>
        <div>
            <label for="password">パスワード</label>
            <input id="password" type="password" name="password" required>
        </div>
        <div><button type="submit">ログイン</button></div>
    </form>
</section>
<%@ include file="common/footer.jspf" %>
