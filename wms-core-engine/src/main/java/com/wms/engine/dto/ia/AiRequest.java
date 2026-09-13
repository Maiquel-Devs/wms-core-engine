package com.wms.engine.dto.ia;

import java.util.List;

public record AiRequest(String model, List<AiMessage> messages, double temperature) {
}