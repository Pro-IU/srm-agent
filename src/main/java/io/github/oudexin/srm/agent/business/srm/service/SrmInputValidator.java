package io.github.oudexin.srm.agent.business.srm.service;

final class SrmInputValidator {
    private SrmInputValidator() {
    }

    static String requiredId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new SrmValidationException(fieldName + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new SrmValidationException(fieldName + "长度不能超过64");
        }
        return trimmed;
    }

    static String requiredMaterialCode(String value) {
        return requiredId(value, "materialCode");
    }
}
