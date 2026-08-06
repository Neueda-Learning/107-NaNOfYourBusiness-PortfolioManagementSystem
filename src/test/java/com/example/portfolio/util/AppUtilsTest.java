package com.example.portfolio.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUtilsTest {

    @Test
    void canBeInstantiated() {
        assertThat(new AppUtils()).isNotNull();
    }
}

