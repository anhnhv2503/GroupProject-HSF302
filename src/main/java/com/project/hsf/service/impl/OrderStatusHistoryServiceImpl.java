package com.project.hsf.service.impl;

import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.repository.OrderStatusHistoryRepository;
import com.project.hsf.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {
    
    private final OrderStatusHistoryRepository repository;

    @Override
    public List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId) {
        return repository.findByOrderIdOrderByChangedAtAsc(orderId);
    }
    
    @Override
    public OrderStatusHistory save(OrderStatusHistory history) {
        return repository.save(history);
    }
}

