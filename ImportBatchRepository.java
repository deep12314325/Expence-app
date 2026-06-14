package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.ImportBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findAllByOrderByImportedAtDesc();
}
