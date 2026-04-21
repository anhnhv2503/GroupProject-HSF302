package com.project.hsf.service;

import java.util.List;
import com.project.hsf.entity.OrderItem;

public interface OrderItemService {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
} 