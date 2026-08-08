package com.rightware.verox.payment.application;

import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MoneyConverter moneyConverter;

    public PaymentService(PaymentRepository paymentRepository, MoneyConverter moneyConverter) {
        this.paymentRepository = paymentRepository;
        this.moneyConverter = moneyConverter;
    }

    @Transactional(readOnly = true)
    public PaymentView getForMerchant(UUID merchantId, String publicId) {
        Payment payment = paymentRepository.findByPublicIdAndMerchantId(publicId, merchantId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                "Payment was not found."
            ));

        return new PaymentView(
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            payment.getStatus().name(),
            moneyConverter.toMajorString(payment.getAmountMinor()),
            payment.getCurrency(),
            payment.getProvider(),
            payment.getConfirmedAt()
        );
    }
}
