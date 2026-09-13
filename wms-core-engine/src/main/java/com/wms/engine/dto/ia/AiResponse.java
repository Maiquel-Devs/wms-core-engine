package com.wms.engine.dto.ia;

import java.util.List;

public record AiResponse(List<Choice> choices) {
    public record Choice(AiMessage message) {}
}