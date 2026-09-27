package com.supportticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

	List<Ticket> findByStatus(TicketStatus status);

	List<Ticket> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
			String titleKeyword, String descriptionKeyword);

	List<Ticket> findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
			TicketStatus titleStatus, String titleKeyword, TicketStatus descriptionStatus, String descriptionKeyword);

	default List<Ticket> findByFilters(String keyword, TicketStatus status) {
		if (keyword == null || keyword.isBlank()) {
			return status == null ? findAll() : findByStatus(status);
		}

		if (status == null) {
			return findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
		}

		return findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
				status, keyword, status, keyword);
	}
}