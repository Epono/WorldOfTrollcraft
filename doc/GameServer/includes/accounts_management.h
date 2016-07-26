/*
 * accounts_management.h
 *
 *  Created on: Oct 19, 2013
 *      Author: nth
 */

#ifndef ACCOUNTS_MANAGEMENT_H_
#define ACCOUNTS_MANAGEMENT_H_

#include <stdio.h>
#include <string.h>
#include <pthread.h>

#include "network.h"
#include "game_management.h"


void runAuthentificationsThread();
void authentificationThread();


#define ACCOUNTS_FILE "./accounts"

typedef struct account
{
	char *login, *password;
	int pos_x, pos_y;
	char opened; // sert a marquer si ce compte est ouvert ou non
} Account;


/**
 * liste des comptes des joueurs triée selon le login.
 */
typedef struct accountlist
{
	Account *accounts;
	unsigned int number_of_accounts;
	unsigned int max_number_of_accounts;
} AccountsList;

AccountsList* createAccountsList(unsigned int max_number_of_account);
void destroyAccountsList(AccountsList *list);
void expandAccountsList(AccountsList *list, unsigned int new_max_length);

int addAccountToList(AccountsList *list, char *login, char *mdp, int x, int y);
int getAccountPosition(AccountsList *list, char *login);
int removeAccountFromList(AccountsList *list, char *login);

int sendAuthentificationAnswer(Connection *connection, Buffer *send_buffer, char answer_type);

int extractLoginAndPassword(GenericPacket *packet, Buffer *associated_buffer, char ** login, char ** password);
int isCorrectLoginOrPassword(const char *string);

Player* createPlayerForAccount(Account *account, Connection *connection);
void destroyPlayer(Player *player);

AccountsList* loadAccountsFromFile(const char *filename);
int saveAccountsFile(const char *filename, AccountsList *list);

#endif /* ACCOUNTS_MANAGEMENT_H_ */
