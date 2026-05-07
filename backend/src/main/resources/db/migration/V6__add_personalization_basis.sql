-- V6: capture the LLM-reported personalization basis on each generated outreach.
--
-- The OutreachService already parses `personalization_basis` from the JSON
-- envelope; we just never persisted it. The Outreach detail UI surfaces this
-- as the "this is grounded in real customer data" signal, so it needs to be
-- queryable per row.

ALTER TABLE outreach_messages
    ADD COLUMN personalization_basis TEXT;
