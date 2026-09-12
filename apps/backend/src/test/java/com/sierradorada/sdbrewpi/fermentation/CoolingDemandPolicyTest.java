package com.sierradorada.sdbrewpi.fermentation;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class CoolingDemandPolicyTest {
    @Test
    void startsCoolingAboveUpperLimit() {
        assertThat(CoolingDemandPolicy.decide(ControlMode.AUTO, 18.5, 18.0, 0.35, false)).isTrue();
    }

    @Test
    void preservesDemandInsideNeutralBand() {
        assertThat(CoolingDemandPolicy.decide(ControlMode.AUTO, 18.1, 18.0, 0.35, true)).isTrue();
        assertThat(CoolingDemandPolicy.decide(ControlMode.AUTO, 18.1, 18.0, 0.35, false)).isFalse();
    }

    @Test
    void forcesDemandOffOutsideAutoMode() {
        assertThat(CoolingDemandPolicy.decide(ControlMode.OFF, 25.0, 18.0, 0.35, true)).isFalse();
        assertThat(CoolingDemandPolicy.decide(ControlMode.MANUAL, 25.0, 18.0, 0.35, true)).isFalse();
    }
}
