package com.filebridge.dto;

public class PaymentConfigResponse {

    private String keyId;
    private int amountInr;
    private long amountPaise;
    private String currency;
    private String planName;

    public PaymentConfigResponse() {}

    public PaymentConfigResponse(String keyId, int amountInr, long amountPaise, String currency, String planName) {
        this.keyId = keyId;
        this.amountInr = amountInr;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.planName = planName;
    }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public int getAmountInr() { return amountInr; }
    public void setAmountInr(int amountInr) { this.amountInr = amountInr; }

    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) { this.amountPaise = amountPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
}
