package com.portfolio.service;

import com.portfolio.dto.Dtos.ContactMessageResponse;
import com.portfolio.dto.Dtos.ContactRequest;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.model.ContactMessage;
import com.portfolio.repository.ContactMessageRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores contact-form submissions and lets the admin review them. */
@Service
@Transactional
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactMessageRepository repository;

    public ContactService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    public void submit(ContactRequest request) {
        // Honeypot: real users never see or fill this field. Pretend success to bots.
        if (request.website() != null && !request.website().isBlank()) {
            log.warn("Contact submission dropped by honeypot");
            return;
        }
        repository.save(new ContactMessage(request.name().strip(), request.email().strip(),
                request.subject().strip(), request.message().strip()));
        // Deliberately no personal data in logs.
        log.info("Contact message stored");
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(ContactMessageResponse::from).toList();
    }

    public ContactMessageResponse markRead(Long id) {
        ContactMessage m = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Message", id));
        m.markRead();
        return ContactMessageResponse.from(m);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Message", id);
        }
        repository.deleteById(id);
    }
}
