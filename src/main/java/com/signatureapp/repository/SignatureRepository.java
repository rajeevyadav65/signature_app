package com.signatureapp.repository;

import com.signatureapp.model.Document;
import com.signatureapp.model.Signature;
import com.signatureapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {

    List<Signature> findByDocumentOrderByCreatedAtAsc(Document document);

    List<Signature> findByDocumentAndStatus(Document document, Signature.SignatureStatus status);

    List<Signature> findBySignerOrderByCreatedAtDesc(User signer);

    boolean existsByDocumentAndSignerEmail(Document document, String signerEmail);

    long countByDocumentAndStatus(Document document, Signature.SignatureStatus status);
}
