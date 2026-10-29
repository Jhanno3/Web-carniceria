package com.carniceria.backend.controller;

import com.carniceria.backend.dto.request.CreateOrderRequestDto;
import com.carniceria.backend.dto.response.OrderResponseDto;
import com.carniceria.backend.mapper.OrderMapper;
import com.carniceria.backend.security.CurrentUser;
import com.carniceria.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper mapper;

    public OrderController(OrderService orderService, OrderMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @PostMapping
    public OrderResponseDto createOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequestDto dto) {
        OrderService.OrderCreationResult result = orderService.createOrder(CurrentUser.userId(jwt), dto);
        return mapper.toResponseDto(result.order(), result.mercadopagoInitPoint());
    }

    @GetMapping("/{id}")
    public OrderResponseDto getOrder(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID id) {
        return mapper.toResponseDto(orderService.getOrderById(CurrentUser.userId(jwt), CurrentUser.role(authentication), id));
    }

    @PutMapping("/{id}/mark-transfer-sent")
    public OrderResponseDto markTransferSent(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return mapper.toResponseDto(orderService.markTransferSent(CurrentUser.userId(jwt), id));
    }
}
