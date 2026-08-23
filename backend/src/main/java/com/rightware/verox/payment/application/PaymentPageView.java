package com.rightware.verox.payment.application;

import java.util.List;

public record PaymentPageView(
    List<PaymentListItemView> items,
    int page,
    int size,
    long totalItems,
    int totalPages
) {
}
