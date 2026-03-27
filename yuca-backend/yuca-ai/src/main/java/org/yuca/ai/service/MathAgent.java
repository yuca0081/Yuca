package org.yuca.ai.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface MathAgent {
    @Agent
    String solve(@V("question") String question);
}
