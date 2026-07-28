package fr.lacaleche.mui.testmod;

import com.mojang.blaze3d.platform.InputConstants;
import fr.lacaleche.mui.MuiApi;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestmodClient implements ClientModInitializer {

    public static final String MOD_ID = "mui-lite-test";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static TestmodClient instance;
    private KeyMapping openDemo;

    public static TestmodClient getInstance() {
        return instance;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        this.openDemo = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mui-lite-test.open_demo", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, KeyMapping.CATEGORY_MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (this.openDemo.consumeClick()) MuiApi.openScreen(new DemoFragment(), client.screen);
        });
        LOGGER.info("Press F8 to open the ModernUI MC Lite demo");
    }

    public static final class DemoFragment extends Fragment {

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable DataSet savedInstanceState) {
            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            content.setBackground(new ColorDrawable(Color.rgb(24, 28, 38)));

            TextView title = new TextView(requireContext());
            title.setText("ModernUI MC Lite");
            title.setTextSize(28);
            title.setTextColor(Color.WHITE);
            title.setGravity(Gravity.CENTER);
            content.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button close = new Button(requireContext());
            close.setText("Close");
            close.setTextColor(Color.WHITE);
            close.setTextSize(18);
            close.setGravity(Gravity.CENTER);
            close.setBackground(new ColorDrawable(Color.rgb(51, 102, 204)));
            close.setOnClickListener(view -> Minecraft.getInstance().schedule(
                    () -> Minecraft.getInstance().setScreen(null)));
            LinearLayout.LayoutParams closeLayout = new LinearLayout.LayoutParams(180, 64);
            closeLayout.topMargin = 24;
            content.addView(close, closeLayout);
            return content;
        }
    }

}
