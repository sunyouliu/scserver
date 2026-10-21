package com.example.documentstore.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentNotificationParserTest {

    private final PaymentNotificationParser parser = new PaymentNotificationParser(new ObjectMapper());

    @Test
    void shouldParseJsonNotification() {
        PaymentNotification notification = parser.parse("""
                {
                  "Event":"xpay_goods_deliver_notify",
                  "OutTradeNo":"D202610170001",
                  "OpenId":"openid-1",
                  "WeChatPayInfo":{"MchOrderNo":"wx-order-1","Amount":100}
                }
                """);

        assertThat(notification.event()).isEqualTo("xpay_goods_deliver_notify");
        assertThat(notification.outTradeNo()).isEqualTo("D202610170001");
        assertThat(notification.openId()).isEqualTo("openid-1");
        assertThat(notification.platformOrderNo()).isEqualTo("wx-order-1");
        assertThat(notification.amountFen()).isEqualTo(100);
    }

    @Test
    void shouldParseXmlNotification() {
        PaymentNotification notification = parser.parse("""
                <xml>
                  <Event>xpay_goods_deliver_notify</Event>
                  <OutTradeNo>D202610170002</OutTradeNo>
                  <OpenId>openid-2</OpenId>
                  <MchOrderNo>wx-order-2</MchOrderNo>
                  <TotalFee>100</TotalFee>
                </xml>
                """);

        assertThat(notification.outTradeNo()).isEqualTo("D202610170002");
        assertThat(notification.openId()).isEqualTo("openid-2");
        assertThat(notification.platformOrderNo()).isEqualTo("wx-order-2");
        assertThat(notification.amountFen()).isEqualTo(100);
    }

    @Test
    void shouldRejectGoodsDeliveryWithoutOpenId() {
        assertThatThrownBy(() -> parser.parse("""
                {"Event":"xpay_goods_deliver_notify","OutTradeNo":"D202610170003"}
                """))
                .hasMessageContaining("OpenId");
    }
}
