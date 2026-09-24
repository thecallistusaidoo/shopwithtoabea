package com.toabea.closet.common;
import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction,String> { List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(String productId); }
