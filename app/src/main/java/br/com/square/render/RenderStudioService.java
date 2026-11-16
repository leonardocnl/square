package br.com.square.render;

import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer;
import com.eteks.sweethome3d.j3d.Object3DBranchFactory;
import com.eteks.sweethome3d.j3d.PhotoRenderer;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.viewcontroller.Object3DFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Servico desacoplado para renderizacao fotorrealista e gerenciamento de iluminacao
 * em segundo plano, sem bloquear a thread de interface de usuario (EDT).
 */
public class RenderStudioService {

    public enum ResolutionPreset {
        HD_720P("1280 x 720 (HD 16:9)", 1280, 720),
        FULL_HD_1080P("1920 x 1080 (Full HD 16:9)", 1920, 1080),
        UHD_4K("3840 x 2160 (4K UHD 16:9)", 3840, 2160),
        SQUARE_1080("1080 x 1080 (Quadrado 1:1)", 1080, 1080),
        PORTRAIT_1350("1080 x 1350 (Retrato 4:5)", 1080, 1350);

        private final String label;
        private final int width;
        private final int height;

        ResolutionPreset(String label, int width, int height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }

        public String getLabel() {
            return label;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum RenderQuality {
        DRAFT(0, "Rascunho Rapido"),
        NORMAL(1, "Padrao"),
        HIGH(2, "Alta Fidelidade (Sunflow)"),
        ULTRA(3, "Producao (Path Tracing)");

        private final int qualityLevel;
        private final String description;

        RenderQuality(int qualityLevel, String description) {
            this.qualityLevel = qualityLevel;
            this.description = description;
        }

        public int getQualityLevel() {
            return qualityLevel;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum RenderStatus {
        IDLE,
        PREPARING,
        RENDERING,
        COMPLETED,
        CANCELLED,
        ERROR
    }

    private final ExecutorService executorService;
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
    private volatile AbstractPhotoRenderer activeRenderer;
    private volatile RenderStatus currentStatus = RenderStatus.IDLE;

    public RenderStudioService() {
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Square-RenderStudio-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public RenderStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Ajusta o horario solar do observador (0.0 a 24.0 horas) na camera do projeto.
     */
    public static void setSunHour(Home home, double hourOfDay) {
        if (home == null || home.getCamera() == null) {
            return;
        }
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, (int) hourOfDay);
        int minutes = (int) Math.round((hourOfDay - (int) hourOfDay) * 60.0);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        home.getCamera().setTime(calendar.getTimeInMillis());
    }

    /**
     * Obtem o horario solar aproximado da camera do projeto em horas (0.0 a 24.0).
     */
    public static double getSunHour(Home home) {
        if (home == null || home.getCamera() == null) {
            return 12.0;
        }
        long time = home.getCamera().getTime();
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(time);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        return hours + (minutes / 60.0);
    }

    /**
     * Liga ou desliga as luzes do teto configuradas no ambiente.
     */
    public static void setCeilingLightsEnabled(Home home, boolean enabled) {
        if (home == null || home.getEnvironment() == null) {
            return;
        }
        int color = enabled ? 0xEBEBEB : 0x000000;
        home.getEnvironment().setCeillingLightColor(color);
    }

    /**
     * Verifica se as luzes do teto estao acesas.
     */
    public static boolean isCeilingLightsEnabled(Home home) {
        if (home == null || home.getEnvironment() == null) {
            return false;
        }
        return (home.getEnvironment().getCeillingLightColor() & 0xFFFFFF) != 0;
    }

    /**
     * Renderiza uma imagem fotorrealista de forma assincrona em segundo plano.
     */
    public CompletableFuture<BufferedImage> renderPhotoAsync(
            Home home,
            Camera camera,
            ResolutionPreset resolution,
            RenderQuality quality,
            Consumer<RenderStatus> statusConsumer,
            Consumer<Integer> progressConsumer) {

        CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        cancellationRequested.set(false);
        currentStatus = RenderStatus.PREPARING;
        if (statusConsumer != null) {
            statusConsumer.accept(currentStatus);
        }

        executorService.submit(() -> {
            try {
                int width = resolution.getWidth();
                int height = resolution.getHeight();
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                currentStatus = RenderStatus.RENDERING;
                if (statusConsumer != null) {
                    statusConsumer.accept(currentStatus);
                }

                AbstractPhotoRenderer.Quality rendererQuality;
                switch (quality) {
                    case DRAFT:
                        rendererQuality = AbstractPhotoRenderer.Quality.LOW;
                        break;
                    case NORMAL:
                        rendererQuality = AbstractPhotoRenderer.Quality.HIGH;
                        break;
                    case HIGH:
                    case ULTRA:
                    default:
                        rendererQuality = AbstractPhotoRenderer.Quality.HIGH;
                        break;
                }

                Object3DFactory factory = new Object3DBranchFactory();
                AbstractPhotoRenderer renderer = AbstractPhotoRenderer.createInstance(
                        PhotoRenderer.class.getName(), home, factory, rendererQuality);
                this.activeRenderer = renderer;

                if (cancellationRequested.get()) {
                    currentStatus = RenderStatus.CANCELLED;
                    if (statusConsumer != null) {
                        statusConsumer.accept(currentStatus);
                    }
                    future.cancel(true);
                    return;
                }

                Camera renderCam = camera != null ? camera : home.getCamera();

                renderer.render(image, renderCam, (img, infoflags, x, y, w, h) -> {
                    if (cancellationRequested.get()) {
                        renderer.stop();
                        return false;
                    }
                    if (progressConsumer != null && height > 0) {
                        int progress = Math.min(100, Math.max(0, (y * 100) / height));
                        progressConsumer.accept(progress);
                    }
                    return true;
                });

                if (cancellationRequested.get()) {
                    currentStatus = RenderStatus.CANCELLED;
                    if (statusConsumer != null) {
                        statusConsumer.accept(currentStatus);
                    }
                    future.cancel(true);
                } else {
                    currentStatus = RenderStatus.COMPLETED;
                    if (statusConsumer != null) {
                        statusConsumer.accept(currentStatus);
                    }
                    if (progressConsumer != null) {
                        progressConsumer.accept(100);
                    }
                    future.complete(image);
                }
            } catch (Exception ex) {
                currentStatus = RenderStatus.ERROR;
                if (statusConsumer != null) {
                    statusConsumer.accept(currentStatus);
                }
                future.completeExceptionally(ex);
            } finally {
                this.activeRenderer = null;
            }
        });

        return future;
    }

    /**
     * Interrompe qualquer renderizacao em execucao.
     */
    public void cancelRender() {
        cancellationRequested.set(true);
        AbstractPhotoRenderer renderer = this.activeRenderer;
        if (renderer != null) {
            renderer.stop();
        }
        currentStatus = RenderStatus.CANCELLED;
    }

    /**
     * Grava uma imagem no disco nos formatos PNG ou JPEG.
     */
    public static void saveImageToFile(BufferedImage image, File targetFile, String formatName) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("Imagem nao pode ser nula");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Arquivo de destino nao pode ser nulo");
        }
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String format = (formatName == null || formatName.trim().isEmpty()) ? "png" : formatName.toLowerCase();
        boolean written = ImageIO.write(image, format, targetFile);
        if (!written) {
            throw new IOException("Nenhum gravador encontrado para o formato: " + format);
        }
    }

    /**
     * Copia uma imagem para a area de transferencia (Clipboard) do sistema operacional.
     */
    public static void copyToClipboard(BufferedImage image) {
        if (image == null) {
            return;
        }
        Transferable transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    return image;
                }
                throw new UnsupportedFlavorException(flavor);
            }
        };
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }

    /**
     * Finaliza os recursos da thread de execucao.
     */
    public void shutdown() {
        cancelRender();
        executorService.shutdownNow();
    }
}
