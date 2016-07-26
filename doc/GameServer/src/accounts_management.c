/*
 * accounts_management.c
 *
 *  Created on: Oct 19, 2013
 *      Author: nth
 */

#include "../includes/accounts_management.h"


void runAuthentificationsThread()
{
	pthread_t thread;
	pthread_create(&thread, NULL, (void * (*)(void *)) authentificationThread, NULL); // permet de lancer un nouveau thread qui appelera la fonction "authentificationThread".
}


void authentificationThread()
{
	// initialisation de la structure sockaddr_in d'ecoute.

	AccountsList *list = loadAccountsFromFile(ACCOUNTS_FILE); // ajouter la détection de l'existence du fichier.
	if(list == NULL)
	{
		errorMessage("Accounts file is corrupted");
		return;
	}

	struct sockaddr_in server;
	initServerSocketAddress(&server);
	int listen_socket = createServerListenSocket(&server);


	ConnectionList connection_list;
	connection_list.nb_connections = 0;

	// initialisation de la fine d'attente des joueurs à charger sur la map et des joueurs déconnectés.
	PlayersQueue players_queues[2];
	initPlayersQueue(&players_queues[0]);
	initPlayersQueue(&players_queues[1]);

	PlayersQueue *new_players_queue = players_queues;
	PlayersQueue *disconnected_players_queue = &players_queues[1];

	// lancement du thread s'occupant de la gestion du déroulement du jeu.

#ifdef RUN_GAME_MANAGEMENT_THREAD
	runGameManagementThread(players_queues);
#endif
	int _continue, return_value;
	char *login, *password;
	Player *player;
	Connection * current_connection;
	int connection_index, account_index;
	GenericPacket packet;
	Buffer recv_buffer, send_buffer;
	initBuffer(&recv_buffer);
	initBuffer(&send_buffer);

	// utilise pour rendr le thread inactif pendant un certain temps.
	int no_activity = 0;
	struct timespec wait_time = {0, 150000}, sleep_return;

	long last_save_time = getTime();
	char save_needed = FALSE;

	while(1)
	{
		no_activity = TRUE;

		acceptNewConnections(listen_socket, &connection_list);
		/// il reste a traiter les packets éventuels de chaque connexion.
		for(connection_index = 0; connection_index < connection_list.nb_connections; connection_index++)
		{
			current_connection = & connection_list.connections[connection_index];
			_continue = TRUE;
			while(_continue)
			{
				return_value = recvPacket(current_connection, &packet, &recv_buffer);
				if(return_value == 1) // traitement du packet recu.
				{
					no_activity = FALSE;
					if(packet.type == AUTHENTIFICATION_REQUEST)
					{
						if(extractLoginAndPassword(&packet, &recv_buffer, &login, &password) < 0) // si la forme des donnees du paquet est incorrecte, on rejette la demande d'authentification.
							sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION);
						else if(!isCorrectLoginOrPassword(login)) // si le login est syntaxiquement incorrecte, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : login \"%s\" is syntactically false", login);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else if(!isCorrectLoginOrPassword(password)) // si le mot de passe est syntaxiquement incorrecte, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : password \"%s\" is syntactically false", password);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else if((account_index = getAccountPosition(list, login)) < 0) // Si le login ne correspond à aucun compte, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : none account matching with login \"%s\".", login);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else if(list->accounts[account_index].opened) // Si le compte correspondant au login est deja ouvert, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : account \"%s\" is already opened.", login);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else if(list->accounts[account_index].opened) // Si le compte correspondant est deja utilise, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : account \"%s\" is already opened.", password, login);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else if(strcmp(password, list->accounts[account_index].password)) // Si le compte correspondant au login a un mot de passe different de celui recu, on rejette la demande d'authentification.
						{
							debugMessage("Authentication has failed : password \"%s\" is incorrect for account \"%s\".", password, login);
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
							{
								warningMessage("Current connection is lost.");
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
							}
						}
						else // l'authentification a reussi, on supprime donc la connection du service de comptes.
						{
							if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, ACCEPT_AUTHENTIFICATION)) == -2)
								warningMessage("Current connection is lost.");

							if(addPlayerToQueue(new_players_queue, (player = createPlayerForAccount(& list->accounts[account_index], current_connection))) < 0) // Si la file d'attente des noueaux joueurs en attente de chargement sur la map est pleine, on rejette la demande d'authentification.
							{
								destroyPlayer(player);
								debugMessage("Authentication has failed : new players queue is full.");
								if((return_value = sendAuthentificationAnswer(current_connection, &send_buffer, REJECT_AUTHENTIFICATION)) == -2)
								{
									warningMessage("Current connection is lost.");
									removeConnectionFromList(& connection_list, connection_index);
									connection_index--;
									_continue = FALSE;
								}
							}
							else
							{
								removeConnectionFromList(& connection_list, connection_index);
								connection_index--;
								_continue = FALSE;
								list->accounts[account_index].opened = TRUE;
								debugMessage("player %s is authenticated", login);
							}
						}
					}
					else // le type du paquet ne concerne pas le service de comptes, donc on l'ignore.
						warningMessage("packet received is not for account management : type is %d", packet.type);
				}
				else if(return_value == 0) // aucun nouveau paquet recu, donc on traite l'eventuelle connexion suivante.
					_continue = FALSE;
				else if(return_value == -1)
					_continue = FALSE;
				else // la connexion est perdue, donc on la supprime de la liste des connexions et on traite l'eventuelle connexion suivante.
				{
					no_activity = FALSE;
					removeConnectionFromList(& connection_list, connection_index);
					connection_index--;
					_continue = FALSE;
				}
			}
		}

		// on traite l'ensemble des joueurs deconnectes.
		while((player = removePlayerFromQueue(disconnected_players_queue)) != NULL)
		{
			no_activity = FALSE;

			account_index = getAccountPosition(list, player->login); // on recupere l'indice du compte associe au joueur 'player'
			if(account_index >= 0) // si le compte est retrouve, on le met a jour.
			{
				list->accounts[account_index].opened = FALSE;
				list->accounts[account_index].pos_x = player->position.x;
				list->accounts[account_index].pos_y = player->position.y;
				save_needed = TRUE;
			}
			destroyPlayer(player);
		}

		if(save_needed && getTime() - last_save_time > 30000000) // sauvegarde des comptes toutes les 30 secondes.
		{
			saveAccountsFile(ACCOUNTS_FILE, list);
			save_needed = FALSE;
			last_save_time = getTime();
		}

		// si aucune activite (reception de paquet ou demande de connexion) n'a eu lieu, alors on rend le thread inactif pendant un certain temps afin de limiter la consommation des ressouces processeur.
		if(no_activity == TRUE)
			nanosleep(&wait_time, &sleep_return);
	}
}




