package com.example.documentstore.payment;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.domain.OrderStatus;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.domain.VirtualOrder;
import com.example.documentstore.payment.dto.CreatePaymentOrderRequest;
import com.example.documentstore.payment.dto.DownloadLinkResponse;
import com.example.documentstore.payment.dto.PaymentOrderResponse;
import com.example.documentstore.repository.DocumentAssetRepository;
import com.example.documentstore.repository.UserAccountRepository;
import com.example.documentstore.repository.VirtualOrderRepository;
import com.example.documentstore.security.SessionKeyCryptoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VirtualPaymentService {

    private static final Logger log = LoggerFactory.getLogger(VirtualPaymentService.class);
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final AppProperties properties;
    private final UserAccountRepository userRepository;
    private final DocumentAssetRepository documentRepository;
    private final VirtualOrderRepository orderRepository;
    private final SessionKeyCryptoService sessionKeyCryptoService;
    private final VirtualPaymentSigner signer;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public VirtualPaymentService(
            AppProperties properties,
            UserAccountRepository userRepository,
            DocumentAssetRepository documentRepository,
            VirtualOrderRepository orderRepository,
            SessionKeyCryptoService sessionKeyCryptoService,
            VirtualPaymentSigner signer,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.orderRepository = orderRepository;
        this.sessionKeyCryptoService = sessionKeyCryptoService;
        this.signer = signer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, CreatePaymentOrderRequest request) {
        validatePaymentConfiguration();
        DocumentAsset document = findDocument(request.productId());
        UserAccount user = findUser(userId);

        VirtualOrder paidOrder = orderRepository
                .findTopByUserIdAndDocumentIdAndStatusOrderByPaidAtDesc(userId, document.getId(), OrderStatus.PAID)
                .orElse(null);
        if (paidOrder != null) {
            return new PaymentOrderResponse("PAID", paidOrder.getOutTradeNo(), null, document.getDownloadLink());
        }

        VirtualOrder order = new VirtualOrder();
        order.setUserId(userId);
        order.setDocumentId(document.getId());
        order.setAmountFen(resolveAmountFen(document));
        order.setStatus(OrderStatus.PENDING);
        saveWithUniqueTradeNo(order);

        String signData = createSignData(order);
        String sessionKey = sessionKeyCryptoService.decrypt(user.getEncryptedSessionKey());
        Map<String, Object> paymentParams = new LinkedHashMap<>();
        paymentParams.put("signData", signData);
        paymentParams.put("paySig", signer.createPaySignature(properties.virtualPayment().appKey(), signData));
        paymentParams.put("signature", signer.createUserSignature(sessionKey, signData));
        paymentParams.put("mode", properties.virtualPayment().mode());

        return new PaymentOrderResponse("PENDING", order.getOutTradeNo(), paymentParams, null);
    }

    @Transactional(readOnly = true)
    public DownloadLinkResponse getDownloadLink(Long userId, String outTradeNo) {
        VirtualOrder order = orderRepository.findByOutTradeNoAndUserId(outTradeNo, userId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "支付订单不存在"));
        if (order.getStatus() != OrderStatus.PAID) {
            return new DownloadLinkResponse(order.getStatus().name(), outTradeNo, null);
        }
        DocumentAsset document = findDocument(order.getDocumentId());
        return new DownloadLinkResponse("PAID", outTradeNo, document.getDownloadLink());
    }

    private String createSignData(VirtualOrder order) {
        Map<String, Object> signData = new LinkedHashMap<>();
        signData.put("offerId", properties.virtualPayment().offerId());
        signData.put("buyQuantity", 1);
        signData.put("env", properties.virtualPayment().env());
        signData.put("currencyType", properties.virtualPayment().currencyType());
        signData.put("productId", order.getDocumentId());
        signData.put("goodsPrice", order.getAmountFen());
        signData.put("outTradeNo", order.getOutTradeNo());
        signData.put("attach", order.getDocumentId());
        try {
            return objectMapper.writeValueAsString(signData);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("虚拟支付参数序列化失败", exception);
        }
    }

    private void saveWithUniqueTradeNo(VirtualOrder order) {
        for (int attempt = 0; attempt < 3; attempt += 1) {
            order.setOutTradeNo(generateOutTradeNo());
            try {
                orderRepository.save(order);
                return;
            } catch (DataIntegrityViolationException exception) {
                log.warn("支付单号 {} 冲突，正在重新生成", order.getOutTradeNo());
            }
        }
        throw new IllegalStateException("无法生成唯一的支付订单号");
    }

    private String generateOutTradeNo() {
        StringBuilder randomPart = new StringBuilder(10);
        for (int index = 0; index < 10; index += 1) {
            randomPart.append(RANDOM_CHARS[secureRandom.nextInt(RANDOM_CHARS.length)]);
        }
        return "D" + ORDER_TIME_FORMAT.format(LocalDateTime.now(ZoneOffset.UTC)) + randomPart;
    }

    private int resolveAmountFen(DocumentAsset document) {
        Integer priceFen = document.getPriceFen();
        return priceFen != null && priceFen > 0 ? priceFen : properties.virtualPayment().priceFen();
    }

    private void validatePaymentConfiguration() {
        if (isBlank(properties.virtualPayment().offerId()) || isBlank(properties.virtualPayment().appKey())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "VIRTUAL_PAYMENT_NOT_CONFIGURED",
                    "服务端尚未配置虚拟支付 OfferID 和 AppKey"
            );
        }
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "登录用户不存在"));
    }

    private DocumentAsset findDocument(String documentId) {
        return documentRepository.findById(documentId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "文档不存在"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
