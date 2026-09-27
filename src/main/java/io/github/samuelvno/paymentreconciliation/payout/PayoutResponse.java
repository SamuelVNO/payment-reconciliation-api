package io.github.samuelvno.paymentreconciliation.payout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayoutResponse(UUID id, String externalId, String orderExternalId, BigDecimal grossAmount,
                             BigDecimal feeAmount, BigDecimal netAmount, LocalDate paidAt) {

    static PayoutResponse from(Payout payout) {
        return new PayoutResponse(payout.getId(), payout.getExternalId(), payout.getOrderExternalId(),
                payout.getGrossAmount(), payout.getFeeAmount(), payout.getNetAmount(), payout.getPaidAt());
    }
}
