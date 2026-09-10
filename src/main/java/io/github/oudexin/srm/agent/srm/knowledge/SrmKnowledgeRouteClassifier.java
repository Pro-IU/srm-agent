package io.github.oudexin.srm.agent.srm.knowledge;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Fixed pre-retrieval classifier: knowledge text is never read while deciding route or permissions. */
@Component
public class SrmKnowledgeRouteClassifier {
    public SrmKnowledgeRoute classify(String message, Map<String, String> parameters) {
        String text = message == null ? "" : message.toLowerCase();
        boolean knowledge = contains(text, "制度", "流程", "规则", "faq", "常见问题", "错误码", "排查", "操作手册", "准入资料", "版本政策");
        // A hybrid plan needs explicit business identifiers. Domain words alone can legitimately describe a policy/FAQ.
        boolean data = parameters != null && !parameters.isEmpty();
        if (knowledge && data) return SrmKnowledgeRoute.HYBRID;
        return knowledge ? SrmKnowledgeRoute.KNOWLEDGE_ONLY : SrmKnowledgeRoute.DATA_ONLY;
    }
    private static boolean contains(String value, String... tokens) { for (String token : tokens) if (value.contains(token)) return true; return false; }
}
