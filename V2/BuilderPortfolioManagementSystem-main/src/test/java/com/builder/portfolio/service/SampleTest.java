package com.builder.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SampleTest {
    int x;

    @BeforeEach
    void setup() { x = 41; }

    @Test
    void addsOne() { assertEquals(42, x + 1); }
}
