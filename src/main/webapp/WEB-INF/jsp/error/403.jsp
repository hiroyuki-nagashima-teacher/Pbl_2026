<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="403" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>403 Forbidden</h1>
    <p>この操作を行う権限がありません。</p>
    <a class="button secondary" href="${pageContext.request.contextPath}/dashboard">ダッシュボードへ戻る</a>
</section>
<%@ include file="../common/footer.jspf" %>
