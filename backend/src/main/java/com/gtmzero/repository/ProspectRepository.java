package com.gtmzero.repository;

import com.gtmzero.entity.Prospect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProspectRepository extends JpaRepository<Prospect, UUID> {

    Optional<Prospect> findByLinkedinUrl(String url);

    Optional<Prospect> findByGithubUrl(String url);
}
