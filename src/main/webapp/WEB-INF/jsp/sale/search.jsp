<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
 【模範解答解説: 売上検索フォーム画面 (sale/search.jsp)】
 売上データの抽出を行うための各種条件（期間、商品、登録スタッフ名、金額範囲）を入力する画面です。
 
 ■ 設計・実装のポイント:
 1. GET送信による検索結果のURL共有化:
    本フォームは `method="get"` で `/sales` （一覧表示）へパラメータを送信します。
    検索結果をURLにクエリパラメータ（`?dateFrom=...`）として持たせることで、
    ページング遷移やブラウザの「戻る」ボタンを押した際にも検索状態が崩れないように設計されています。
 2. サーバー側バリデーションエラーの表示:
    「期間Fromが期間Toより後の日付の場合」や「金額Fromが金額Toより大きい場合」といった入力エラー情報（`errors`）を、
    該当する入力欄の直下に動的出力してユーザーへの迅速なフィードバックを行います。
--%>
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
