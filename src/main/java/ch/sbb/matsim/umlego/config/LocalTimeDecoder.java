package ch.sbb.matsim.umlego.config;

import org.github.gestalt.config.decoder.Decoder;
import org.github.gestalt.config.decoder.DecoderContext;
import org.github.gestalt.config.decoder.Priority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.entity.ValidationLevel;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.GResultOf;

import java.time.LocalTime;

public class LocalTimeDecoder implements Decoder<LocalTime> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "LocalTime";
    }

    @Override
    public boolean canDecode(String path, Tags tags, ConfigNode node, TypeCapture<?> type) {
        return type.getRawType() == LocalTime.class;
    }

    @Override
    public GResultOf<LocalTime> decode(String path, Tags tags, ConfigNode node, TypeCapture<?> type, DecoderContext decoderContext) {

        String value = node.getValue().orElseThrow();

        try {
            // Split the time string by colon
            String[] parts = value.split(":");

            // Parse hours and minutes
            int hours = Integer.parseInt(parts[0]);
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            // Create LocalTime object
            LocalTime result = LocalTime.of(hours, minutes);

            return GResultOf.result(result);
        } catch (Exception e) {
            return GResultOf.errors(new ValidationError(ValidationLevel.ERROR) {
                @Override
                public String description() {
                    return "Failed to parse time '" + value + "' at path: " + path +
                            ". Expected format: 'hour:minute' (e.g. '8:30')";
                }
            });
        }
    }
}
