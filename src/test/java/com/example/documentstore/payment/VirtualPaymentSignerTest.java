package com.example.documentstore.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualPaymentSignerTest {

    private final VirtualPaymentSigner signer = new VirtualPaymentSigner();

    @Test
    void shouldCreateWechatCompatibleSignatures() {
        String signData = "{\"offerId\":\"1\"}";

        assertThat(signer.createPaySignature("app-key", signData))
                .isEqualTo("afb03a2cc34c7a34137717eeaa944e4e360a93878928c6b6ff0b0912b2bd91a7");
        assertThat(signer.createUserSignature("session-key", signData))
                .isEqualTo("78827a75fd9a5a78403a3db17ce973911896ae4bd4a611ee09a096cfdf5dcb2d");
    }
}
