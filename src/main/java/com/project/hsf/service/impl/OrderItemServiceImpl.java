package com.project.hsf.service.impl;

import com.project.hsf.entity.OrderItem;
import com.project.hsf.repository.OrderItemRepository;
import com.project.hsf.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId) {
        return orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
    }
}
