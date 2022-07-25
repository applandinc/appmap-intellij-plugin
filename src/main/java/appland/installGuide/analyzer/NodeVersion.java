package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class NodeVersion {
    @SerializedName("major") int major;
    @SerializedName("minor") int minor;
    @SerializedName("path") int patch;

    private static final Logger LOG = Logger.getInstance(NodeVersion.class);

    public static @Nullable NodeVersion parse(@NotNull String version) {
        try {
            var digits = Arrays.stream(version.replaceAll("[^\\d.]", "").split("\\."))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            while (digits.size() < 3) {
                digits.add(0);
            }

            return new NodeVersion(digits.get(0), digits.get(1), digits.get(2));
        } catch (Exception e) {
            LOG.warn("error parsing NodeJS version", e);
            return null;
        }
    }
}
