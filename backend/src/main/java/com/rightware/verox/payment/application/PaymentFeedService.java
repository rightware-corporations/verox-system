package com.rightware.verox.payment.application;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentFeedService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final EnumSet<PaymentStatus> ATTENTION_STATUSES = EnumSet.of(
        PaymentStatus.PENDING,
        PaymentStatus.REVIEW_REQUIRED
    );

    private final PaymentRepository paymentRepository;
    private final PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;
    private final MoneyConverter moneyConverter;

    public PaymentFeedService(
        PaymentRepository paymentRepository,
        PilotManualPaymentAcceptanceRepository manualAcceptanceRepository,
        MoneyConverter moneyConverter
    ) {
        this.paymentRepository = paymentRepository;
        this.manualAcceptanceRepository = manualAcceptanceRepository;
        this.moneyConverter = moneyConverter;
    }

    @Transactional(readOnly = true)
    public PaymentPageView list(
        MerchantPrincipal principal,
        String statusValue,
        Boolean attentionRequired,
        Integer pageValue,
        Integer sizeValue
    ) {
        int page = pageValue == null ? 0 : pageValue;
        int size = sizeValue == null ? DEFAULT_SIZE : sizeValue;
        if (page < 0) {
            throw invalidPagination("page must be greater than or equal to 0.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw invalidPagination("size must be between 1 and 100.");
        }

        PaymentStatus status = parseStatus(statusValue);
        if (Boolean.TRUE.equals(attentionRequired) && status != null && !ATTENTION_STATUSES.contains(status)) {
            return new PaymentPageView(java.util.List.of(), page, size, 0, 0);
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> payments;
        if (Boolean.TRUE.equals(attentionRequired)) {
            var statuses = status == null ? ATTENTION_STATUSES : EnumSet.of(status);
            payments = paymentRepository.findAttentionRequired(
                principal.merchantId(),
                principal.environment(),
                statuses,
                pageable
            );
        } else if (status != null) {
            payments = paymentRepository.findAllByMerchantIdAndEnvironmentAndStatus(
                principal.merchantId(),
                principal.environment(),
                status,
                pageable
            );
        } else {
            payments = paymentRepository.findAllByMerchantIdAndEnvironment(
                principal.merchantId(),
                principal.environment(),
                pageable
            );
        }

        var ids = payments.getContent().stream().map(Payment::getId).toList();
        Map<UUID, PilotManualPaymentAcceptance> acceptances = ids.isEmpty()
            ? Map.of()
            : manualAcceptanceRepository
                .findAllByMerchantIdAndPaymentIdIn(principal.merchantId(), ids)
                .stream()
                .collect(Collectors.toMap(
                    PilotManualPaymentAcceptance::getPaymentId,
                    Function.identity()
                ));

        var items = payments.getContent().stream()
            .map(payment -> toView(payment, acceptances.get(payment.getId())))
            .toList();

        return new PaymentPageView(
            items,
            payments.getNumber(),
            payments.getSize(),
            payments.getTotalElements(),
            payments.getTotalPages()
        );
    }

    private PaymentListItemView toView(
        Payment payment,
        PilotManualPaymentAcceptance acceptance
    ) {
        String effectiveStatus = payment.getStatus() == PaymentStatus.CONFIRMED
            ? PaymentStatus.CONFIRMED.name()
            : acceptance != null
                ? "MANUALLY_ACCEPTED"
                : payment.getStatus().name();
        boolean attentionRequired = ATTENTION_STATUSES.contains(payment.getStatus()) && acceptance == null;

        return new PaymentListItemView(
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            payment.getCheckoutSession().getDescription(),
            payment.getStatus().name(),
            effectiveStatus,
            attentionRequired,
            moneyConverter.toMajorString(payment.getAmountMinor()),
            payment.getCurrency(),
            payment.getProvider(),
            payment.getCreatedAt(),
            payment.getConfirmedAt(),
            acceptance == null ? null : acceptance.getAcceptedAt()
        );
    }

    private PaymentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_STATUS",
                "Payment status is invalid."
            );
        }
    }

    private ApiException invalidPagination(String message) {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_PAGINATION",
            message
        );
    }
}
