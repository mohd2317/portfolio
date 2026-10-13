package com.portfolio.repository;

import com.portfolio.model.Skill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByOrderByDisplayOrderAsc();
}
