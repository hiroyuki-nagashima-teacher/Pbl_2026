<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--
 【模範解答解説: 商品一覧画面 (product/list.jsp)】
 商品マスタに登録されている商品の一覧を表示し、ソート、ページング、および店長向けのアクション（追加・編集・削除）を提供します。
 
 ■ 設計・実装のポイント:
 1. 検索・ページ状態の維持（クエリパラメータの再構築）:
    Javaコード片（スクリプトリット）を用いて、現在のソート条件やページ番号を除外した「その他の検索パラメータ」を再構築（`baseQuery`）します。
    これにより、ページングやソート列をクリックした際にも、検索条件が欠落しないURLを生成しています。
 2. 権限による操作ボタンの非表示（UI要件）:
    要件定義「6.1 共通UI方針: 売上登録専用ユーザーには、商品追加・編集・削除ボタンを表示しない」に基づき、
    `${sessionScope.loginAccount.manager}` で囲んで店長のみにレンダリングします。
 3. 誤操作防止の削除確認（エラー・例外要件）:
    削除ボタンの `onsubmit` にJavaScriptの `confirm` メソッドを組み込み、誤クリックによる即時削除を防いでいます。
--%>
<c:set var="pageTitle" value="商品一覧" />
<%@ include file="../common/header.jspf" %>

<%
    // クエリパラメータの再構築用（page, sortBy, order を除外して他の検索条件を引き継ぐ）
    StringBuilder sb = new StringBuilder();
    java.util.Map<String, String[]> params = request.getParameterMap();
    for (java.util.Map.Entry<String, String[]> entry : params.entrySet()) {
        String key = entry.getKey();
        if (!"sortBy".equals(key) && !"order".equals(key) && !"page".equals(key)) {
            for (String val : entry.getValue()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(java.net.URLEncoder.encode(key, "UTF-8"))
                  .append("=")
                  .append(java.net.URLEncoder.encode(val, "UTF-8"));
            }
        }
    }
    String baseQuery = sb.toString();
    request.setAttribute("baseQuery", baseQuery.isEmpty() ? "" : baseQuery + "&");
%>

<section class="panel">
    <h1>商品一覧</h1>
    <div class="actions">
        <%-- 店長ロール（manager = true）のみ、商品追加ボタンを表示 --%>
        <c:if test="${sessionScope.loginAccount.manager}">
            <a class="button" href="${pageContext.request.contextPath}/products/new">商品追加</a>
        </c:if>
        <a class="button secondary" href="${pageContext.request.contextPath}/products/csv">
            <svg style="width:16px;height:16px;margin-right:6px;vertical-align:middle" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
            </svg>CSVエクスポート
        </a>
    </div>
    
    <div class="table-container">
        <table>
            <thead>
                <%-- ソート列がクリックされた際、現在のソート順（asc/desc）をトグル（反転）させるためのEL変数設定 --%>
                <c:set var="nextOrder" value="${order == 'asc' ? 'desc' : 'asc'}" />
                <tr>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=product_id&order=${sortBy == 'product_id' ? nextOrder : 'asc'}">
                            ID <c:choose>
                                <c:when test="${sortBy == 'product_id' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'product_id' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=product_name&order=${sortBy == 'product_name' ? nextOrder : 'asc'}">
                            商品名 <c:choose>
                                <c:when test="${sortBy == 'product_name' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'product_name' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=category_name&order=${sortBy == 'category_name' ? nextOrder : 'asc'}">
                            カテゴリー <c:choose>
                                <c:when test="${sortBy == 'category_name' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'category_name' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=price&order=${sortBy == 'price' ? nextOrder : 'asc'}">
                            価格 <c:choose>
                                <c:when test="${sortBy == 'price' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'price' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=on_sale&order=${sortBy == 'on_sale' ? nextOrder : 'asc'}">
                            販売状態 <c:choose>
                                <c:when test="${sortBy == 'on_sale' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'on_sale' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>
                        <a href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=updated_at&order=${sortBy == 'updated_at' ? nextOrder : 'asc'}">
                            更新日時 <c:choose>
                                <c:when test="${sortBy == 'updated_at' && order == 'asc'}">▲</c:when>
                                <c:when test="${sortBy == 'updated_at' && order == 'desc'}">▼</c:when>
                                <c:otherwise>↕</c:otherwise>
                            </c:choose>
                        </a>
                    </th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="p" items="${products}">
                <tr>
                    <td><c:out value="${p.id}" /></td>
                    <td><c:out value="${p.name}" /></td>
                    <td><c:out value="${p.categoryName}" /></td>
                    <%-- 金額のフォーマット表示（3桁カンマ区切り） --%>
                    <td><fmt:formatNumber value="${p.price}" pattern="#,##0" />円</td>
                    <td><c:out value="${p.onSale ? '販売中' : '販売停止'}" /></td>
                    <td><c:out value="${p.formattedUpdatedAt}" /></td>
                    <td>
                        <%-- 店長（manager = true）のみに編集および削除ボタンを表示（操作導線の非表示） --%>
                        <c:if test="${sessionScope.loginAccount.manager}">
                            <a class="button secondary" href="${pageContext.request.contextPath}/products/edit?id=${p.id}">編集</a>
                            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/products/delete" onsubmit="return confirm('商品を削除しますか？');">
                                <input type="hidden" name="id" value="${p.id}">
                                <button class="danger" type="submit">削除</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <!-- ページネーション UI -->
    <c:if test="${totalPages > 1}">
        <div class="pagination">
            <c:if test="${currentPage > 1}">
                <a class="page-link" href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=${sortBy}&order=${order}&page=${currentPage - 1}">&laquo; 前へ</a>
            </c:if>
            
            <%-- 総ページ数が大きい場合に備えて、前後2ページと最初・最後以外のページを「...」で省略表示するロジック --%>
            <c:forEach var="i" begin="1" end="${totalPages}">
                <c:choose>
                    <c:when test="${i == 1 || i == totalPages || (i >= currentPage - 2 && i <= currentPage + 2)}">
                        <c:choose>
                            <c:when test="${i == currentPage}">
                                <span class="page-item active">${i}</span>
                            </c:when>
                            <c:otherwise>
                                <a class="page-link" href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=${sortBy}&order=${order}&page=${i}">${i}</a>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:when test="${i == currentPage - 3 || i == currentPage + 3}">
                        <span class="page-ellipsis">...</span>
                    </c:when>
                </c:choose>
            </c:forEach>
            
            <c:if test="${currentPage < totalPages}">
                <a class="page-link" href="${pageContext.request.contextPath}/products?${baseQuery}sortBy=${sortBy}&order=${order}&page=${currentPage + 1}">次へ &raquo;</a>
            </c:if>
        </div>
        <div style="text-align: center; color: var(--text-muted); font-size: 13px; margin-top: 8px;">
            全 ${totalCount} 件中 ${currentPage} / ${totalPages} ページを表示
        </div>
    </c:if>
</section>
<%@ include file="../common/footer.jspf" %>
