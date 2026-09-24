package com.toabea.closet.common;
import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface CustomerRepository extends MongoRepository<Customer,String> { Optional<Customer> findByEmailIgnoreCase(String email); }
