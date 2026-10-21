package com.example.documentstore.payment;

import com.example.documentstore.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
public class PaymentNotificationParser {

    private final ObjectMapper objectMapper;

    public PaymentNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PaymentNotification parse(String body) {
        if (body == null || body.isBlank()) {
            throw invalidCallback("支付回调内容为空");
        }
        String trimmedBody = body.trim();
        return trimmedBody.startsWith("<") ? parseXml(trimmedBody) : parseJson(trimmedBody);
    }

    private PaymentNotification parseJson(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode payInfo = firstObject(root, "WeChatPayInfo", "wechat_pay_info", "PayInfo");
            JsonNode goodsInfo = firstObject(root, "GoodsInfo", "goods_info");
            return validate(new PaymentNotification(
                    firstText(root, "Event", "event"),
                    firstText(root, "OutTradeNo", "out_trade_no", "outTradeNo"),
                    firstText(root, "OpenId", "openid", "openId", "FromUserName"),
                    firstText(payInfo, "MchOrderNo", "mch_order_no", "mchOrderNo"),
                    parseAmount(firstText(payInfo, "amount", "Amount", "TotalFee", "total_fee", "paid_fee", "PaidFee"),
                            firstText(goodsInfo, "goodsPrice", "GoodsPrice", "Price"))
            ));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidCallback("支付回调 JSON 无法解析");
        }
    }

    private PaymentNotification parseXml(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            return validate(new PaymentNotification(
                    xmlText(document, "Event"),
                    xmlText(document, "OutTradeNo"),
                    firstNonBlank(xmlText(document, "OpenId"), xmlText(document, "FromUserName")),
                    xmlText(document, "MchOrderNo"),
                    parseAmount(xmlText(document, "TotalFee"), xmlText(document, "GoodsPrice"))
            ));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidCallback("支付回调 XML 无法解析");
        }
    }

    private PaymentNotification validate(PaymentNotification notification) {
        if (isBlank(notification.event())) {
            throw invalidCallback("支付回调缺少 Event");
        }
        if ("xpay_goods_deliver_notify".equals(notification.event())) {
            if (isBlank(notification.outTradeNo())) {
                throw invalidCallback("支付回调缺少 OutTradeNo");
            }
            if (isBlank(notification.openId())) {
                throw invalidCallback("支付回调缺少 OpenId");
            }
        }
        return notification;
    }

    private Integer parseAmount(String firstAmount, String secondAmount) {
        Integer payInfoAmount = parseAmount(firstAmount);
        return payInfoAmount == null ? parseAmount(secondAmount) : payInfoAmount;
    }

    private Integer parseAmount(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private JsonNode firstObject(JsonNode node, String... fieldNames) {
        if (node == null) {
            return objectMapper.createObjectNode();
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isObject()) {
                return value;
            }
        }
        return objectMapper.createObjectNode();
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private String xmlText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException invalidCallback(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_CALLBACK", message);
    }
}
