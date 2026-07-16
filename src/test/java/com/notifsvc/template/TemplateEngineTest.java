package com.notifsvc.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateEngineTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    @Test
    void substitutesAllPlaceholders() {
        String result = templateEngine.render(
                "Hi {{firstName}}, welcome to {{company}}!",
                Map.of("firstName", "Bob", "company", "Acme"));

        assertThat(result).isEqualTo("Hi Bob, welcome to Acme!");
    }

    @Test
    void tolerantOfWhitespaceInsidePlaceholder() {
        String result = templateEngine.render("Hi {{ firstName }}!", Map.of("firstName", "Bob"));

        assertThat(result).isEqualTo("Hi Bob!");
    }

    @Test
    void missingVariableThrowsInsteadOfLeakingLiteralPlaceholder() {
        assertThatThrownBy(() -> templateEngine.render("Hi {{firstName}}!", Map.of()))
                .isInstanceOf(TemplateRenderException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void noPlaceholdersReturnsBodyUnchanged() {
        String result = templateEngine.render("Just a plain message.", Map.of());

        assertThat(result).isEqualTo("Just a plain message.");
    }

    @Test
    void sameVariableUsedMultipleTimesIsSubstitutedEverywhere() {
        String result = templateEngine.render("{{name}} - {{name}}", Map.of("name", "X"));

        assertThat(result).isEqualTo("X - X");
    }
}
