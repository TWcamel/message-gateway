package com.mnp.commons.model;

/**
 * Inbox / Outbox semantic direction.
 * <ul>
 *   <li>INBOX: traffic inbound to the owning system</li>
 *   <li>OUTBOX: traffic outbound from the owning system</li>
 * </ul>
 */
public enum Direction {
    INBOX("inbox"),
    OUTBOX("outbox");

    private final String label;

    Direction(final String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
