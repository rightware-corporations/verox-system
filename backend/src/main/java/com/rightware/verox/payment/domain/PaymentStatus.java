package com.rightware.verox.payment.domain;

public enum PaymentStatus {
    PENDING,
    VERIFYING,
    CONFIRMED,
    REVIEW_REQUIRED,
    FAILED,
    EXPIRED
}
