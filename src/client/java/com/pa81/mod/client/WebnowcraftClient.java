package com.pa81.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class WebnowcraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("==========================================");
        System.out.println("[Webnowcraft] Mod Initializing...");

        try {
            SpicetifyServer.init();
            System.out.println("[Webnowcraft] Server init called.");
        } catch (Exception e) {
            System.err.println("[Webnowcraft] FAILED to start server:");
            e.printStackTrace();
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            System.out.println("[Webnowcraft] Stopping server...");
            SpicetifyServer.stopServer();
        });

        HudRenderCallback.EVENT.register(new OverlayRenderer());
        System.out.println("==========================================");
    }
}