package com.carniceria.backend.controller;

import com.carniceria.backend.dto.response.AdminOrderResponseDto;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.mapper.OrderMapper;
import com.carniceria.backend.model.Order;
import com.carniceria.backend.security.CurrentUser;
import com.carniceria.backend.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderMapper mapper;

    public AdminOrderController(OrderService orderService, OrderMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AdminOrderResponseDto> listAllOrders(Authentication authentication) {
        return orderService.listAllOrders(CurrentUser.role(authentication)).stream()
                .map(this::toAdminDto)
                .toList();
    }

    @PutMapping("/{id}/confirm-transfer")
    public AdminOrderResponseDto confirmTransfer(Authentication authentication, @PathVariable UUID id) {
        Order order = orderService.confirmTransferPayment(CurrentUser.role(authentication), id);
        return toAdminDto(order);
    }

    private AdminOrderResponseDto toAdminDto(Order order) {
        ProfileEntity client = orderService.profileOf(order.userId());
        return mapper.toAdminResponseDto(order, client.getFirstName(), client.getLastName(), client.getPhone());
    }
}