/**
 * Cree de façon dynamique un element de type Player, l'initialise et le renvoie.
 * Retourne NULL en cas d'erreur.
 */
Player* createPlayerForAccount(Account *account, Connection *connection)
{
	Player *player = (Player *) malloc(sizeof(Player));

	player->position.x = account->pos_x;
	player->position.y = account->pos_y;
	player->login = strCreateCopy(account->login);
	memcpy(& player->connection, connection, sizeof(Connection));

	return player;
}

void destroyPlayer(Player *player)
{
	if(player != NULL)
	{
		free(player->login);
		free(player);
	}
}

int sendAuthentificationAnswer(Connection *connection, Buffer *send_buffer, char answer_type)
{
	if(answer_type != REJECT_AUTHENTIFICATION && answer_type != ACCEPT_AUTHENTIFICATION)
		return -1;
	GenericPacket packet;
	packet.data = NULL;
	packet.data_length = 0;
	packet.type = answer_type;
	traceMessage("Authentication packet will be sent");
	return sendPacket(connection, &packet, send_buffer);
}

/**
 * Extrait le login et le mot de passe des données du paquet.
 * Retourne 0 si l'extraction est reussie, -1 en cas d'erreur (un des champs absent ou vide.).
 */
int extractLoginAndPassword(GenericPacket *packet, Buffer *associated_buffer, char ** login, char ** password)
{
	unsigned int login_length=0, password_length=0;
	char *string = (char *) packet->data;

	unsigned int current_index = 0;

	while(login_length < packet->data_length && string[login_length] != '\0')
		login_length++;

	if(login_length == packet->data_length)
	{
		debugMessage("Authentication packet is not correct : none character of end of string.");
		return -1;
	}
	else if(login_length == 0)
	{
		debugMessage("Authentication packet is not correct : login string can't be empty.");
		return -1;
	}


	current_index = login_length + 1;
	if(current_index == packet->data_length)
	{
		debugMessage("Authentication packet is not correct : password is missing.");
		return -1;
	}

	while(current_index < packet->data_length && string[current_index] != '\0')
		current_index++;

	if(current_index == packet->data_length)
	{
		expandBufferLengthWithCpy(associated_buffer, packet->data_length + PACKET_HEADER_LENTGH + 1);
		packet->data = associated_buffer->data + PACKET_HEADER_LENTGH;
		string = (char *) packet->data;
		string[current_index] = '\0';
	}
	else if(current_index == login_length + 1)
	{
		debugMessage("Authentication packet is not correct : password string can't be empty.");
		return -1;
	}

	*login = string;
	*password = & string[login_length + 1];

	debugMessage("\ncouple extracted : %s, %s\n", string,  & string[login_length + 1]);

	return 0;
}


