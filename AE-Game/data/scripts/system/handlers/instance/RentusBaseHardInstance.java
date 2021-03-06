package instance;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import org.typezero.gameserver.ai2.NpcAI2;
import org.typezero.gameserver.ai2.manager.WalkManager;
import org.typezero.gameserver.controllers.effect.PlayerEffectController;
import org.typezero.gameserver.instance.handlers.GeneralInstanceHandler;
import org.typezero.gameserver.instance.handlers.InstanceID;
import org.typezero.gameserver.model.EmotionType;
import org.typezero.gameserver.model.actions.NpcActions;
import org.typezero.gameserver.model.gameobjects.Creature;
import org.typezero.gameserver.model.gameobjects.Npc;
import org.typezero.gameserver.model.gameobjects.StaticDoor;
import org.typezero.gameserver.model.gameobjects.player.Player;
import org.typezero.gameserver.network.aion.serverpackets.SM_DIE;
import org.typezero.gameserver.network.aion.serverpackets.SM_EMOTION;
import org.typezero.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import org.typezero.gameserver.services.NpcShoutsService;
import org.typezero.gameserver.skillengine.SkillEngine;
import org.typezero.gameserver.utils.PacketSendUtility;
import org.typezero.gameserver.world.WorldMapInstance;
import org.typezero.gameserver.world.knownlist.Visitor;
import org.typezero.gameserver.world.zone.ZoneInstance;
import org.typezero.gameserver.world.zone.ZoneName;
import java.util.List;
import java.util.Map;

/**
 * @author Romanz
 */
@InstanceID(300620000)
public class RentusBaseHardInstance extends GeneralInstanceHandler {

	private Map<Integer, StaticDoor> doors;
	private boolean isInstanceDestroyed;

