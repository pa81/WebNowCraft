package com.pa81.mod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class OverlayRenderer implements HudRenderCallback {
    private String lastCoverUrl = "";
    private Identifier currentCoverId = null;
    private final Set<String> failedUrls = new HashSet<>();

    private static final int WIDTH = 190;
    private static final int HEIGHT = 56;
    private static final int BG_COLOR = new Color(20, 20, 20, 230).getRGB();
    private static final int ACCENT_COLOR = new Color(30, 215, 96).getRGB();

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getDebugHud().shouldShowDebugHud()) return;

        MusicState state = SpicetifyServer.getState();
        WebnowcraftConfig config = WebnowcraftConfig.get();

        // カバー画像の更新確認
        updateCoverTexture(client, state.coverUrl);

        int screenWidth = client.getWindow().getScaledWidth();
        // int screenHeight = client.getWindow().getScaledHeight();

        // 基準点の計算
        float scale = config.scale;
        int baseX;
        int baseY = config.yOffset;

        if (config.anchorLeft) {
            // 左上基準
            baseX = config.xOffset;
        } else {
            // 右上基準 (デフォルト)
            // 右端から WIDTH * scale 分だけ戻った位置が開始点
            // さらに xOffset で微調整（負の値なら左へ、正の値なら右へ）
            baseX = (int) (screenWidth - (WIDTH * scale)) + config.xOffset;
        }

        // 行列スタックを保存（他のHUDに影響を与えないため）
        context.getMatrices().push();

        // 1. 位置を移動
        context.getMatrices().translate(baseX, baseY, 0);
        // 2. サイズを拡大縮小
        context.getMatrices().scale(scale, scale, 1.0f);

        // ここからは (0, 0) を左上として描画すれば、上記の設定位置・サイズで表示される
        renderContent(context, client, state);

        // 行列スタックを復元
        context.getMatrices().pop();
    }

    // 描画ロジックを分離（座標は 0, 0 基準）
    private void renderContent(DrawContext context, MinecraftClient client, MusicState state) {
        // 背景
        context.fill(0, 0, WIDTH, HEIGHT, BG_COLOR);

        // アルバムアート
        if (currentCoverId != null) {
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    currentCoverId,
                    4, 4,
                    0.0f, 0.0f,
                    48, 48,
                    48, 48
            );
        } else {
            context.fill(4, 4, 52, 52, 0xFF333333);
        }

        // テキスト
        int textX = 60;
        int textY = 6;
        int maxWidth = WIDTH - 70;

        context.drawText(client.textRenderer, truncate(client, state.title, maxWidth), textX, textY, 0xFFFFFF, true);
        context.drawText(client.textRenderer, truncate(client, state.artist, maxWidth), textX, textY + 12, 0xBBBBBB, true);

        // プログレスバー
        int barWidth = 120;
        int barHeight = 4;
        int barX = textX;
        int barY = 40;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF404040);

        if (state.durationSec > 0) {
            float percent = (float) state.positionSec / state.durationSec;
            if (percent > 1.0f) percent = 1.0f;
            int progressPx = (int) (barWidth * percent);
            context.fill(barX, barY, barX + progressPx, barY + barHeight, ACCENT_COLOR);
        }

        // 時間表示
        String timeDisplay = state.positionStr + " / " + state.durationStr;
        int timeWidth = client.textRenderer.getWidth(timeDisplay);
        context.drawText(client.textRenderer, timeDisplay, WIDTH - timeWidth - 5, 25, 0x888888, true);
    }

    private String truncate(MinecraftClient client, String text, int maxWidth) {
        if (text == null) return "";
        if (client.textRenderer.getWidth(text) <= maxWidth) return text;
        return client.textRenderer.trimToWidth(text, maxWidth - 6) + "...";
    }

    private void updateCoverTexture(MinecraftClient client, String url) {
        if (url == null || url.isEmpty() || url.equals("Default")) return;
        if (url.equals(lastCoverUrl) || failedUrls.contains(url)) return;

        lastCoverUrl = url;

        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                if (conn.getResponseCode() != 200) {
                    failedUrls.add(url);
                    return;
                }

                byte[] rawBytes;
                try (InputStream is = conn.getInputStream();
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    rawBytes = buffer.toByteArray();
                }

                BufferedImage bImage = ImageIO.read(new ByteArrayInputStream(rawBytes));
                if (bImage == null) {
                    failedUrls.add(url);
                    return;
                }

                ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
                ImageIO.write(bImage, "png", pngStream);
                byte[] pngBytes = pngStream.toByteArray();

                ByteBuffer byteBuffer = BufferUtils.createByteBuffer(pngBytes.length);
                byteBuffer.put(pngBytes);
                byteBuffer.flip();

                NativeImage image = NativeImage.read(byteBuffer);

                client.execute(() -> {
                    if (currentCoverId != null) {
                        client.getTextureManager().destroyTexture(currentCoverId);
                    }
                    Identifier newId = Identifier.of("webnowcraft", "cover_" + System.currentTimeMillis());
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                    client.getTextureManager().registerTexture(newId, texture);
                    currentCoverId = newId;
                });

            } catch (Exception e) {
                System.err.println("[Webnowcraft] Error loading image: " + e.getMessage());
                failedUrls.add(url);
            }
        });
    }
}