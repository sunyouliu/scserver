package com.example.documentstore.payment;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.domain.OrderStatus;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.domain.VirtualOrder;
import com.example.documentstore.repository.UserAccountRepository;
import com.example.documentstore.repository.VirtualOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackService.class);
    private static final String GOODS_DELIVER_EVENT = "xpay_goods_deliver_notify";

    private final PaymentNotificationParser notificationParser;
    private final VirtualOrderRepository orderRepository;
    private final UserAccountRepository userRepository;

    public PaymentCallbackService(
            PaymentNotificationParser notificationParser,
            VirtualOrderRepository orderRepository,
            UserAccountRepository userRepository
    ) {
        this.notificationParser = notificationParser;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void handle(String body) {
        PaymentNotification notification = notificationParser.parse(body);
        if (!GOODS_DELIVER_EVENT.equals(notification.event())) {
            log.info("忽略未处理的微信虚拟支付事件：{}", notification.event());
            return;
        }

        VirtualOrder order = orderRepository.findByOutTradeNo(notification.outTradeNo()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "回调对应的支付订单不存在"));
        UserAccount user = userRepository.findById(order.getUserId()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "订单用户不存在"));

        if (!notification.openId().equals(user.getOpenId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_USER_MISMATCH", "支付回调用户与订单用户不一致");
        }
        if (notification.amountFen() != null && notification.amountFen() > 0
                && !notification.amountFen().equals(order.getAmountFen())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "支付回调金额与订单金额不一致");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }
        if (order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_NOT_PAYABLE", "订单已退款或已失效，不能标记为已支付");
        }

        order.markPaid(notification.platformOrderNo());
        log.info("虚拟支付订单 {} 已确认支付", order.getOutTradeNo());
    }
}
