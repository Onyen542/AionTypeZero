/*
 * Copyright (c) 2015, TypeZero Engine (game.developpers.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of TypeZero Engine nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.typezero.gameserver.services.mail;

import com.aionemu.commons.database.dao.DAOManager;
import org.typezero.gameserver.configs.administration.AdminConfig;
import org.typezero.gameserver.configs.main.LoggingConfig;
import org.typezero.gameserver.dao.InventoryDAO;
import org.typezero.gameserver.dao.MailDAO;
import org.typezero.gameserver.dao.PlayerDAO;
import org.typezero.gameserver.model.gameobjects.Item;
import org.typezero.gameserver.model.gameobjects.Letter;
import org.typezero.gameserver.model.gameobjects.LetterType;
import org.typezero.gameserver.model.gameobjects.player.Mailbox;
import org.typezero.gameserver.model.gameobjects.player.Player;
import org.typezero.gameserver.model.gameobjects.player.PlayerCommonData;
import org.typezero.gameserver.model.items.storage.Storage;
import org.typezero.gameserver.model.items.storage.StorageType;
import org.typezero.gameserver.model.templates.item.Disposition;
import org.typezero.gameserver.model.templates.mail.MailMessage;
import org.typezero.gameserver.network.aion.serverpackets.SM_DELETE_ITEM;
import org.typezero.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import org.typezero.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import org.typezero.gameserver.services.AdminService;
import org.typezero.gameserver.services.HousingBidService;
import org.typezero.gameserver.services.item.ItemFactory;
import org.typezero.gameserver.services.player.PlayerMailboxState;
import org.typezero.gameserver.utils.PacketSendUtility;
import org.typezero.gameserver.utils.ThreadPoolManager;
import org.typezero.gameserver.utils.audit.AuditLogger;
import org.typezero.gameserver.utils.idfactory.IDFactory;
import org.typezero.gameserver.world.World;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kosyachok
 */
public class MailService {

	private static final Logger log = LoggerFactory.getLogger("MAIL_LOG");
	protected Queue<Player> newPlayers;

	public static final MailService getInstance() {
		return SingletonHolder.instance;
	}

	private MailService() {
		newPlayers = new ConcurrentLinkedQueue<Player>();
	}

