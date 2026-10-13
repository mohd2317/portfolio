package com.portfolio.repository;

import com.portfolio.model.Testimonial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    List<Testimonial> findAllByOrderByDisplayOrderAsc();
}