	@Override
	public void onDie(final Npc npc) {
		if (isInstanceDestroyed) {
			return;
		}

		switch (npc.getNpcId()) {
			case 236298:
				spawn(236705, 141.4f, 254.7f, 209.5f, (byte) 0);
				break;
			case 236299:
				doors.get(145).setOpen(true);
                                despawnNpc(instance.getNpc(701156));
				break;
			case 236302:
				doors.get(70).setOpen(true);
				break;
			case 217307:
				doors.get(236).setOpen(true);
				instance.doOnAllPlayers(new Visitor<Player>() {

					@Override
					public void visit(Player player) {
						removeEffects(player);
					}

				});
				startZantarazEvent();
				break;
			case 236300:
				spawn(730401, 193.6f, 436.5f, 262f, (byte) 86);
				Npc ariana = (Npc) spawn(799670, 183.736f, 391.392f, 260.571f, (byte) 26);
				NpcShoutsService.getInstance().sendMsg(ariana, 1500417, ariana.getObjectId(), 0, 5000);
				NpcShoutsService.getInstance().sendMsg(ariana, 1500418, ariana.getObjectId(), 0, 8000);
				NpcShoutsService.getInstance().sendMsg(ariana, 1500419, ariana.getObjectId(), 0, 11000);
				spawnEndEvent(800227, "3002800003", 2000);
				spawnEndEvent(800227, "3002800004", 2000);
				spawnEndEvent(800228, "3002800007", 4000);
				spawnEndEvent(800227, "3002800005", 6000);
				spawnEndEvent(800228, "3002800006", 8000);
				spawnEndEvent(800229, "3002800008", 10000);
				spawnEndEvent(800229, "3002800009", 10000);
				spawnEndEvent(800230, "30028000010", 12000);
				spawnEndEvent(800230, "30028000011", 12000);
				break;
			case 282394:
				spawn(282395, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading());
				despawnNpc(npc);
				break;
			case 283000:
			case 283001:
			case 856015:
				despawnNpc(npc);
				break;
			case 217299:
				final float x = npc.getX();
				final float y = npc.getY();
				final float z = npc.getZ();
				final byte h = npc.getHeading();
				ThreadPoolManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						if (!isInstanceDestroyed) {
							if (x > 0 && y > 0 && z > 0) {
								spawn(217300, x, y, z, h);
							}
						}
					}

				}, 4000);
				ThreadPoolManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						despawnNpc(npc);
					}

				}, 2000);
				break;
		}
	}

	private void startWalk(Npc npc, String walkId) {
		npc.getSpawn().setWalkerId(walkId);
		WalkManager.startWalking((NpcAI2) npc.getAi2());
		npc.setState(1);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}

	private void startZantarazEvent() {
		final Npc ariana = (Npc) spawn(799667, 674.73f, 625.42f, 156f, (byte) 0);
		final Npc priest1 = (Npc) spawn(800198, 679.6f, 634.59f, 156f, (byte) 0);
		final Npc priest2 = (Npc) spawn(800198, 683.3f, 623.59f, 156f, (byte) 0);
		final Npc warrior1 = (Npc) spawn(800196, 684f, 632.61f, 156f, (byte) 0);
		final Npc warrior2 = (Npc) spawn(800196, 687.56f, 622.07f, 156f, (byte) 0);
		final Npc ranger1 = (Npc) spawn(800197, 690.47f, 631.58f, 156f, (byte) 0);
		final Npc ranger2 = (Npc) spawn(800197, 683.09f, 618.9f, 156f, (byte) 0);
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				startWalk(ariana, "30028000017");
				startWalk(priest1, "30028000018");
				startWalk(priest2, "30028000019");
				startWalk(warrior1, "30028000020");
				startWalk(warrior2, "30028000021");
				startWalk(ranger1, "30028000022");
				startWalk(ranger2, "30028000023");
			}

		}, 1000);
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				final Npc invisible = (Npc) spawn(282601, 621.4345f, 617.3071f, 154.125f, (byte) 0);
				invisible.getKnownList().doOnAllPlayers(new Visitor<Player>() {

					@Override
					public void visit(Player player) {
						PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, 0, 482, 65536, invisible.getObjectId()));
					}

				});
				stopWalk(priest1);
				stopWalk(priest2);
				stopWalk(warrior1);
				stopWalk(warrior2);
				stopWalk(ranger1);
				stopWalk(ranger2);
				NpcActions.delete(invisible);
				NpcActions.delete(ariana);
			}

		}, 12000);
	}

	private void stopWalk(Npc npc) {
		npc.getSpawn().setWalkerId(null);
		WalkManager.stopWalking((NpcAI2) npc.getAi2());
	}

	private void spawnEndEvent(int npcId, String walkern, int time) {
		sp(npcId, 193.39548f, 435.56158f, 260.57135f, (byte) 86, time, walkern);
	}

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		doors = instance.getDoors();
		if (Rnd.get(1, 100) > 70) {
			switch(Rnd.get(1, 4)) {
				case 1:
					spawn(218572,  538.6639f, 477.2887f, 145.82251f, (byte) 90);
					break;
				case 2:
					spawn(218572, 377.38275f, 461.9165f, 138.54454f, (byte) 60);
					break;
				case 3:
					spawn(218572, 317.74368f, 623.0686f, 150.33286f, (byte) 45);
					break;
				case 4:
					spawn(218572, 316.23618f, 726.1624f, 163.5f, (byte) 40);
					break;
			}
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(19376);
		effectController.removeEffect(19350);
		effectController.removeEffect(20027);
		effectController.removeEffect(20037);
		effectController.removeEffect(20031);
	}

	private boolean canUseTank() {
		Npc zantaraz = instance.getNpc(217307);
		if (zantaraz != null && !NpcActions.isAlreadyDead(zantaraz)) {
			return true;
		}
		return false;
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701151:
				SkillEngine.getInstance().getSkill(npc, 19909, 60, npc).useNoAnimationSkill();
				despawnNpc(npc);
				break;
			case 701152:
				SkillEngine.getInstance().getSkill(npc, 19910, 60, npc).useNoAnimationSkill();
				despawnNpc(npc);
				break;
			case 218611:
				if (canUseTank()) {
					SkillEngine.getInstance().getSkill(npc, 20027, 60, player).useNoAnimationSkill();
					NpcActions.scheduleRespawn(npc);
				}
				despawnNpc(npc);
				break;
			case 218610:
				if (canUseTank()) {
					SkillEngine.getInstance().getSkill(npc, 19350, 60, player).useNoAnimationSkill();
					NpcActions.scheduleRespawn(npc);
				}
				despawnNpc(npc);
				break;
			case 701097:
				despawnNpc(npc);
				break;
			case 701100:
				if (instance.getNpc(799543) == null) {
					spawn(799543, 506.303f, 613.902f, 158.179f, (byte) 0);
				}
				break;
		}
	}

	private void despawnNpcs(List<Npc> npcs) {
		for (Npc npc : npcs) {
			npc.getController().onDelete();
		}
	}

	private void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time,
		final String walkern) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					Npc npc = (Npc) spawn(npcId, x, y, z, h);
					npc.getSpawn().setWalkerId(walkern);
					startEndWalker(npc);
					unSetEndWalker(npc);
				}
			}

		}, time);
	}

	private void startEndWalker(final Npc npc) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					WalkManager.startWalking((NpcAI2) npc.getAi2());
					npc.setState(1);
					PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
				}
			}

		}, 3000);
	}

	private void unSetEndWalker(final Npc npc) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					stopWalk(npc);
				}
			}

		}, 8000);
	}

	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("RESIDENTIAL_ZONE_300280000")) {
			removeEffects(player);
		}
	}

	@Override
	public boolean onDie(final Player player, Creature lastAttacker) {
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0
				: lastAttacker.getObjectId()), true);

		PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), player.haveSelfRezItem(), 0, 8));
		return true;
	}
}
