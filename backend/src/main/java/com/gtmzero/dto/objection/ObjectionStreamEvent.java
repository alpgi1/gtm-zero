package com.gtmzero.dto.objection;

import java.util.List;
import java.util.UUID;

/**
 * Sealed hierarchy of events emitted on the SSE stream.
 * Order during a successful run: Started → Retrieved → Token* → Completed.
 * Failed terminates early.
 */
public sealed interface ObjectionStreamEvent {

    /** SSE event name — used as the `event:` field on the wire. */
    String eventName();

    record Started(UUID queryId, long timestamp) implements ObjectionStreamEvent {
        @Override public String eventName() { return "started"; }
    }

    record Retrieved(List<CitationDto> citations, long latencyMs) implements ObjectionStreamEvent {
        @Override public String eventName() { return "retrieved"; }
    }

    record Token(String text) implements ObjectionStreamEvent {
        @Override public String eventName() { return "token"; }
    }

    record Completed(ObjectionResponse response) implements ObjectionStreamEvent {
        @Override public String eventName() { return "completed"; }
    }

    record Failed(String message, String code) implements ObjectionStreamEvent {
        @Override public String eventName() { return "failed"; }
    }
}
