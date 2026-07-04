-- Track the section a draft applicant last worked on, so long multi-section
-- forms can resume at the right page (not always page 1) when the user returns.
ALTER TABLE submission ADD COLUMN current_section_key VARCHAR(100) NULL;
