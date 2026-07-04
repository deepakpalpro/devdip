package com.banking.forms.submission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.infrastructure.SubmissionEventRepository;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SubmissionServiceTest {

    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final FormQueryService formQueryService = mock(FormQueryService.class);
    private final SectionValidator sectionValidator = mock(SectionValidator.class);
    private final SectionStorageRouter sectionStorageRouter = mock(SectionStorageRouter.class);
    private final SubmissionEventRecorder eventRecorder = mock(SubmissionEventRecorder.class);
    private final SubmissionEventRepository eventRepository = mock(SubmissionEventRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final SubmissionService service = new SubmissionService(
            submissionRepository,
            formQueryService,
            sectionValidator,
            sectionStorageRouter,
            eventRecorder,
            eventRepository,
            new ObjectMapper(),
            eventPublisher);

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VERSION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void discardDraftRemovesSectionDataEventsAndSubmission() {
        UUID id = UUID.randomUUID();
        Submission draft = new Submission(id, TENANT, VERSION, USER);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(draft));
        when(formQueryService.findPublishedByVersionId(VERSION)).thenReturn(Optional.of(publishedForm()));
        SectionStorageStrategy storage = mock(SectionStorageStrategy.class);
        when(sectionStorageRouter.resolve(StorageStrategy.JSON_BLOB)).thenReturn(storage);

        service.discardDraft(TENANT, id, USER);

        verify(storage).deleteSections(id);
        verify(eventRepository).deleteBySubmissionId(id);
        verify(submissionRepository).delete(draft);
    }

    @Test
    void discardDraftRejectsNonDraftSubmissions() {
        UUID id = UUID.randomUUID();
        Submission submitted = new Submission(id, TENANT, VERSION, USER);
        submitted.markSubmitted(Instant.now());
        when(submissionRepository.findById(id)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> service.discardDraft(TENANT, id, USER))
                .isInstanceOf(SubmissionValidationException.class);

        verify(submissionRepository, never()).delete(any());
        verify(eventRepository, never()).deleteBySubmissionId(any());
        verifyNoInteractions(sectionStorageRouter);
    }

    @Test
    void discardDraftHidesOtherApplicantsSubmissions() {
        UUID id = UUID.randomUUID();
        Submission draft = new Submission(id, TENANT, VERSION, USER);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(draft));
        UUID otherUser = UUID.randomUUID();

        assertThatThrownBy(() -> service.discardDraft(TENANT, id, otherUser))
                .isInstanceOf(SubmissionNotFoundException.class);

        verify(submissionRepository, never()).delete(any());
    }

    @Test
    void discardDraftThrowsWhenSubmissionMissing() {
        UUID id = UUID.randomUUID();
        when(submissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.discardDraft(TENANT, id, USER))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    private static PublishedFormView publishedForm() {
        return new PublishedFormView(
                UUID.randomUUID(), VERSION, "CONTACT_DEMO", "Contact Demo", "Demo", StorageStrategy.JSON_BLOB, null);
    }
}
