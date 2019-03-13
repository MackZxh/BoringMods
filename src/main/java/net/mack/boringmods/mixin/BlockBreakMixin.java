package net.mack.boringmods.mixin;

import net.mack.boringmods.impl.ClientInitializer;
import net.mack.boringmods.impl.ModInitializer;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(Block.class)
public abstract class BlockBreakMixin {
    private static final int excavateMaxBlocks = 64;
    private static final int excavateRange = 8;

    @Inject(method = "onBreak",
            at = @At(value = "HEAD"))
    private void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        Block block = state.getBlock();
        boolean isLogBlock = false;

        if (block instanceof LogBlock) {
            isLogBlock = true;
        } else if (!(block instanceof OreBlock || block instanceof RedstoneOreBlock || block == Blocks.OBSIDIAN)) {
            return;
        }

        if (world.isClient() && ClientInitializer.keyExcavate.isPressed() &&
                player.isUsingEffectiveTool(state) &&
                player.getHungerManager().getFoodLevel() > 0) {
            int brokenBlocks = 1;
            ArrayList<BlockPos> blocksToBreak = new ArrayList<>();
            blocksToBreak.add(pos);
            BlockPos currentPos = pos;
            ArrayList<BlockPos> nextToBreak = new ArrayList<>();
            float exhaust = 0;
            ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
            if (null == networkHandler) {
                return;
            }
            ClientConnection connection = networkHandler.getClientConnection();
            while (brokenBlocks < excavateMaxBlocks && player.isUsingEffectiveTool(state)
                    && player.getHungerManager().getFoodLevel() > 0) {
                ArrayList<BlockPos> blocksNeighbour = getNeighbours(world, currentPos, block, isLogBlock);
                blocksNeighbour.removeAll(blocksToBreak);
                for (BlockPos p : blocksNeighbour) {
                    if (brokenBlocks >= excavateMaxBlocks ||
                            player.getHungerManager().getFoodLevel() <= exhaust / 2 ||
                            brokenBlocks >= (player.getMainHandStack().getDurability() - player.getMainHandStack().getDamage())) {
                        break;
                    }
                    if (!blocksToBreak.contains(p) &&
                            player.isUsingEffectiveTool(world.getBlockState(p))) {
                        if (p.distanceTo(pos) <= excavateRange) {
                            nextToBreak.add(p);
                        }
                        world.breakBlock(p, !player.isCreative());
                        connection.sendPacket(ModInitializer.createBreackPacket(p));
                        blocksToBreak.add(p);
                        brokenBlocks++;
                        exhaust = (0.005F * brokenBlocks) * (brokenBlocks / 8.0F + 1);
                    }
                }
                if (nextToBreak.size() == 0) {
                    break;
                }
                currentPos = nextToBreak.get(0);
                nextToBreak.remove(currentPos);
            }
            if (!player.isCreative()) {
                connection.sendPacket(ModInitializer.createEndPacket(brokenBlocks - 1, exhaust));
            }
        }
    }

    private ArrayList<BlockPos> getNeighbours(World world, BlockPos pos, Block block, boolean isLogBlock) {
        ArrayList<BlockPos> neighbours = new ArrayList<>();
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = (isLogBlock ? 0 : -1); z <= 1; ++z) {
                    BlockPos currentPos = pos.add(x, y, z);
                    if (!(0 == x && 0 == y && 0 == z)
                            && world.getBlockState(currentPos).getBlock() == block) {
                        neighbours.add(currentPos);
                    }
                }
            }
        }
        return neighbours;
    }
}
