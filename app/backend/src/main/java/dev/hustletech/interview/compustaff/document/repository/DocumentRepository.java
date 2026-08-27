package dev.hustletech.interview.compustaff.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.hustletech.interview.compustaff.document.domain.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

}
