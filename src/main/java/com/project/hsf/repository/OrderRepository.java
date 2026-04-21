package com.project.hsf.repository;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedDateDesc(User customer);
}
