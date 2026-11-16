package com.filebridge.repository;

import com.filebridge.model.PaymentTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

    boolean existsByPaymentId(String paymentId);

    Optional<PaymentTransaction> findByOrderId(String orderId);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
