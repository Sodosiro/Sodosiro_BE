package com.sodosiro.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR 자동 코드리뷰 동작 확인용 샘플 테스트.
 * Spring 컨텍스트 없이 순수 JUnit 만 사용한다.
 */
class SampleTest {

    @Test
    @DisplayName("문자열을 뒤집으면 역순 문자열이 반환된다")
    void reverseString() {
        String input = "sodosiro";

        String reversed = new StringBuilder(input).reverse().toString();

        assertEquals("orisodos", reversed);
    }

    @Test
    @DisplayName("두 정수를 더하면 합이 반환된다")
    void addTwoNumbers() {
        int sum = add(2, 3);

        assertEquals(5, sum);
    }

    @Test
    @DisplayName("양수 판별이 정상 동작한다")
    void isPositive() {
        assertTrue(add(1, 1) > 0);
    }

    private int add(int a, int b) {
        return a + b;
    }
}
