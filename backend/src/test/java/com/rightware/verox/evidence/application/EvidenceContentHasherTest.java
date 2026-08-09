package com.rightware.verox.evidence.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceContentHasherTest {

    private final EvidenceContentHasher hasher = new EvidenceContentHasher();

    @Test
    void hashesEvidenceContentDeterministically() {
        assertThat(hasher.sha256("VEROX evidence"))
            .isEqualTo("bd5a7f0b689e615840a105844b97dc7e0a8311120f75b55f129cb5ea9ae71ee9");
    }

    @Test
    void rejectsBlankEvidenceContent() {
        assertThatThrownBy(() -> hasher.sha256("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }
}
