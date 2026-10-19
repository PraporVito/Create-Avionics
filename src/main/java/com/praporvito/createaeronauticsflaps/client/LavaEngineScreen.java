package com.praporvito.createaeronauticsflaps.client;

import com.praporvito.createaeronauticsflaps.block.LavaEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LavaEngineScreen extends AbstractContainerScreen<LavaEngineMenu> {
    // Используем текстуру от маленького двигателя, она подойдет
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("createaeronauticsflaps", "textures/gui/lava_engine.png");

    public LavaEngineScreen(LavaEngineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 188;
        this.imageWidth = 176;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}