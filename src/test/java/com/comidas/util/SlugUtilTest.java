package com.comidas.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @Test
    void generaSlugsLimpios() {
        assertThat(SlugUtil.toSlug("Sweater Cuello Tortuga")).isEqualTo("sweater-cuello-tortuga");
        assertThat(SlugUtil.toSlug("Ñandú & Café 100%")).isEqualTo("nandu-cafe-100");
        assertThat(SlugUtil.toSlug("  Remera / Buzo -- Talle M  ")).isEqualTo("remera-buzo-talle-m");
        assertThat(SlugUtil.toSlug("---")).isEmpty();
        assertThat(SlugUtil.toSlug(null)).isEmpty();
    }
}
