package com.ams.resident.repository;

import com.ams.resident.entity.ResidentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentProfileRepository extends JpaRepository<ResidentProfile, String> {
    boolean existsByUserId(String userId);
}
