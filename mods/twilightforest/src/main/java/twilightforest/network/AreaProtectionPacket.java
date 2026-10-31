package twilightforest.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import twilightforest.TwilightForestMod;
import twilightforest.entity.ProtectionBox;
import twilightforest.init.TFParticleType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AreaProtectionPacket implements CustomPacketPayload {

	private static final AtomicInteger NEXT_CLIENT_ID = new AtomicInteger(-1);

	public static final Type<AreaProtectionPacket> TYPE = new Type<>(TwilightForestMod.prefix("add_protection_box"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AreaProtectionPacket> STREAM_CODEC = CustomPacketPayload.codec(AreaProtectionPacket::write, AreaProtectionPacket::new);

	private final List<BoundingBox> sbb;
	private final BlockPos pos;

	public AreaProtectionPacket(List<BoundingBox> sbb, BlockPos pos) {
		this.sbb = sbb;
		this.pos = pos;
	}

	public AreaProtectionPacket(FriendlyByteBuf buf) {
		this.sbb = new ArrayList<>();
		int len = buf.readInt();
		for (int i = 0; i < len; i++) {
			this.sbb.add(new BoundingBox(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));
		}
		this.pos = buf.readBlockPos();
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeInt(this.sbb.size());
		this.sbb.forEach(box -> {
			buf.writeInt(box.minX());
			buf.writeInt(box.minY());
			buf.writeInt(box.minZ());
			buf.writeInt(box.maxX());
			buf.writeInt(box.maxY());
			buf.writeInt(box.maxZ());
		});
		buf.writeBlockPos(this.pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@SuppressWarnings("Convert2Lambda")
	public static void handle(AreaProtectionPacket message, IPayloadContext ctx) {
		//ensure this is only done on clients as this uses client only code
		if (ctx.flow().isClientbound()) {
			ctx.enqueueWork(new Runnable() {
				@Override
				public void run() {
					ClientLevel level = ctx.player().level() instanceof ClientLevel clientLevel ? clientLevel : Minecraft.getInstance().level;
					message.sbb.forEach(box -> addProtectionBox(level, box));

					for (int i = 0; i < 20; i++) {
						double vx = level.getRandom().nextGaussian() * 0.02D;
						double vy = level.getRandom().nextGaussian() * 0.02D;
						double vz = level.getRandom().nextGaussian() * 0.02D;

						double x = message.pos.getX() + 0.5D + level.getRandom().nextFloat() - level.getRandom().nextFloat();
						double y = message.pos.getY() + 0.5D + level.getRandom().nextFloat() - level.getRandom().nextFloat();
						double z = message.pos.getZ() + 0.5D + level.getRandom().nextFloat() - level.getRandom().nextFloat();

						level.addParticle(TFParticleType.PROTECTION.get(), x, y, z, vx, vy, vz);
					}
				}
			});
		}
	}

	static void addProtectionBox(ClientLevel level, BoundingBox bounds) {
		for (Entity entity : level.entitiesForRendering()) {
			if (entity instanceof ProtectionBox box && box.lifeTime > 0 && box.matches(bounds)) {
				box.resetLifetime();
				return;
			}
		}
		ProtectionBox box = new ProtectionBox(level, bounds);
		// 26.3 client levels return zero from getNextEntityId; these visual-only
		// entities have no server spawn packet to supply an ID. Avoid live IDs
		// because ClientLevel.addEntity would otherwise replace that entity.
		int id;
		do {
			id = NEXT_CLIENT_ID.getAndUpdate(previous -> previous == Integer.MIN_VALUE ? -1 : previous - 1);
		} while (level.getEntity(id) != null);
		box.setId(id);
		level.addEntity(box);
	}
}