AccountsList* createAccountsList(unsigned int max_number_of_account)
{
	AccountsList *accounts_list = (AccountsList *) malloc(sizeof(AccountsList));

	accounts_list->max_number_of_accounts = max_number_of_account;
	accounts_list->number_of_accounts = 0;

	if(max_number_of_account > 0)
		accounts_list->accounts = (Account *) calloc(max_number_of_account, sizeof(Account));
	else
		accounts_list->accounts = NULL;

	return accounts_list;
}

void destroyAccountsList(AccountsList *list)
{
	if(list != NULL)
	{
		int i;
		if(list->accounts != NULL)
		{
			for(i=0; i < list->number_of_accounts; i++)
			{
				free(list->accounts[i].login);
				free(list->accounts[i].password);
			}
			free(list->accounts);
		}
		free(list);
	}
}

void expandAccountsList(AccountsList *list, unsigned int new_max_length)
{
	if(list != NULL && list->max_number_of_accounts < new_max_length)
	{
		Account *accounts = (Account *) calloc(new_max_length, sizeof(Account));

		if(list->number_of_accounts > 0)
			memcpy(accounts, list->accounts, sizeof(Account) * list->number_of_accounts);
		if(list->accounts != NULL)
			free(list->accounts);
		list->accounts = accounts;
		list->max_number_of_accounts = new_max_length;
	}
}

/**
 * ajoute un compte a la liste.
 * retourne la position a laquelle le compte a ete insere, ou -1 en cas d'erreur ou si le login d'un compte de la liste est identique a celui du compte a ajouter.
 */
int addAccountToList(AccountsList *list, char *login, char *password, int x, int y)
{
	if(list == NULL || login == NULL || password == NULL) return-1;


	if(list->number_of_accounts == 0) // si la liste est vide, on étend sa taille et on ajoute le compte en premiere position.
	{
		expandAccountsList(list, 10);
		list->accounts[0].login = strCreateCopy(login);
		list->accounts[0].password = strCreateCopy(password);
		list->accounts[0].pos_x = x;
		list->accounts[0].pos_y = y;
		list->accounts[0].opened = FALSE;
		list->number_of_accounts++;

		debugMessage("account (%s : %s : %d : %d) added to list", login, password, x, y);
		return 0;
	}

	// on recherche la position à laquelle inserer le nouveau compte, si aucun compte de la liste à un login identique a celui du compte a creer.

	int min = 0, max = list->number_of_accounts - 1;
	int middle = (max + 1)/2;
	int result;

	while((result = strcmp(login, list->accounts[middle].login)) && min < max)
	{
		if(result < 0)
		{
			max = middle-1;
			middle = min + (max-min+1)/2;
		}
		else
		{
			min = max < middle+1 ? max : middle+1;
			middle = min + (max-min+1)/2;
		}
	}

	if(result == 0) // le login d'un compte de la liste est indentique a celui du compte a creer, on renvoie donc -1.
	{
		warningMessage("Account %s already exists", login);
		return -1;
	}
	else // le login est valide, on insere donc le compte dans la liste en respectant le critere de tri sur le login.
	{
		if(list->number_of_accounts == list->max_number_of_accounts)
			expandAccountsList(list, list->max_number_of_accounts + 10);

		int account_position = result < 0 ? middle : middle + 1;
		int i;
		for(i = (int) list->number_of_accounts; i > account_position; i--) // decalage des comptes dont le login est superieur au login du compte a ajouter.
			memcpy(& list->accounts[i], & list->accounts[i-1],sizeof(Account));

		list->accounts[account_position].login = strCreateCopy(login);
		list->accounts[account_position].password = strCreateCopy(password);
		list->accounts[account_position].pos_x = x;
		list->accounts[account_position].pos_y = y;
		list->accounts[0].opened = FALSE;

		list->number_of_accounts++;

		debugMessage("account (%s : %s : %d : %d) added to list", login, password, x, y);

		return account_position;
	}
}

