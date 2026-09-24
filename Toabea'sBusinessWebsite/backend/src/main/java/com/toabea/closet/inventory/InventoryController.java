package com.toabea.closet.inventory;
import com.toabea.closet.common.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/inventory") public class InventoryController { private final InventoryTransactionRepository repo; public InventoryController(InventoryTransactionRepository r){repo=r;} @GetMapping("/{productId}/transactions") public List<InventoryTransaction> history(@PathVariable String productId){return repo.findByProductIdOrderByCreatedAtDesc(productId);} }
