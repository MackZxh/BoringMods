package net.mack.boringmods.client.gui.button;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.recipebook.RecipeBookGui;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.util.math.MathHelper;

public class SortButtonWidget extends ButtonWidget {
    private org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("boringmods");

    private final Container container;
    private static boolean pressed = false;
    private RecipeBookGui recipeBookGui;

    public SortButtonWidget(int sId, int left, int top, int width, int height,
                            Container cont, RecipeBookGui recipe,
                            ButtonWidget.PressAction pressAction) {
        super(left, top, width, height, "Sort", pressAction);

        this.container = cont;
        this.recipeBookGui = recipe;
    }

    private int getLeft(boolean narrow, int screenWidth) {
        int left;
        if (this.recipeBookGui.isOpen() && !narrow) {
            left = 177 + (screenWidth - 376) / 2;
        } else {
            left = (screenWidth - 176) / 2;
        }

        return left;
    }

    public void draw(int cursorX, int cursorY, float float_1) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer fontRenderer = client.textRenderer;
        client.getTextureManager().bindTexture(WIDGETS_LOCATION);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);


//        logger.info(String.format("Window Width %d, Widnow Scaled Width %d", client.window.getWidth(), client.window.getScaledWidth()));

        int left = this.getLeft(false, client.window.getScaledWidth());
        this.x = left + 144;

        int leftWidth = this.width / 2;
        int rightWidth = this.width - leftWidth;
        int topHeight = this.height / 2;
        int bottomHeight = this.height - topHeight;
        int textureId = this.getYImage(this.isHovered());
        int texX = 0;
        int texY = 46 + textureId * 20;
        this.blit(this.x, this.y, texX, texY, leftWidth, topHeight);
        this.blit(this.x + leftWidth, this.y, 200 - rightWidth, texY, rightWidth, topHeight);
        this.blit(this.x, this.y + topHeight, texX, texY + 20 - bottomHeight, leftWidth, bottomHeight);
        this.blit(this.x + leftWidth, this.y + topHeight, 200 - rightWidth, texY + 20 - bottomHeight, rightWidth, bottomHeight);
        this.renderBg(client, cursorX, cursorY);
        int fontColor = 14737632;
        if (!this.active) {
            fontColor = 10526880;
        } else if (this.isHovered()) {
            fontColor = 16777120;
        }

        this.drawCenteredString(fontRenderer, this.getMessage(), this.x + leftWidth, this.y + topHeight - fontRenderer.fontHeight / 2, fontColor | MathHelper.ceil(this.alpha * 255.0F) << 24);

        int top = this.y - 62;
//        left *= client.window.getScaleFactor();
        if (SortButtonWidget.pressed) {
            GlStateManager.disableDepthTest();
            for (int i = 0; i < this.container.slotList.size(); i++) {
                Slot slot = this.container.slotList.get(i);
                String text = String.format("%d", i);
                client.textRenderer.drawWithShadow(text, slot.xPosition + left, slot.yPosition + top, 0xffaabbcc);
            }
            GlStateManager.enableDepthTest();
        }
    }
}
