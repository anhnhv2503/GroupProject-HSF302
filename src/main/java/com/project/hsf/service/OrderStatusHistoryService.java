package com.project.hsf.service;

import java.util.List;
import com.project.hsf.entity.OrderStatusHistory;

public interface OrderStatusHistoryService {

    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
    
    OrderStatusHistory save(OrderStatusHistory history);
} 


