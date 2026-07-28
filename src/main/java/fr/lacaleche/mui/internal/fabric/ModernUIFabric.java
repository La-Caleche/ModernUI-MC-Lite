package fr.lacaleche.mui.internal.fabric;

import fr.lacaleche.mui.internal.ModernUIClient;
import fr.lacaleche.mui.internal.UIManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates the ModernUI application before renderer bootstrap and owns client shutdown wiring. */
public final class ModernUIFabric extends ModernUIClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("mui-lite");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UIManager.destroy());
        LOGGER.info("ModernUI MC Lite application initialized");
    }
}
