<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
 【模範解答解説: ログインページ (login.jsp)】
 アカウントの「ログインID + パスワード」を入力して認証を行うためのログイン画面です。
 
 ■ 設計・実装のポイント:
 1. パスワード入力の隠蔽（セキュリティ要件）:
    要件定義「5.2 セキュリティ: パスワード入力欄はマスク表示する」に従い、
    `<input type="password">` を使用して入力内容を非表示化しています。
 2. 脆弱性対策（XSS防止）と値の保持:
    ログイン失敗時に前回の入力ログインIDを保持する際、`<c:out value="${loginId}" />` を経由して
    HTMLエスケープを行うことで、XSS（クロスサイトスクリプティング）を防止しています。
 3. ブラウザ標準チェックの活用:
    `required` 属性を付与することで、サーバーへ無駄な空リクエストを送信する前に
    ブラウザ側で簡易的な空チェックを行います。
--%>
<c:set var="pageTitle" value="ログイン" />
<%@ include file="common/header.jspf" %>

<div class="login-page-wrapper">
    <div class="login-glow-bg"></div>
    <div class="login-card-container">
        <div class="login-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logo.png?v=9" alt="StockPilot Logo" class="login-logo">
            <h1 class="login-service-name">StockPilot</h1>
        </div>

        <%-- サーブレットで認証エラーとなった場合の警告メッセージ出力エリア --%>
        <c:if test="${not empty error}">
            <div class="alert login-alert"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" class="login-form">
            <div class="login-field">
                <label for="loginId" class="login-label">ログインID</label>
                <%-- 入力保持の際も確実にエスケープする --%>
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