	/**
	 * TODO split this method
	 *
	 * @param sender
	 * @param recipientName
	 * @param title
	 * @param message
	 * @param attachedItemObjId
	 * @param attachedItemCount
	 * @param attachedKinahCount
	 * @param express
	 */
	public void sendMail(Player sender, String recipientName, String title, String message, int attachedItemObjId, int attachedItemCount,
			int attachedKinahCount, LetterType letterType) {

		if (letterType == LetterType.BLACKCLOUD || recipientName.length() > 16)
			return;

		if (title.length() > 20)
			title = title.substring(0, 20);

		if (message.length() > 1000)
			message = message.substring(0, 1000);

		PlayerCommonData recipientCommonData = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonDataByName(recipientName);

		if (recipientCommonData == null) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.NO_SUCH_CHARACTER_NAME));
			return;
		}

		if ((recipientCommonData.getRace() != sender.getRace()) && sender.getAccessLevel() < AdminConfig.GM_LEVEL) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.MAIL_IS_ONE_RACE_ONLY));
			return;
		}

		Player recipient = World.getInstance().findPlayer(recipientCommonData.getPlayerObjId());
		if (recipient != null) {
			if (!recipient.getMailbox().haveFreeSlots()) {
				PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.RECIPIENT_MAILBOX_FULL));
				return;
			}
		}
		else if (recipientCommonData.getMailboxLetters() > 99) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.RECIPIENT_MAILBOX_FULL));
			return;
		}

		if (!validateMailSendPrice(sender, attachedKinahCount, attachedItemObjId, attachedItemCount))
			return;

		Item attachedItem = null;
		int finalAttachedKinahCount = 0;

		int kinahMailCommission = 0;
		int itemMailCommission = 0;

		Storage senderInventory = sender.getInventory();

		if (attachedItemObjId != 0 && attachedItemCount > 0) {
			Item senderItem = senderInventory.getItemByObjId(attachedItemObjId);

			if (senderItem == null) {
				return;
			}

			if (!AdminService.getInstance().canOperate(sender, null, senderItem, "mail"))
				return;

			float qualityPriceRate;
			switch (senderItem.getItemTemplate().getItemQuality()) {
				case JUNK:
				case COMMON:
					qualityPriceRate = 0.02f;
					break;

				case RARE:
					qualityPriceRate = 0.03f;
					break;

				case LEGEND:
				case UNIQUE:
					qualityPriceRate = 0.04f;
					break;

				case MYTHIC:
				case EPIC:
					qualityPriceRate = 0.05f;
					break;

				default:
					qualityPriceRate = 0.02f;
					break;
			}

			if (senderItem.getItemCount() < attachedItemCount) {
				return;// Client hack
			}

			// Check Mailing untradables with Cash items (Special courier passes)
			if (!senderItem.isTradeable(sender)) {
				Disposition dispo = senderItem.getItemTemplate().getDisposition();
				if (dispo == null || dispo.getId() == 0 || dispo.getCount() == 0) //can not be traded, hack
					return;

				if (senderInventory.getItemCountByItemId(dispo.getId()) >= dispo.getCount()) {
					senderInventory.decreaseByItemId(dispo.getId(), dispo.getCount());
				}
				else
					return;
			}

			// reuse item in case of full decrease of count
			if (senderItem.getItemCount() == attachedItemCount) {
				senderInventory.remove(senderItem);
				PacketSendUtility.sendPacket(sender, new SM_DELETE_ITEM(attachedItemObjId));
				attachedItem = senderItem;
			}
			else if (senderItem.getItemCount() > attachedItemCount) {
				attachedItem = ItemFactory.newItem(senderItem.getItemTemplate().getTemplateId(), attachedItemCount);
				senderInventory.decreaseItemCount(senderItem, attachedItemCount);
			}

			if (attachedItem == null)
				return;

			attachedItem.setEquipped(false);
			attachedItem.setEquipmentSlot(0);
			attachedItem.setItemLocation(StorageType.MAILBOX.getId());
			itemMailCommission = Math.round((attachedItem.getItemTemplate().getPrice() * attachedItem.getItemCount()) * qualityPriceRate);
		}

		/**
		 * Calculate kinah
		 */
		if (attachedKinahCount > 0) {
			if (senderInventory.getKinah() - attachedKinahCount >= 0) {
				finalAttachedKinahCount = attachedKinahCount;
				kinahMailCommission = Math.round(attachedKinahCount * 0.01f);
			}
		}

		int finalMailKinah = 10 + kinahMailCommission + itemMailCommission + finalAttachedKinahCount;

		if (senderInventory.getKinah() > finalMailKinah)
			senderInventory.decreaseKinah(finalMailKinah);
		else {
			AuditLogger.info(sender, "Mail kinah exploit.");
			return;
		}

		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());

		Letter newLetter = new Letter(IDFactory.getInstance().nextId(), recipientCommonData.getPlayerObjId(), attachedItem,
				finalAttachedKinahCount, title, message, sender.getName(), time, true, letterType);

		// first save attached item for FK consistency
		if (attachedItem != null)
			if (!DAOManager.getDAO(InventoryDAO.class).store(attachedItem, recipientCommonData.getPlayerObjId()))
				return;
		// save letter
		if (!DAOManager.getDAO(MailDAO.class).storeLetter(time, newLetter))
			return;

		/**
		 * Send mail update packets
		 */
		if (recipient != null) {
			Mailbox recipientMailbox = recipient.getMailbox();
			recipientMailbox.putLetterToMailbox(newLetter);

			// packets for sender
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.MAIL_SEND_SECCESS));

			// packets for recipient
			PacketSendUtility.sendPacket(recipient, new SM_MAIL_SERVICE(recipientMailbox));
			recipientMailbox.isMailListUpdateRequired = true;

			// if recipient have opened mail list we should update it
			if (recipientMailbox.mailBoxState != 0) {
				boolean isPostman = (recipientMailbox.mailBoxState & PlayerMailboxState.EXPRESS) == PlayerMailboxState.EXPRESS;
				PacketSendUtility.sendPacket(recipient, new SM_MAIL_SERVICE(recipient, recipientMailbox.getLetters(), isPostman));
			}

			if (letterType == LetterType.EXPRESS)
				PacketSendUtility.sendPacket(recipient, SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY);
		}

		if (attachedItem != null) {
			if (LoggingConfig.LOG_MAIL)
				log.info("[MAILSERVICE] [Player: " + sender.getName() + "] send [Item: " + attachedItem.getItemId()
						+ (LoggingConfig.ENABLE_ADVANCED_LOGGING ? "] [Item Name: " + attachedItem.getItemName() + "]" : "]") + " [Count: "
						+ attachedItem.getItemCount() + "] to [Reciever: " + recipientName + "]");
		}

		/**
		 * Update loaded common data and db if player is offline
		 */
		if (!recipientCommonData.isOnline()) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.MAIL_SEND_SECCESS));
			recipientCommonData.setMailboxLetters(recipientCommonData.getMailboxLetters() + 1);
			DAOManager.getDAO(MailDAO.class).updateOfflineMailCounter(recipientCommonData);
		}
	}

	/**
	 * Read letter with specified letter id
	 *
	 * @param player
	 * @param letterId
	 */
	public void readMail(Player player, int letterId) {
		Letter letter = player.getMailbox().getLetterFromMailbox(letterId);
		if (letter == null) {
			log.warn("Cannot read mail " + player.getObjectId() + " " + letterId);
			return;
		}

		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player, letter, letter.getTimeStamp().getTime()));
		letter.setReadLetter();
	}

	/**
	 * @param player
	 * @param letterId
	 * @param attachmentType
	 */
	public void getAttachments(Player player, int letterId, int attachmentType) {
		Letter letter = player.getMailbox().getLetterFromMailbox(letterId);

		if (letter == null)
			return;

		switch (attachmentType) {
			case 0: {
				Item attachedItem = letter.getAttachedItem();
				if (attachedItem == null)
					return;
				if (player.getInventory().isFull()) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
					return;
				}
				player.getInventory().add(attachedItem);
				if (!DAOManager.getDAO(InventoryDAO.class).store(attachedItem, player.getObjectId()))
					return;

				PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(letterId, attachmentType));
				letter.removeAttachedItem();
				break;
			}
			case 1: {
				player.getInventory().increaseKinah(letter.getAttachedKinah());
				PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(letterId, attachmentType));
				letter.removeAttachedKinah();
				break;
			}
		}
	}

	/**
	 * @param player
	 * @param letterId
	 */
	public void deleteMail(Player player, int[] mailObjId) {
		Mailbox mailbox = player.getMailbox();

		for (int letterId : mailObjId) {
			mailbox.removeLetter(letterId);
			DAOManager.getDAO(MailDAO.class).deleteLetter(letterId);
		}
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(mailObjId));
	}

	/**
	 * @param sender
	 * @param attachedKinahCount
	 * @param attachedItemObjId
	 * @param attachedItemCount
	 * @return
	 */
	private boolean validateMailSendPrice(Player sender, int attachedKinahCount, int attachedItemObjId, int attachedItemCount) {
		int itemMailCommission = 0;
		int kinahMailCommission = Math.round(attachedKinahCount * 0.01f);
		if (attachedItemObjId != 0) {
			Item senderItem = sender.getInventory().getItemByObjId(attachedItemObjId);
			if (senderItem == null || senderItem.getItemTemplate() == null)
				return false;
			float qualityPriceRate;
			switch (senderItem.getItemTemplate().getItemQuality()) {
				case JUNK:
				case COMMON:
					qualityPriceRate = 0.02f;
					break;

				case RARE:
					qualityPriceRate = 0.03f;
					break;

				case LEGEND:
				case UNIQUE:
					qualityPriceRate = 0.04f;
					break;

				case MYTHIC:
				case EPIC:
					qualityPriceRate = 0.05f;
					break;

				default:
					qualityPriceRate = 0.02f;
					break;
			}

			itemMailCommission = Math.round((senderItem.getItemTemplate().getPrice() * attachedItemCount) * qualityPriceRate);
		}

		int finalMailPrice = 10 + itemMailCommission + kinahMailCommission;

		if (sender.getInventory().getKinah() >= finalMailPrice)
			return true;

		return false;
	}

	/**
	 * @param player
	 */
	public void onPlayerLogin(Player player) {
		ThreadPoolManager.getInstance().schedule(new MailLoadTask(player), 5000);
	}

	public void refreshMail(Player player) {
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player.getMailbox()));
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player, player.getMailbox().getLetters(), false));
	}

	/**
	 * Task to load all player mail items
	 *
	 * @author ATracer
	 */
	private class MailLoadTask implements Runnable {

		private Player player;

		private MailLoadTask(Player player) {
			this.player = player;
		}

		@Override
		public void run() {
			player.setMailbox(DAOManager.getDAO(MailDAO.class).loadPlayerMailbox(player));
			PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player.getMailbox()));
			HousingBidService.getInstance().onPlayerLogin(player);
		}

	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final MailService instance = new MailService();
	}

}