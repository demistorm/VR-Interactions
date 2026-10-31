package win.demistorm.vr_interactions.client.feature.horse;

// Renderstate extras stashed at extractRenderState and read back at model render time
public interface ReinsRenderState {

    void vr_interactions$hideSaddleReins(boolean hide);

    boolean vr_interactions$hideSaddleReins();

    void vr_interactions$trackedHorseId(int id);

    int vr_interactions$trackedHorseId();
}
