package jp.co.pbl2026.sales.model;

import java.time.LocalDate;

public class SaleSearchCondition {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String staffName;
    private Integer amountFrom;
    private Integer amountTo;

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public Integer getAmountFrom() { return amountFrom; }
    public void setAmountFrom(Integer amountFrom) { this.amountFrom = amountFrom; }
    public Integer getAmountTo() { return amountTo; }
    public void setAmountTo(Integer amountTo) { this.amountTo = amountTo; }
}
