package com.filebridge.dto;

public class CreateOrderResponse {

    private String orderId;
    private long amount; // in paise
    private String currency;
    private String keyId;
    private String planName;

    public CreateOrderResponse() {}

    public CreateOrderResponse(String orderId, long amount, String currency, String keyId, String planName) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.keyId = keyId;
        this.planName = planName;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
}
