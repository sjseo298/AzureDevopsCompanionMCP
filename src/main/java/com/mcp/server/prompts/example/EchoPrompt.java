package com.mcp.server.prompts.example;

import com.mcp.server.prompts.base.BasePrompt;
import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt de ejemplo para la plantilla MCP.
 * Devuelve un mensaje con el texto recibido como argumento.
 */
public class EchoPrompt extends BasePrompt {
    
    public EchoPrompt() {
        super(
            "echo",
            "Echo Prompt",
            "Devuelve un mensaje con el texto recibido como argumento.",
            List.of(new Prompt.PromptArgument("texto", "Texto", "Texto a repetir", true))
        );
    }

    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        String texto = getStringArgument(arguments, "texto", "Hello World!");
        return new PromptResult(
            "Echo de texto solicitado",
            List.of(
                userMessage("Por favor repite este texto: " + texto),
                assistantMessage("El texto que me has pedido repetir es: " + texto)
            )
        );
    }
}
