package com.sodosiro.domain.bingo.service;

import java.util.List;
import java.util.Set;

/** 3x3 빙고판의 가로3 + 세로3 + 대각2 = 8개 라인 완성 여부를 계산한다. position은 1~9. */
final class BingoLineCalculator {

    private static final List<Set<Integer>> LINES = List.of(
            Set.of(1, 2, 3), Set.of(4, 5, 6), Set.of(7, 8, 9),
            Set.of(1, 4, 7), Set.of(2, 5, 8), Set.of(3, 6, 9),
            Set.of(1, 5, 9), Set.of(3, 5, 7)
    );

    private BingoLineCalculator() {
    }

    static int countCompletedLines(Set<Integer> completedPositions) {
        return (int) LINES.stream().filter(completedPositions::containsAll).count();
    }
}
