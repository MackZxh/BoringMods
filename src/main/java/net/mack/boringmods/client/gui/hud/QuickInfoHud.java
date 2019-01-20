package net.mack.boringmods.client.gui.hud;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.datafixers.DataFixUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.tag.TagManager;
import net.minecraft.text.TextFormat;
import net.minecraft.util.BlockHitResult;
import net.minecraft.util.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class QuickInfoHud extends Drawable {
    private final MinecraftClient client;
    private final FontRenderer fontRenderer;
    @Nullable
    private WorldChunk chunkClient;
    @Nullable
    private CompletableFuture<WorldChunk> chunkFuture;
    @Nullable
    private ChunkPos chunkPos;
    @Nullable
    private ChunkNibbleArray lightingArray;

    private org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("BoringMods");

    public QuickInfoHud(MinecraftClient mcClient) {
        this.client = mcClient;
        this.fontRenderer = mcClient.fontRenderer;
        this.logger.info("QuickInfo Hud initialized.");
    }

    public void resetChunk() {
        this.chunkFuture = null;
        this.chunkClient = null;
        this.logger.info("Reset Chunk.");
    }

    public void draw(float esp) {
        this.client.getProfiler().push("quickinfo");

        GlStateManager.pushMatrix();

        this.drawInfos();

        this.drawLighting();

        GlStateManager.popMatrix();

        this.client.getProfiler().pop();
    }

    private void drawLighting() {
        if (null == this.lightingArray)
            return;
        for (Byte l : this.lightingArray.asByteArray()) {
                Byte b = l.byteValue();
        }
    }

    private void drawInfos() {
        int top = 2;
        int scaleWidth = this.client.window.getScaledWidth();
        int lineHeight = this.fontRenderer.fontHeight + 1;
        for (String line : getInfos()) {
            if (!Strings.isNullOrEmpty(line)) {
                int lineWidth = this.fontRenderer.getStringWidth(line);
                int left = scaleWidth - lineWidth - 2;
                drawRect(left - 1, top - 1, left + lineWidth + 1, top + lineHeight + 1, Color.blue.getRGB());
                this.fontRenderer.draw(line, left, top, Color.lightGray.getRGB());
            }
        }
    }

    private List<String> getInfos() {
        List<String> infos = new ArrayList<>();
        Entity player = this.client.getCameraEntity();
        if (null == player) {
            player = this.client.player;
        }
        BlockPos pos = player.getPos();
        Direction facing = player.getHorizontalFacing();
        infos.add(String.format("%d, %d, %d : %s", pos.getX(), pos.getY(), pos.getZ(), facing.asString()));

        infos.add(getTimeDesc());

        // Lighting
        ChunkPos posChunk = new ChunkPos(pos);
        if (!Objects.equals(this.chunkPos, posChunk)) {
            this.chunkPos = posChunk;
            this.resetChunk();
        }
        World world = this.getWorld();
        if (world.isBlockLoaded(pos)) {
            WorldChunk chunk = this.getClientChunk();
            if (!chunk.isEmpty()) {
                infos.add(String.format("Client: %d total, %d sky, %d block",
                        chunk.getLightLevel(pos, 0), world.getLightLevel(LightType.SKY_LIGHT, pos), world.getLightLevel(LightType.BLOCK_LIGHT, pos)));
                chunk = this.getChunk();
                if (null != chunk) {
                    LightingProvider provider = world.getChunkManager().getLightingProvider();
                    infos.add(String.format("Server: %d sky, %d block",
                            provider.get(LightType.SKY_LIGHT).getLightLevel(pos), provider.get(LightType.BLOCK_LIGHT).getLightLevel(pos)));
                    this.lightingArray = provider.get(LightType.BLOCK_LIGHT).getChunkLightArray(pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }

        ClientPlayNetworkHandler net = this.client.getNetworkHandler();
        TagManager tagManager = null;
        if (null != net) {
            tagManager = net.getTagManager();
        }
        Entity entity = this.client.targetedEntity;
        if (null != entity) {
            infos.add(TextFormat.UNDERLINE + String.valueOf(Registry.ENTITY_TYPE.getRawId(entity.getType())));
            TextFormat format = TextFormat.GRAY;
            if (entity instanceof Monster) {
                format = TextFormat.RED;
            } else if (entity instanceof PassiveEntity) {
                format = TextFormat.GREEN;
            }
            infos.add(format + entity.getDisplayName().getFormattedText());
//        } else if (null != this.client.hitResult && this.client.hitResult.getType() == HitResult.Type.BLOCK) {
        } else if (null != this.client.hitResult && this.client.hitResult.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult)this.client.hitResult).getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            infos.add(TextFormat.UNDERLINE + String.valueOf(Registry.BLOCK.getId(block)));
            infos.add(block.getTextComponent().getFormattedText());

            // properties
            ImmutableSetMultimap<Property<?>, Comparable<?>> entries = state.getEntries().asMultimap();
            for (Property<?> key : entries.keySet()) {
                ImmutableSet<Comparable<?>> value = entries.get(key);
            }

            // tags
            if (null != tagManager) {
                for (Identifier id : tagManager.blocks().getTagsFor(block)) {
                    infos.add(String.format("#%s", id));
                }
            }
        }

        HitResult hitFluid = player.rayTrace(20.0D, 0.0F, true);
//        if (null != hitFluid && hitFluid.getType() == HitResult.Type.BLOCK) {
        if (null != hitFluid && hitFluid.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult)hitFluid).getBlockPos();
            FluidState state = world.getFluidState(pos);
            Fluid fluid = state.getFluid();
            infos.add(TextFormat.UNDERLINE + String.valueOf(Registry.FLUID.getId(fluid)));
            infos.add(((fluid instanceof LavaFluid) ? TextFormat.RED : null) +
                    state.getBlockState().getBlock().getTextComponent().getFormattedText());

            // properties

            // tags
            if (null != tagManager) {
                for (Identifier id : tagManager.fluids().getTagsFor(fluid)) {
                    infos.add(String.format("#%s", id));
                }
            }
        }

        return infos;
    }

    private String getTimeDesc() {
        long totalTime = this.getWorld().getTimeOfDay();
        long realDays = (totalTime + 6000) % 24000;
        long timeOfDays = totalTime % 24000;
        long hours = ((timeOfDays + 6000) / 1000) % 24;
        long minutes = ((timeOfDays % 1000) * 60) / 1000;

        return String.format("Days %d, %02d:%02d", realDays, hours, minutes);
    }

    private World getWorld() {
        return DataFixUtils.orElse(Optional.ofNullable(this.client.getServer()).map((integratedServer) -> integratedServer.getWorld(this.client.world.dimension.getType())), this.client.world);
    }

    @Nullable
    private WorldChunk getChunk() {
        if (null == this.chunkFuture) {
            IntegratedServer integratedServer = this.client.getServer();
            if (null != integratedServer) {
                ServerWorld serverWorld = integratedServer.getWorld(this.client.world.dimension.getType());
                if (null != serverWorld && null != this.chunkPos) {
                    this.chunkFuture = serverWorld.method_16177(this.chunkPos.x, this.chunkPos.z, false);
                }
            }
        }
        if (null == this.chunkFuture) {
            this.chunkFuture = CompletableFuture.completedFuture(this.getClientChunk());
        }

        return this.chunkFuture.getNow(null);
    }

    private WorldChunk getClientChunk() {
        if (null == this.chunkClient && null != this.chunkPos) {
            this.chunkClient = this.client.world.getWorldChunk(this.chunkPos.x, this.chunkPos.z);
        }

        return this.chunkClient;
    }
}
