package com.rightware.verox.evidence.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceContentHasherTest {

    private final EvidenceContentHasher hasher = new EvidenceContentHasher();

    @Test
    void hashesEvidenceContentDeterministically() {
        assertThat(hasher.sha256("VEROX evidence"))
            .isEqualTo("554c18f34d5a2be180e3d72f6ba69435907a24e894d88722f11cff223677369d");
    }

    @Test
    void rejectsBlankEvidenceContent() {
        assertThatThrownBy(() -> hasher.sha256("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }
}
