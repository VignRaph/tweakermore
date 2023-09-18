/*
 * This file is part of the TweakerMore project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023  Fallen_Breath and contributors
 *
 * TweakerMore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TweakerMore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TweakerMore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.fallenbreath.tweakermore.impl.features.infoView;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.WorldUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.impl.features.infoView.beacon.BeaconEffectRenderer;
import me.fallenbreath.tweakermore.impl.features.infoView.commandBlock.CommandBlockContentRenderer;
import me.fallenbreath.tweakermore.impl.features.infoView.redstoneDust.RedstoneDustUpdateOrderRenderer;
import me.fallenbreath.tweakermore.impl.features.infoView.respawnBlock.RespawnBlockExplosionViewerBlock;
import me.fallenbreath.tweakermore.impl.features.infoView.villager.VillagerTradeRenderer;
import me.fallenbreath.tweakermore.util.FabricUtil;
import me.fallenbreath.tweakermore.util.PositionUtil;
import me.fallenbreath.tweakermore.util.ThrowawayRunnable;
import me.fallenbreath.tweakermore.util.render.context.RenderContext;
import me.fallenbreath.tweakermore.util.render.RenderUtil;
import me.fallenbreath.tweakermore.util.render.TweakerMoreIRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InfoViewRenderer implements TweakerMoreIRenderer, IClientTickHandler {
    private static final InfoViewRenderer INSTANCE = new InfoViewRenderer();
    private static final List<AbstractBlockInfoViewer> CONTENT_PREVIEWERS = Lists.newArrayList(
            new RedstoneDustUpdateOrderRenderer(),
            new CommandBlockContentRenderer(),
            new RespawnBlockExplosionViewerBlock(),
            new BeaconEffectRenderer()
    );

    private static final List<AbstractEntityInfoViewer> ENTITY_PREVIEWERS = Lists.newArrayList(
            new VillagerTradeRenderer()
    );

    private InfoViewRenderer () {
        TickHandler.getInstance().registerClientTickHandler(this);
    }

    public static InfoViewRenderer getInstance () {
        return INSTANCE;
    }

    @Override
    public void onRenderWorldLast (RenderContext context) {
        if (!TweakerMoreConfigs.INFO_VIEW.getBooleanValue()) {
            return;
        }

        List<AbstractBlockInfoViewer> blockViewers = CONTENT_PREVIEWERS.stream().
                filter(AbstractBlockInfoViewer::isRenderEnabled).
                collect(Collectors.toList());

        List<AbstractEntityInfoViewer> entityViewers = ENTITY_PREVIEWERS.stream().
                filter(AbstractEntityInfoViewer::isRenderEnabled).
                collect(Collectors.toList());


        if (blockViewers.isEmpty() && entityViewers.isEmpty()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        World world = WorldUtils.getBestWorld(mc);
        World clientWorld = mc.world;
        ClientPlayerEntity player = mc.player;
        if (world == null || clientWorld == null || player == null) {
            return;
        }

        for (Entity entity:mc.world.getEntities()) {
            if (mc.player.distanceTo(entity) < TweakerMoreConfigs.INFO_VIEW_ENTITY_TARGET_DISTANCE.getDoubleValue()){
                for (AbstractEntityInfoViewer viewer : entityViewers) {
                    Vec3d vec3d = player.getRotationVec(1.0F).normalize();
                    //#if MC < 11500
                    //$$ Vec3d vec3d2 = new Vec3d(entity.x - player.x, entity.y - player.y, entity.z - player.z);
                    //#else
                    Vec3d vec3d2 = new Vec3d(entity.getX() - player.getX(), entity.getEyeY() - player.getEyeY(), entity.getZ() - player.getZ());
                    //#endif
                    double d = vec3d2.length();
                    vec3d2 = vec3d2.normalize();
                    double e = vec3d.dotProduct(vec3d2);
                    boolean pointingAt = e > 1.0D - 0.025D / d && player.canSee(entity);

                    boolean enabled = viewer.isValidTarget(pointingAt);
                    if (enabled && viewer.shouldRenderFor(world, entity.getPos(), entity)) {
                        viewer.render(context, world, entity.getPos(), entity);
                    }
                }
            }
        }

        if (blockViewers.isEmpty()) {
            return;
        }

        Vec3d camPos = mc.player.getCameraPosVec(RenderUtil.tickDelta);
        Vec3d camVec = mc.player.getRotationVec(RenderUtil.tickDelta);
        double reach = TweakerMoreConfigs.INFO_VIEW_TARGET_DISTANCE.getDoubleValue();
        double angle = Math.PI * TweakerMoreConfigs.INFO_VIEW_BEAM_ANGLE.getDoubleValue() / 2 / 180;
        HitResult target = mc.player.rayTrace(reach, RenderUtil.tickDelta, false);
        BlockPos crossHairPos = target instanceof BlockHitResult ? ((BlockHitResult) target).getBlockPos() : null;



        List<BlockPos> positions = Lists.newArrayList();
        positions.addAll(PositionUtil.beam(camPos, camPos.add(camVec.normalize().multiply(reach)), angle, PositionUtil.BeamMode.BEAM));
        if (crossHairPos != null && !positions.contains(crossHairPos)) {
            positions.add(crossHairPos);
        }

        // debug
        if (TweakerMoreConfigs.TWEAKERMORE_DEBUG_BOOL.getBooleanValue() && FabricUtil.isDevelopmentEnvironment()) {
            Function<BlockPos, Double> distanceGetter = pos -> camPos.distanceTo(PositionUtil.centerOf(pos));
            double maxDis = positions.stream().map(distanceGetter).mapToDouble(x -> x).max().orElse(1);
            positions.forEach(pos -> me.fallenbreath.tweakermore.util.render.TextRenderer.create().
                    color(0xFFFFFF00 | (int) (255.0 * distanceGetter.apply(pos) / maxDis)).
                    atCenter(pos).text("x").render()
            );
        }

        // sort by distance in descending order, so we render block info from far to near (simulating depth test)
        List<Pair<BlockPos, Double>> posPairs = Lists.newArrayList();
        positions.forEach(pos -> posPairs.add(Pair.of(pos, camPos.squaredDistanceTo(PositionUtil.centerOf(pos)))));
        posPairs.sort(Comparator.comparingDouble(Pair::getSecond));
        Collections.reverse(posPairs);

        ChunkCachedWorldAccess chunkCache = new ChunkCachedWorldAccess(world);
        blockViewers.forEach(AbstractBlockInfoViewer::onInfoViewStart);
        for (Pair<BlockPos, Double> pair : posPairs) {
            BlockPos blockPos = pair.getFirst();
            boolean isCrossHairPos = blockPos.equals(crossHairPos);
            Supplier<BlockState> blockState = Suppliers.memoize(() -> clientWorld.getBlockState(blockPos));  // to avoid async block get --> faster
            Supplier<BlockEntity> blockEntity = Suppliers.memoize(() -> chunkCache.getBlockEntity(blockPos));
            ThrowawayRunnable sync = ThrowawayRunnable.of(() -> this.syncBlockEntity(world, blockPos));

            for (AbstractBlockInfoViewer viewer : blockViewers) {
                boolean enabled = viewer.isValidTarget(isCrossHairPos);
                if (enabled && viewer.shouldRenderFor(world, blockPos, blockState.get(), blockEntity.get())) {
                    if (viewer.requireBlockEntitySyncing() && !(world instanceof ServerWorld)) {
                        sync.run();
                    }
                    viewer.render(context, world, blockPos, blockState.get(), blockEntity.get());
                }
            }
        }
        blockViewers.forEach(AbstractBlockInfoViewer::onInfoViewEnd);
    }

    @SuppressWarnings ("unused")
    private void syncBlockEntity (World world, BlockPos blockPos) {
        // serverDataSyncer do your job here
    }

    private void syncEntity (World world, Entity entity) {
        // serverDataSyncer do your job here
        if (world.isClient()){

        }
    }

    @Override
    public void onClientTick (MinecraftClient client) {
        CONTENT_PREVIEWERS.forEach(AbstractBlockInfoViewer::onClientTick);
        ENTITY_PREVIEWERS.forEach(AbstractEntityInfoViewer::onClientTick);
    }
}
