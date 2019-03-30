package net.mack.boringmods.client.gui.button;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mack.boringmods.client.options.ModOption;
import net.minecraft.client.gui.widget.ButtonWidget;

@Environment(EnvType.CLIENT)
public class OptionButtonWidget extends ButtonWidget {
    private ModOption modOption;

    public OptionButtonWidget(int x, int y, int width, int height, ModOption option, String title, ButtonWidget.PressAction pressAction) {
        super(x, y, width, height, title, pressAction);
        this.modOption = option;
    }
}
