<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="アカウント一覧" />
<%@ include file="../common/header.jspf" %>
<section class="panel">
    <h1>アカウント一覧</h1>
    <div class="actions">
        <a class="button" href="${pageContext.request.contextPath}/accounts/new">アカウント追加</a>
    </div>
    <table>
        <thead>
            <tr><th>ID</th><th>ログインID</th><th>スタッフ名</th><th>ロール</th><th>更新日時</th><th>操作</th></tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${accounts}">
            <tr>
                <td><c:out value="${a.id}" /></td>
                <td><c:out value="${a.loginId}" /></td>
                <td><c:out value="${a.staffName}" /></td>
                <td><c:out value="${a.manager ? '店長' : '売上登録専用'}" /></td>
                <td><c:out value="${a.updatedAt}" /></td>
                <td>
                    <a class="button secondary" href="${pageContext.request.contextPath}/accounts/edit?id=${a.id}">編集</a>
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/accounts/delete" onsubmit="return confirm('アカウントを削除しますか？');">
                        <input type="hidden" name="id" value="${a.id}">
                        <button class="danger" type="submit">削除</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</section>
<%@ include file="../common/footer.jspf" %>
