package com.carniceria.backend.service;

import com.carniceria.backend.model.Order;
import com.carniceria.backend.model.OrderItem;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Único punto de contacto con la API de Mercado Pago. Nada en el resto del backend sabe nada
 * del SDK: solo conoce este service (ver constitution.md sección 7).
 */
@Service
public class MercadoPagoService {

    private final String accessToken;
    private final String frontendBaseUrl;
    private final String backendBaseUrl;

    public MercadoPagoService(@Value("${app.mercadopago-access-token}") String accessToken,
                               @Value("${app.cors-allowed-origins}") String frontendBaseUrl,
                               @Value("${app.base-url}") String backendBaseUrl) {
        this.accessToken = accessToken;
        this.frontendBaseUrl = frontendBaseUrl;
        this.backendBaseUrl = backendBaseUrl;
    }

    @PostConstruct
    void configureSdk() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    public record PreferenceResult(String preferenceId, String initPoint) {
    }

    public PreferenceResult createPreference(Order order) {
        try {
            List<PreferenceItemRequest> items = order.items().stream().map(this::toItemRequest).toList();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendBaseUrl + "/checkout.html")
                    .pending(frontendBaseUrl + "/checkout.html")
                    .failure(frontendBaseUrl + "/checkout.html")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .externalReference(order.id().toString())
                    .notificationUrl(backendBaseUrl + "/api/webhooks/mercadopago")
                    .build();

            Preference preference = new PreferenceClient().create(request);
            return new PreferenceResult(preference.getId(), preference.getInitPoint());
        } catch (Exception e) {
            throw new IllegalStateException("No pudimos crear la preferencia de pago de Mercado Pago.", e);
        }
    }

    private PreferenceItemRequest toItemRequest(OrderItem item) {
        // MP no admite cantidades fraccionarias: para cortes por kg, 1 ítem = el subtotal ya calculado.
        BigDecimal unitPrice = item.subtotalEstimated() != null ? item.subtotalEstimated() : BigDecimal.ZERO;
        return PreferenceItemRequest.builder()
                .title(item.productNameSnapshot())
                .quantity(1)
                .unitPrice(unitPrice)
                .build();
    }

    public record MercadoPagoPaymentInfo(String id, String status, String externalReference) {
    }

    /** Nunca confiar en el cuerpo de un webhook: esto vuelve a pedirle el pago entero a la API real. */
    public MercadoPagoPaymentInfo getPayment(String paymentId) {
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(paymentId));
            return new MercadoPagoPaymentInfo(String.valueOf(payment.getId()), payment.getStatus(), payment.getExternalReference());
        } catch (Exception e) {
            throw new IllegalStateException("No pudimos consultar el pago en Mercado Pago.", e);
        }
    }
}