/**
 * si un compte de la liste est associee au login 'login', il est supprime et la fonction retourne 0.
 * Sinon, retourne -2 si compte non trouve, sinon -1.
 */
int removeAccountFromList(AccountsList *list, char *login)
{
	int account_position = getAccountPosition(list, login);

	if(account_position < 0)
		return account_position;

	free(list->accounts[account_position].login);
	free(list->accounts[account_position].password);

	int i;
	for(i = account_position+1; i < (int) list->number_of_accounts; i++) // decalage des comptes dont le login est superieur au login du compte a ajouter.
		memcpy(& list->accounts[i-1], & list->accounts[i],sizeof(Account));

	list->number_of_accounts--;

	debugMessage("Account matching with login \"%s\" removed from list", login);
	return 0;
}

/**
 * Recherche la position du compte designe par le login 'login'.
 * Retourne la position du compte ayant le login 'login s'il existe, sinon -2 s'il n'est pas trouve, sinon -1.
 */
int getAccountPosition(AccountsList *list, char *login)
{
	if(list == NULL || login == NULL) return-1;
	if(list->number_of_accounts == 0)
		return -2;

	int min = 0, max = list->number_of_accounts - 1;
	int middle = (max + 1)/2;
	int result;

	while((result = strcmp(login, list->accounts[middle].login)) && min < max)
	{
		if(result < 0)
		{
			max = middle-1;
			middle = min + (max-min+1)/2;
		}
		else
		{
			min = max < middle+1 ? max : middle+1;
			middle = min + (max-min+1)/2;
		}
	}

	if(result == 0) // le login d'un compte de la liste correspond a celui recherche, on renvoie donc la position de ce compte.
		return middle;

	else
		return -2;
}


