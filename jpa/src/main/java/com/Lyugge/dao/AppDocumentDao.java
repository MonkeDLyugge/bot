package com.Lyugge.dao;

import com.Lyugge.entity.AppDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppDocumentDao extends JpaRepository<AppDocument, Long> {}
