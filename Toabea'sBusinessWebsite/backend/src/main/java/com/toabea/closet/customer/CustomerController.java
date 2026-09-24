package com.toabea.closet.customer;
import com.toabea.closet.common.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/customers") public class CustomerController { private final CustomerRepository repo; public CustomerController(CustomerRepository r){repo=r;} @GetMapping public List<Customer> all(){return repo.findAll();} @GetMapping("/{id}") public Customer one(@PathVariable String id){return repo.findById(id).orElseThrow();} }
