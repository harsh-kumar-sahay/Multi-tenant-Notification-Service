package com.notifsvc.template;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable substitution for {{varName}} placeholders. Missing variables fail loudly
 * at render time rather than silently emitting the literal placeholder, so a bad
 * send request surfaces immediately instead of shipping "Hi {{firstName}}" to a user.
 */
@Component
public class TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    public String render(String templateBody, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(templateBody);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.get(key);
            if (value == null) {
                throw new TemplateRenderException("Missing value for template variable '" + key + "'");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
