package com.rockandhardplaces.credential;

/** Credential verification and trade qualification are separate, explicit assessments. */
public final class CredentialTrust {
    private CredentialTrust() {}
    public enum Status { PROVIDED, PENDING_VERIFICATION, VERIFIED }
    public enum EvidenceKind {
        UNION_TRADE_CREDENTIAL, TRADE_LICENSE, APPRENTICESHIP_COMPLETION,
        TRADE_SCHOOL_COMPLETION, INDUSTRY_CERTIFICATION, SAFETY_CERTIFICATION,
        MANUFACTURER_CERTIFICATION, EQUIPMENT_CERTIFICATION, CONTINUING_EDUCATION,
        INSURANCE, OTHER
    }
    public static boolean relevantKind(String type, EvidenceKind kind) {
        return switch (kind) {
            case TRADE_LICENSE -> "LICENSE".equals(type);
            case UNION_TRADE_CREDENTIAL, APPRENTICESHIP_COMPLETION, TRADE_SCHOOL_COMPLETION,
                    INDUSTRY_CERTIFICATION -> "CERTIFICATION".equals(type);
            default -> false;
        };
    }
    public static boolean supportsQualification(Status status, String scope, String type,
            EvidenceKind kind, Long assessedTradeId) {
        return status == Status.VERIFIED && "PERSONAL".equals(scope)
                && assessedTradeId != null && relevantKind(type, kind);
    }
}
