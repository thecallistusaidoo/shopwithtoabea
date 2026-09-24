package com.toabea.closet.common;
import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface AdminUserRepository extends MongoRepository<AdminUser,String> { Optional<AdminUser> findByUsername(String username); }
