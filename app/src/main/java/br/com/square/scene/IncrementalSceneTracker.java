package br.com.square.scene;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de atualizacoes incrementais para o grafo de cena 3D.
 * Desacoplado da camada grafica nativa do Java3D legado.
 * Evita reconstruir ou desmontar subarvores inteiras a cada pequeno evento de translacao
 * ou rotacao na interface 2D/3D, aplicando mutacoes diretamente nos alvos de transformacao.
 */
public class IncrementalSceneTracker {

    /**
     * Interface agnostica para o destino de transformacao geometrica no grafo de cena.
     */
    public interface SceneTransformTarget {
        void setTransform(double tx, double ty, double tz, double angleY);
    }

    public enum DirtyType {
        CLEAN,
        TRANSFORM,
        GEOMETRY,
        APPEARANCE
    }

    public static class TrackedNode {
        private final Object item;
        private final SceneTransformTarget target;
        private DirtyType dirtyType = DirtyType.CLEAN;
        private double tx;
        private double ty;
        private double tz;
        private double angleY;
        private long lastModifiedTime = System.currentTimeMillis();

        public TrackedNode(Object item, SceneTransformTarget target) {
            this.item = item;
            this.target = target;
        }

        public Object getItem() {
            return item;
        }

        public SceneTransformTarget getTarget() {
            return target;
        }

        public DirtyType getDirtyType() {
            return dirtyType;
        }

        public void setDirtyType(DirtyType dirtyType) {
            this.dirtyType = dirtyType;
            this.lastModifiedTime = System.currentTimeMillis();
        }

        public double getTx() {
            return tx;
        }

        public double getTy() {
            return ty;
        }

        public double getTz() {
            return tz;
        }

        public double getAngleY() {
            return angleY;
        }

        public long getLastModifiedTime() {
            return lastModifiedTime;
        }

        public void update(double tx, double ty, double tz, double angleY) {
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.angleY = angleY;
            this.lastModifiedTime = System.currentTimeMillis();
            if (this.target != null) {
                this.target.setTransform(tx, ty, tz, angleY);
            }
            this.dirtyType = DirtyType.CLEAN;
        }
    }

    private final Map<Object, TrackedNode> trackedNodes = new ConcurrentHashMap<Object, TrackedNode>();

    /**
     * Registra um item de dominio e seu respectivo alvo de transformacao.
     */
    public void trackNode(Object item, SceneTransformTarget target) {
        if (item != null) {
            trackedNodes.put(item, new TrackedNode(item, target));
        }
    }

    /**
     * Remove o rastreamento do item quando este for excluido do projeto.
     */
    public void untrackNode(Object item) {
        if (item != null) {
            trackedNodes.remove(item);
        }
    }

    /**
     * Atualiza diretamente a posicao e rotacao do no em O(1),
     * sem invalidar nem reconstruir as geometrias associadas.
     */
    public boolean updateTransform(Object item, float x, float y, float elevation, float angleRadians) {
        TrackedNode node = trackedNodes.get(item);
        if (node == null) {
            return false;
        }
        node.update(x, elevation, y, angleRadians);
        return true;
    }

    public TrackedNode getTrackedNode(Object item) {
        return trackedNodes.get(item);
    }

    /**
     * Marca o item com um estado dirty especifico.
     */
    public void markDirty(Object item, DirtyType dirtyType) {
        TrackedNode trackedNode = trackedNodes.get(item);
        if (trackedNode != null) {
            trackedNode.setDirtyType(dirtyType);
        }
    }

    public boolean isDirty(Object item) {
        TrackedNode trackedNode = trackedNodes.get(item);
        return trackedNode != null && trackedNode.getDirtyType() != DirtyType.CLEAN;
    }

    public int getTrackedCount() {
        return trackedNodes.size();
    }

    public void clear() {
        trackedNodes.clear();
    }
}
