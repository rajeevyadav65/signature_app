package com.signatureapp.repository;

import com.signatureapp.model.Document;
import com.signatureapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerOrderByCreatedAtDesc(User owner);

    List<Document> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, Document.DocumentStatus status);

    Optional<Document> findBySigningToken(String signingToken);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Document> searchByOwnerAndKeyword(@Param("owner") User owner, @Param("keyword") String keyword);

    long countByOwner(User owner);

    long countByOwnerAndStatus(User owner, Document.DocumentStatus status);
}