AccountsList* loadAccountsFromFile(const char *filename)
{
	int file_descriptor = open(filename, O_RDONLY);

	if(file_descriptor < 0)
	{
		if(errno == ENOENT)
			errorMessage("File \"%s\" doesn't exist.", filename);
		else
			errorMessage("File \"%s\" can't be opened.", filename);
		return NULL;
	}

	unsigned int nb_accounts;
	int return_value = readUIntFromFile(file_descriptor, &nb_accounts, ';');
	if(return_value < 0)
	{
		if(return_value == -2)
			errorMessage("Number of accounts not found in file.");
		else
			errorMessage("An error has occurred while reading file.");

		close(file_descriptor);
		return NULL;
	}
	goAfterCharacterInFile(file_descriptor, ';');

	AccountsList *list = createAccountsList(nb_accounts);
	traceMessage("Accounts list of length %d created", (int) nb_accounts);
	char buffer1[50], buffer2[50], *login, *password;
	unsigned int tmp;
	int x, y;

	int i;
	for(i=0; i<nb_accounts; i++)
	{
		// lecture du login.
		if(readStringFromFile(file_descriptor, buffer1, 50, ':') < 0 || (login = extractStringWithoutSpace(buffer1)) == NULL)
		{
			destroyAccountsList(list);
			close(file_descriptor);
			return NULL;
		}
		if(!isCorrectLoginOrPassword(login))
		{
			warningMessage("Login \"%s\" readed in file is syntactically false. Accounts list can't be load from file.", login);
			destroyAccountsList(list);
			close(file_descriptor);
			return NULL;
		}
		goAfterCharacterInFile(file_descriptor, ':');

		// Lecture du mot de passe.
		if(readStringFromFile(file_descriptor, buffer2, 50, ':') < 0 || (password = extractStringWithoutSpace(buffer2)) == NULL)
		{
			destroyAccountsList(list);
			close(file_descriptor);
			return NULL;
		}
		if(!isCorrectLoginOrPassword(password))
		{
			warningMessage("Password \"%s\" readed in file is syntactically false. Accounts list can't be load from file.", password);
			destroyAccountsList(list);
			close(file_descriptor);
			return NULL;
		}
		goAfterCharacterInFile(file_descriptor, ':');

		// lecture de x.
		return_value = readUIntFromFile(file_descriptor, &tmp, ':');
		if(return_value == -1)
		{
			errorMessage("An error has occurred while reading file.");
			destroyAccountsList(list);
			close(file_descriptor);
			return NULL;
		}
		else if(return_value == -2)
			x = -1;
		else
			x = (int) tmp;
		goAfterCharacterInFile(file_descriptor, ':');

		// Lecture de y si necessaire.
		if(x != -1)
		{
			return_value = readUIntFromFile(file_descriptor, &tmp, ';');
			if(return_value == -1)
			{
				errorMessage("An error has occurred while reading file.");
				destroyAccountsList(list);
				close(file_descriptor);
				return NULL;
			}
			else if(return_value == -2)
				x = y = -1;
			else
				y = (int) tmp;
		}
		else
			y = -1;

		goAfterCharacterInFile(file_descriptor, ';');

		addAccountToList(list, login, password, x, y);
	}

	close(file_descriptor);
	return list;
}

/**
 * Verifie si un mot de passe ou un login est syntaxiquement correct. Les caracteres acceptés sont : a-zA-Z0-9_
 * Retourne 'TRUE' si c'est vrai, sinon 'FALSE'.
 */
int isCorrectLoginOrPassword(const char *string)
{
	if(string == NULL) return FALSE;

	char *current_character = (char *) string;
	if(*current_character == '\0' || !isalnum(*current_character))
		return FALSE;
	current_character++;

	while(*current_character != '\0' && (isalnum(*current_character) || *current_character == '_'))
		current_character++;

	if(*current_character == '\0')
		return TRUE;
	return FALSE;
}

int saveAccountsFile(const char *filename, AccountsList *list)
{
	if(filename == NULL || list == NULL)
		return -1;

	int file_descriptor = open(filename, O_WRONLY | O_CREAT, S_IRUSR|S_IWUSR|S_IRGRP);

	if(file_descriptor < 0)
	{
		if(errno == ENOENT)
			errorMessage("File \"%s\" doesn't exist.", filename);
		else
			errorMessage("File \"%s\" can't be opened.", filename);
		return -1;
	}

	int length;

	char buffer[50], field_separator[] = " : ", account_separator[] = " ;\n";
	// ecriture du nombre de comptes.
	length = sprintf(buffer, "%u ;\n\n", list->number_of_accounts);
	write(file_descriptor, buffer, length);

	unsigned int i;
	Account *account;

	for(i=0; i < list->number_of_accounts; i++)
	{
		account = &list->accounts[i];
		write(file_descriptor, account->login, strlen(account->login));
		write(file_descriptor, field_separator, strlen(field_separator));

		write(file_descriptor, account->password, strlen(account->password));
		write(file_descriptor, field_separator, strlen(field_separator));

		if(account->pos_x >= 0)
		{
			length = sprintf(buffer, "%u", (unsigned int) account->pos_x);
			write(file_descriptor, buffer, length);
		}
		write(file_descriptor, field_separator, strlen(field_separator));

		if(account->pos_y >= 0)
		{
			length = sprintf(buffer, "%u", (unsigned int) account->pos_y);
			write(file_descriptor, buffer, length);
		}
		write(file_descriptor, account_separator, strlen(account_separator));

		debugMessage("Account  (%s : %s : %d : %d) saved in file.", account->login, account->password, account->pos_x, account->pos_y);
	}

	close(file_descriptor);
	return 0;
}


