package appland.lang.agents;

import org.jetbrains.annotations.NotNull;

public enum AgentLanguage {
    Ruby;

    @NotNull
    public String getName() {
        switch (this) {
            case Ruby:
                return "ruby";
            default:
                throw new IllegalStateException("unsupported language " + this);
        }
    }
}
