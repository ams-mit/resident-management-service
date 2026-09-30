package kln.ams.residentmanagement.repository;

import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String>, JpaSpecificationExecutor<Profile> {

    Optional<Profile> findByIdAndProfileType(String id, ProfileType profileType);

    Optional<Profile> findByUserIdAndProfileType(String userId, ProfileType profileType);

    boolean existsByUserIdAndProfileType(String userId, ProfileType profileType);

    List<Profile> findByUserId(String userId);
}
