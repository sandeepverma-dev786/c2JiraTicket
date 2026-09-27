package com.supportticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.supportticket.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

	List<Comment> findByTicket_Id(UUID ticketId);
}