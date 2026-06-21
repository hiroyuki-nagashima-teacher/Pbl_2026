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
                        <a href="${pageContext.request.contextPath}/sales?dateFrom=${todayDate}&dateTo=${todayDate}"
                            class="kpi-card">
                            <div class="kpi-title">本日の売上</div>
                            <div class="kpi-value">
                                ¥
                                <fmt:formatNumber value="${todaySalesAmount}" pattern="#,##0" />
                            </div>
                            <div class="kpi-desc">今日1日の売上合計額</div>
                        </a>
                        <a href="${pageContext.request.contextPath}/sales?dateFrom=${monthStartDate}&dateTo=${todayDate}"
                            class="kpi-card">
                            <div class="kpi-title">今月の売上</div>
                            <div class="kpi-value">
                                ¥
                                <fmt:formatNumber value="${monthSalesAmount}" pattern="#,##0" />
                            </div>
                            <div class="kpi-desc">当月1日からの累計売上額</div>
                        </a>
                    </div>

                    <!-- グラフエリア -->
                    <div class="chart-panel" style="margin-top: 24px;">
                        <h2
                            style="font-family: 'Outfit', sans-serif; font-size: 18px; margin-top: 0; margin-bottom: 16px; font-weight: 600;">
                            直近7日間の売上推移</h2>
                        <div style="position: relative; height: 320px; width: 100%;">
                            <canvas id="salesChart"></canvas>
                        </div>
                    </div>

                    <!-- カテゴリ売上比率 & 商品売上ランキング -->
                    <div class="dashboard-grid-2col" style="margin-top: 24px;">
                        <!-- 円グラフ -->
                        <div class="chart-panel">
                            <h2
                                style="font-family: 'Outfit', sans-serif; font-size: 18px; margin-top: 0; margin-bottom: 16px; font-weight: 600;">
                                カテゴリごとの売上比率</h2>
                            <div
                                style="position: relative; height: 320px; width: 100%; display: flex; justify-content: center; align-items: center;">
                                <canvas id="categoryChart"></canvas>
                            </div>
                        </div>

                        <!-- ランキング -->
                        <div class="chart-panel">
                            <h2
                                style="font-family: 'Outfit', sans-serif; font-size: 18px; margin-top: 0; margin-bottom: 16px; font-weight: 600;">
                                商品売上ランキング</h2>
                            <div style="overflow-x: auto; width: 100%;">
                                <table style="width: 100%; border-collapse: collapse;">
                                    <thead>
                                        <tr>
                                            <th style="width: 60px; text-align: center;">順位</th>
                                            <th>商品名</th>
                                            <th style="text-align: right;">売上金額</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${topProducts}" varStatus="status">
                                            <tr>
                                                <td style="text-align: center; font-weight: bold;">
                                                    <span class="rank-badge rank-${status.count}">${status.count}</span>
                                                </td>
                                                <td>
                                                    <c:out value="${item.productName}" />
                                                </td>
                                                <td style="text-align: right; font-weight: 500;">
                                                    ¥
                                                    <fmt:formatNumber value="${item.totalAmount}" pattern="#,##0" />
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty topProducts}">
                                            <tr>
                                                <td colspan="3"
                                                    style="text-align: center; color: var(--text-muted); padding: 24px 0;">
                                                    データがありません</td>
                                            </tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- Chart.jsのロードと描画 -->
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <script>
                    document.addEventListener("DOMContentLoaded", function () {
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
                                            label: function (context) {
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
                                            callback: function (value) {
                                                return '¥' + value.toLocaleString();
                                            }
                                        },
                                        min: 0
                                    }
                                }
                            }
                        });

                        // カテゴリごとの売上比率（円グラフ）の描画
                        const categoryCanvas = document.getElementById('categoryChart');
                        if (categoryCanvas) {
                            const categoryCtx = categoryCanvas.getContext('2d');
                            const categoryLabels = [
                                <c:forEach var="item" items="${categorySales}" varStatus="status">
                                    "${item.categoryName}"<c:if test="${not status.last}">,</c:if>
                                </c:forEach>
                            ];
                            const categoryData = [
                                <c:forEach var="item" items="${categorySales}" varStatus="status">
                                    ${item.totalAmount}<c:if test="${not status.last}">,</c:if>
                                </c:forEach>
                            ];

                            if (categoryLabels.length === 0) {
                                categoryLabels.push("データなし");
                                categoryData.push(0);
                            }

                            const colors = [
                                '#6366f1', // Indigo (Primary)
                                '#8b5cf6', // Violet (Secondary)
                                '#d946ef', // Fuchsia
                                '#06b6d4', // Cyan
                                '#ec4899', // Pink
                                '#3b82f6', // Blue
                                '#14b8a6'  // Teal
                            ];

                            const totalSum = categoryData.reduce((a, b) => a + b, 0);

                            new Chart(categoryCtx, {
                                type: 'pie',
                                data: {
                                    labels: categoryLabels,
                                    datasets: [{
                                        data: categoryData,
                                        backgroundColor: colors.slice(0, categoryLabels.length),
                                        borderWidth: 2,
                                        borderColor: '#ffffff'
                                    }]
                                },
                                options: {
                                    responsive: true,
                                    maintainAspectRatio: false,
                                    plugins: {
                                        legend: {
                                            position: 'bottom',
                                            labels: {
                                                color: '#64748b',
                                                font: {
                                                    family: 'Plus Jakarta Sans',
                                                    size: 12
                                                },
                                                padding: 16,
                                                generateLabels: function (chart) {
                                                    const data = chart.data;
                                                    if (data.labels.length && data.datasets.length) {
                                                        return data.labels.map(function (label, i) {
                                                            const meta = chart.getDatasetMeta(0);
                                                            const ds = data.datasets[0];
                                                            const val = ds.data[i];
                                                            const percentage = totalSum > 0 ? ((val / totalSum) * 100).toFixed(1) : 0;
                                                            return {
                                                                text: label + ' (' + percentage + '%)',
                                                                fillStyle: ds.backgroundColor[i],
                                                                strokeStyle: ds.borderColor,
                                                                lineWidth: ds.borderWidth,
                                                                hidden: isNaN(ds.data[i]) || meta.data[i].hidden,
                                                                index: i
                                                            };
                                                        });
                                                    }
                                                    return [];
                                                }
                                            }
                                        },
                                        tooltip: {
                                            backgroundColor: '#1e293b',
                                            titleColor: '#f8fafc',
                                            bodyColor: '#f8fafc',
                                            padding: 12,
                                            cornerRadius: 8,
                                            callbacks: {
                                                label: function (context) {
                                                    let label = context.label || '';
                                                    if (label) {
                                                        label += ': ';
                                                    }
                                                    if (context.parsed !== null) {
                                                        const val = context.parsed;
                                                        const percentage = totalSum > 0 ? ((val / totalSum) * 100).toFixed(1) : 0;
                                                        label += '¥' + val.toLocaleString() + ' (' + percentage + '%)';
                                                    }
                                                    return label;
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
                </script>

                <%@ include file="common/footer.jspf" %>