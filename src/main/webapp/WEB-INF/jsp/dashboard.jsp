<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="ダッシュボード" />
<%@ include file="common/header.jspf" %>

<section class="panel">
    <h1>ダッシュボード</h1>
    <p class="readonly">ログイン後のトップ画面です。本日の売上状況および直近の推移をご確認いただけます。</p>

    <!-- KPIカードエリア -->
    <div class="dashboard-grid">
        <div class="kpi-card">
            <div class="kpi-title">本日の売上</div>
            <div class="kpi-value">
                ¥<fmt:formatNumber value="${todaySalesAmount}" pattern="#,##0" />
            </div>
            <div class="kpi-desc">今日1日の売上合計額</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">今月の売上</div>
            <div class="kpi-value">
                ¥<fmt:formatNumber value="${monthSalesAmount}" pattern="#,##0" />
            </div>
            <div class="kpi-desc">当月1日からの累計売上額</div>
        </div>
    </div>

    <!-- グラフエリア -->
    <div class="chart-panel" style="margin-top: 24px;">
        <h2 style="font-family: 'Outfit', sans-serif; font-size: 18px; margin-top: 0; margin-bottom: 16px; font-weight: 600;">直近7日間の売上推移</h2>
        <div style="position: relative; height: 320px; width: 100%;">
            <canvas id="salesChart"></canvas>
        </div>
    </div>
</section>

<!-- Chart.jsのロードと描画 -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
document.addEventListener("DOMContentLoaded", function() {
    const canvas = document.getElementById('salesChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    // 日付と金額データの作成 (JSTLを用いてJS配列を構築)
    const labels = [
        <c:forEach var="item" items="${recentSales}" varStatus="status">
            "${item.saleDate}"<c:if test="${not status.last}">,</c:if>
        </c:forEach>
    ];
    const data = [
        <c:forEach var="item" items="${recentSales}" varStatus="status">
            ${item.totalAmount}<c:if test="${not status.last}">,</c:if>
        </c:forEach>
    ];

    // デフォルトでデータが空の場合はダミーか0を表示
    if (labels.length === 0) {
        labels.push("データなし");
        data.push(0);
    }

    // グラデーションの作成
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
    gradient.addColorStop(1, 'rgba(99, 102, 241, 0.0)');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '売上金額 (円)',
                data: data,
                borderColor: '#6366f1',
                borderWidth: 3,
                backgroundColor: gradient,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#6366f1',
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: '#1e293b',
                    titleColor: '#f8fafc',
                    bodyColor: '#f8fafc',
                    padding: 12,
                    cornerRadius: 8,
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return '売上額: ¥' + context.parsed.y.toLocaleString();
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#64748b',
                        font: {
                            family: 'Plus Jakarta Sans'
                        }
                    }
                },
                y: {
                    grid: {
                        color: '#e2e8f0'
                    },
                    ticks: {
                        color: '#64748b',
                        font: {
                            family: 'Plus Jakarta Sans'
                        },
                        callback: function(value) {
                            return '¥' + value.toLocaleString();
                        }
                    },
                    min: 0
                }
            }
        }
    });
});
</script>

<%@ include file="common/footer.jspf" %>
