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


package com.aionengine.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionengine.loginserver.model.Account;

/**
 * DAO that manages accounts.
 *
 * @author SoulKeeper, Dr2co
 */
public abstract class AccountDAO implements DAO {

	/**
	 * Returns account by name or null
	 *
	 * @param name account name
	 * @return account object or null
	 */
	public abstract Account getAccount(String name);

	public abstract Account getAccount(int id);

	/**
	 * Retuns account id or -1 in case of error
	 *
	 * @param name name of account
	 * @return id or -1 in case of error
	 */
	public abstract int getAccountId(String name);

	/**
	 * Reruns account count If error occured - returns -1
	 *
	 * @return account count
	 */
	public abstract int getAccountCount();

	/**
	 * Inserts new account to database. Sets account ID to id that was generated by DB.
	 *
	 * @param account account to insert
	 * @return true if was inserted, false in other case
	 */
	public abstract boolean insertAccount(Account account);

	/**
	 * Updates account in database
	 *
	 * @param account account to update
	 * @return true if was updated, false in other case
	 */
	public abstract boolean updateAccount(Account account);

	/**
	 * Updates lastServer field of account
	 *
	 * @param accountId  account id
	 * @param lastServer last accessed server
	 * @return was updated successful or not
	 */
	public abstract boolean updateLastServer(int accountId, byte lastServer);

	/**
	 * Updates last ip that was used to access an account
	 *
	 * @param accountId account id
	 * @param ip        ip address
	 * @return was update successful or not
	 */
	public abstract boolean updateLastIp(int accountId, String ip);

	/**
	 * Get last ip that was used to access an account
	 *
	 * @param accountId account id
	 * @return ip address
	 */
	public abstract String getLastIp(int accountId);

	/**
	 * Updates last mac that was used to access an account
	 *
	 * @param accountId account id
	 * @param mac       mac address
	 * @return was update successful or not
	 */
	public abstract boolean updateLastMac(int accountId, String mac);

	/**
	 * Updates account membership
	 *
	 * @param accountId account id
	 * @return was update successful or not
	 */
	public abstract void updateMembership(int accountId);

	public abstract boolean updateMembership(Account account);

	/**
	 * Deletion of all accounts, inactive for more than dayOfInactivity days
	 *
	 * @param daysOfInactivity
	 */
	public abstract void deleteInactiveAccounts(int daysOfInactivity);

	/**
	 * Returns uniquire class name for all implementations
	 *
	 * @return uniquire class name for all implementations
	 */
	@Override
	public final String getClassName() {
		return AccountDAO.class.getName();
	}

}