package com.example.travel.domain.user.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NicknamePolicyTest {
    @Test
    void truncatesNicknameToTenCharacters() {
        assertThat(NicknamePolicy.truncate("12345678901")).isEqualTo("1234567890");
    }

    @Test
    void doesNotSplitSurrogatePairAtLimit() {
        assertThat(NicknamePolicy.truncate("123456789😀"))
                .isEqualTo("123456789");
    }
}
