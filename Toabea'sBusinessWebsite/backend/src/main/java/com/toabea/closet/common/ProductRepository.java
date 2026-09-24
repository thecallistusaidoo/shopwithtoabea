package com.toabea.closet.common;
import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface ProductRepository extends MongoRepository<Product,String> { List<Product> findByActiveTrueOrderByCreatedAtDesc(); }
