package win.demistorm.vr_interactions.client.feature.horse;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.mixin.ModelPartAccessor;

// Render flush helpers for the drawing equine (live model capture and temporary children map rein hiding)
public final class RenderedEquineContext {

    private static final Logger log = LoggerFactory.getLogger(RenderedEquineContext.class);

    // Vanilla names (left/right_saddle_line) plus common resourcepack ones
    private static final String[] REIN_NAME_HINTS = {"rein", "saddle_line", "bridle"};

    private record ReinLink(ModelPart parent, String name, ModelPart part) {}

    private static final WeakHashMap<ModelPart, List<ReinLink>> reinPartsCache = new WeakHashMap<>();
    // Only the first capture per horse per frame counts
    private static final Set<Integer> capturedThisFrame = new HashSet<>();
    private static final Map<String, Long> lastDrawDebugLogs = new HashMap<>();
    private static boolean suppressFailed;

    private RenderedEquineContext() {}

    public static void newFrame() {
        capturedThisFrame.clear();
    }

    // Called at ModelFeatureRenderer.renderModel HEAD while the rope is active
    public static void beginModelDraw(Model<?> model, Object state) {
        if (!(state instanceof ReinsRenderState ext) || !ext.vr_interactions$hideSaddleReins()) {
            return;
        }
        try {
            for (ReinLink link : reinPartsFor(model.root())) {
                childrenOf(link.parent()).remove(link.name());
            }
        } catch (Throwable t) {
            logOnce("Rein hiding failed on {}: {}", model.getClass().getSimpleName(), t.toString());
        }
    }

    // renderModel TAIL restores rein parts and captures the live pose
    public static void endModelDraw(Model<?> model, Object state) {
        if (!(state instanceof ReinsRenderState ext) || !ext.vr_interactions$hideSaddleReins()) {
            return;
        }
        ModelPart root = model.root();
        List<ReinLink> links = reinPartsFor(root);
        if (VRInteractions.debugMode) {
            logDraw(model, root, links);
        }
        try {
            for (ReinLink link : links) {
                childrenOf(link.parent()).putIfAbsent(link.name(), link.part());
            }
        } catch (Throwable t) {
            logOnce("Rein restore failed on {}: {}", model.getClass().getSimpleName(), t.toString());
        }
        if (capturedThisFrame.add(ext.vr_interactions$trackedHorseId())) {
            HorseRingsVisuals.captureLive(ext.vr_interactions$trackedHorseId(), root);
        }
    }

    // (DEBUG) One line per model class per second
    private static void logDraw(Model<?> model, ModelPart root, List<ReinLink> links) {
        long now = System.currentTimeMillis();
        String key = model.getClass().getSimpleName();
        if (now - lastDrawDebugLogs.getOrDefault(key, 0L) < 1000) {
            return;
        }
        lastDrawDebugLogs.put(key, now);
        StringBuilder names = new StringBuilder();
        for (ReinLink link : links) {
            if (names.length() > 0) {
                names.append(",");
            }
            names.append(link.name());
        }
        log.debug("Equine draw model={} root={} hiddenReins={} [{}]",
                key, root.getClass().getSimpleName(), links.size(), names);
    }

    private static void logOnce(String format, Object a, Object b) {
        if (!suppressFailed) {
            suppressFailed = true;
            log.warn(format, a, b);
        }
    }

    private static Map<String, ModelPart> childrenOf(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).vr_interactions$children();
    }

    // Rein-like parts with their parents per model tree, cached for model instance lifetime (reload accounted)
    private static List<ReinLink> reinPartsFor(ModelPart root) {
        return reinPartsCache.computeIfAbsent(root, r -> {
            List<ReinLink> links = new ArrayList<>();
            r.visit(new PoseStack(), (pose, path, index, cube) -> {
                ModelPart current = r;
                for (String segment : path.split("/")) {
                    if (segment.isEmpty() || !current.hasChild(segment)) {
                        continue;
                    }
                    ReinLink link = new ReinLink(current, segment, current.getChild(segment));
                    if (isReinName(segment) && !links.contains(link)) {
                        links.add(link);
                    }
                    current = current.getChild(segment);
                }
            });
            return links;
        });
    }

    private static boolean isReinName(String name) {
        String lower = name.toLowerCase();
        for (String hint : REIN_NAME_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
