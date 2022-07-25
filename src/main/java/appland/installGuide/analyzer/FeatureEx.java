package appland.installGuide.analyzer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class FeatureEx extends Feature {
    public @Nullable String depFile;
    public @Nullable String plugin;
    public @Nullable String pluginType;
}
