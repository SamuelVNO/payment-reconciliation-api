package io.github.samuelvno.paymentreconciliation.store;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StoreTest {

    @ParameterizedTest
    @CsvSource({
            "150.00, 0.0299, 4.48",  // 4.485 -> HALF_EVEN arredonda para o par (4.48); HALF_UP daria 4.49
            "89.90,  0.0299, 2.69",  // 2.68801
            "100.00, 0.0350, 3.50"
    })
    void calculatesExpectedFeeWithBankersRounding(String gross, String rate, String expectedFee) {
        Store store = new Store("Loja", "12345678000190", new BigDecimal(rate));

        assertThat(store.expectedFeeFor(new BigDecimal(gross))).isEqualByComparingTo(expectedFee);
    }

    @Test
    void bigDecimalEqualsConsidersScale() {
        // Armadilha clássica: equals compara valor E escala; compareTo compara só o valor.
        assertThat(new BigDecimal("10.0")).isNotEqualTo(new BigDecimal("10.00"));
        assertThat(new BigDecimal("10.0").compareTo(new BigDecimal("10.00"))).isZero();
    }
}
