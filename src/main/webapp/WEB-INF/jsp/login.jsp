<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="ログイン" />
<%@ include file="common/header.jspf" %>

<div class="login-page-wrapper">
    <div class="login-glow-bg"></div>
    <div class="login-card-container">
        <div class="login-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logo.png?v=9" alt="StockPilot Logo" class="login-logo">
            <h1 class="login-service-name">StockPilot</h1>
        </div>

        <c:if test="${not empty error}">
            <div class="alert login-alert"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" class="login-form">
            <div class="login-field">
                <label for="loginId" class="login-label">ログインID</label>
                <input id="loginId" name="loginId" value="<c:out value='${loginId}' />" placeholder="ユーザーIDを入力してください" required autocomplete="username">
            </div>

            <div class="login-field">
                <label for="password" class="login-label">パスワード</label>
                <input id="password" type="password" name="password" placeholder="パスワードを入力してください" required autocomplete="current-password">
            </div>

            <button type="submit" class="login-btn">サインイン</button>
        </form>
    </div>
</div>

<%@ include file="common/footer.jspf" %>
