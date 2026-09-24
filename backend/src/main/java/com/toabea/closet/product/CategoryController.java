package com.toabea.closet.product;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document; import org.springframework.data.mongodb.repository.MongoRepository; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.*;
@Document("categories") @Getter @Setter @NoArgsConstructor class Category { @Id private String id; @Indexed(unique=true) private String name; private String imageUrl; private boolean active=true; private Instant createdAt=Instant.now(); }
interface CategoryRepository extends MongoRepository<Category,String> { List<Category> findByActiveTrueOrderByNameAsc(); }
@RestController @RequestMapping("/api/categories") public class CategoryController { private final CategoryRepository repo; public CategoryController(CategoryRepository r){repo=r;}
public record CategoryRequest(@NotBlank @Size(max=60) String name,@Size(max=500) String imageUrl,boolean active){}
@GetMapping("/public") public List<Category> publicList(){return repo.findByActiveTrueOrderByNameAsc();}
@GetMapping public List<Category> all(){return repo.findAll();}
@PostMapping public Category create(@Valid @RequestBody CategoryRequest r){Category c=new Category();c.setName(r.name().trim());c.setImageUrl(r.imageUrl());c.setActive(r.active());return repo.save(c);}
@PutMapping("/{id}") public Category update(@PathVariable String id,@Valid @RequestBody CategoryRequest r){Category old=repo.findById(id).orElseThrow();old.setName(r.name().trim());old.setImageUrl(r.imageUrl());old.setActive(r.active());return repo.save(old);}
@DeleteMapping("/{id}") public void delete(@PathVariable String id){repo.deleteById(id);} }
