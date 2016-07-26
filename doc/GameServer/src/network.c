/*
 * reseau.c
 *
 *  Created on: Oct 12, 2013
 *      Author: nth
 */
#include "../includes/network.h"

// ces macro-fonctions sont accessibles uniquement dans ce fichier.
#define sendPingRequest(connection) sendPing(connection, PING_REQUEST)
#define sendPingAnswer(connection) sendPing(connection, PING_ANSWER)

// ces fonctions sont accessibles uniquement dans ce fichier.
static int recvGenericPacket(int socket, GenericPacket *packet, Buffer *buffer);
static int sendGenericPacket(int socket, GenericPacket *packet, Buffer *buffer);
static int sendPing(Connection *connection, char ping_type);



/**
 * Cette fonction envoie un packet de type PING_REQUEST ou PING_ANSWER.
 * Retourne : valeurs de retour identiques celles de la fonction sendPacket.
 */
static int sendPing(Connection *connection, char ping_type)
{
	if(connection->ping_sent)
		return 0;

	char buffer[PACKET_HEADER_LENTGH];

	((int *) buffer)[0] = htonl(PACKET_ID_SEQUENCE);
	((int *) buffer)[1] = 0;
	buffer[2*sizeof(int)] = ping_type;

	int return_value = send(connection->socket, buffer, PACKET_HEADER_LENTGH, 0);

	if(return_value < 0)
	{
		int error = errno;
		if(error == EWOULDBLOCK)
		{
			errorMessage("Packet can't be send yet. Try again later.");
			return -1;
		}
		else if(error == ENOTCONN || error == EPIPE)
		{
			warningMessage("Connection is lost. Packet can't be sent. Socket will be closed");
			close(connection->socket);
			return -2;
		}
		else
			return -1;
	}
	connection->ping_sent = TRUE;
	return 0;
}

/**
 * Cree une socket d'ecoute attache a l'adresse passee en parametre.
 * Retourne le descripteur de la socket creee, ou -1 en cas d'erreur.
 */
int createServerListenSocket(struct sockaddr_in *server_address)
{
	// création de la socket avec les paramètres suivants :
	// PF_INET : famille de protocoles utilisant le protocole IP.
	// SOCK_STREAM : socket en mode connecté.
	// IPPROTO_TCP : utilisation du protocole TCP.
	int listening_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (listening_socket < 0) // si la création du socket échoue, on affiche un message sur la sortie standard d'erreur (stderr) et on ferme le programme
	{
		errorMessage("Error : listen socket can't be created");
		return -1;
	}

	if (bind(listening_socket, (struct sockaddr *) server_address, sizeof(struct sockaddr_in)) < 0)
	{
		errorMessage("Error : bind");
		close(listening_socket);
		return -1;
	}

	/// cette option permet de rendre les fonctions d'acceptation de connexions (accept), d'envoi de données (send) et de lecture (recv) non bloquantes.
	/// caractéristique essentielle pour traiter plusieurs connexions dans un seul thread.
	int mode = fcntl(listening_socket, F_GETFL);
	fcntl(listening_socket, F_SETFL, mode | O_NONBLOCK);


	if (listen(listening_socket, TCP_CONNECTION_QUEUE_LENGTH) < 0)
	{
		errorMessage("Error : listen");
		close(listening_socket);
		return -1;
	}

	return listening_socket;
}

/**
 * Initialise l'adresse du serveur.
 */
void initServerSocketAddress(struct sockaddr_in *address)
{
	address->sin_family = AF_INET; // famille de protocoles IPV4
	address->sin_port = htons(SERVER_PORT); // htons : Host TO Network Short
	memset(&address->sin_zero, 0, sizeof(address->sin_zero));
	address->sin_addr.s_addr = htonl(INADDR_ANY); /// htonl : Host TO Network Long /// connection sur toutes les addresses de la machine.
}

void acceptNewConnections(int listening_socket, ConnectionList *connection_list)
{
	int socket;
	Connection connection;
	int optval;
	size_t optlen = sizeof(int);

	// on accepte toutes les demandes de connexion en attente (dans la limite du nombre maximum de connexions simultanées supportées).
	while ((!connectionListFull(connection_list)) && (connection.socket = accept(listening_socket, NULL, 0)) >= 0)
	{
		// Cette opion permet d'envoyer immédiatement toutes les données. Par defaut, TCP stocke les données dans un buffer
		// et les envoie dès que la quantité de données à envoyer est suffisament importante.
		optval = 1;
		setsockopt(connection.socket, IPPROTO_TCP, TCP_NODELAY, &optval, optlen);

		// Cette option permet de spécifier le nombre de secondes d'inactivité (sur la connexion) a attendre avant de tester la connexion.
		optval = 5;
		setsockopt(connection.socket, IPPROTO_TCP, TCP_KEEPIDLE, &optval, optlen);

		// Cette option permet de spécifier le nombre de secondes entre chaque test de la connexion (ie entre chaque "ping" envoyé).
		optval = 2;
		setsockopt(connection.socket, IPPROTO_TCP, TCP_KEEPINTVL, &optval, optlen);

		// Cette option permet de spécifier le nombre de tests de la connexion à effectuer avant de la considerer comme perdue et de fermer le socket.
		optval = 3;
		setsockopt(connection.socket, IPPROTO_TCP, TCP_KEEPCNT, &optval, optlen);

		// Cette option permet d'activer le mode keep alive. Ce mode permet de verifier la persistance de la connexion,
		// meme si aucun echange n'est realise via cette connection depuis un certain temps.
		optval = 1;
		setsockopt(connection.socket, SOL_SOCKET, SO_KEEPALIVE, &optval, optlen);

		fcntl(connection.socket, F_SETFL, fcntl(connection.socket, F_GETFL) | O_NONBLOCK);

		connection.last_time_activity = getTime(); // initialisation.
		connection.ping_sent = FALSE;

		if(addConnectionToList(connection_list, &connection) == -1)
			errorMessage("erreur concernant la liste des sockets : socket invalide.");
	}

}


static int sendGenericPacket(int socket, GenericPacket *packet, Buffer *buffer)
{
	if (packet == NULL || buffer == NULL || socket < 0)
		return -1;

	unsigned int total_lentgh = packet->data_length + PACKET_HEADER_LENTGH;

	/// si le buffer n'est pas assez grand, on le remplace par un nouveau de longueur suffisante.
	if (buffer->buffer_length < total_lentgh)
		expandBufferLength(buffer, total_lentgh);

	unsigned char *ptr = buffer->data;

	/// remplissage de l'entête du packet.

	*((int *) ptr) = htonl(PACKET_ID_SEQUENCE); /// insertion de la séquence de début de trame.
	ptr = &ptr[sizeof(int)];

	*((int *) ptr) = htonl((int) packet->data_length); // insertion de la taille des données à envoyer (en octet(s)).
	ptr = &ptr[sizeof(int)];

	*((char *) ptr) = packet->type; // insertion du type de données à envoyer.
	ptr = &ptr[1];

	if(packet->data_length > 0)
		memcpy(ptr, packet->data, packet->data_length); // insertion des données à envoyer.

	return send(socket, buffer->data, (size_t) total_lentgh, 0); // envoie de la trame construite précédement.
}

/**Cette fonction permet d'envoyer le packet 'packet' via le socket spécifié en paramètre.
 * le buffer spécifié en paramètre ne doit pas être modifié.
 *
 * retourne : 	_ 0  si l'envoi l'envoi du packet a reussi.
 * 				_ -1 si l''envoi a échoue mais la connexion n'est pas perdue.
 * 				_ -2 si l'envoie a echoue et la connexion est perdue (le socket est automatiquement fermé).
 *
 */
int sendPacket(Connection *connection, GenericPacket *packet, Buffer *buffer)
{
	if(connection == NULL) return -1;

	int return_value =  sendGenericPacket(connection->socket, packet, buffer);

	if(return_value < 0)
	{
		int error = errno;
		if(error == EWOULDBLOCK)
		{
			errorMessage("Packet can't be send yet. Try again later.");
			return -1;
		}
		else if(error == ENOTCONN || error == EPIPE)
		{
			warningMessage("Connection is lost. Packet can't be sent. Socket will be closed");
			close(connection->socket);
			return -2;
		}
		else
			return -1;
	}
	else
	{
		//connection->last_time_activity = getTime();
		return 0;
	}
}


static int recvGenericPacket(int socket, GenericPacket *packet, Buffer *buffer)
{
	if (socket < 0 || packet == NULL || buffer == NULL)
		return -1;

	int sequence = 0;
	unsigned int data_length, total_length;
	int return_value;
	int packet_id = htonl(PACKET_ID_SEQUENCE);

	// 1 : detection de la séquence de debut de packet.
	while ((return_value = recv(socket, &sequence, sizeof(int), MSG_PEEK)) == sizeof(int) && sequence != packet_id)
	{
		recv(socket, &sequence, 1, 0);
		sequence = 0;
	}

	if (return_value == -1)
		return -1;
	else if(return_value == 0)
		return -2;
	else if (return_value < sizeof(int)) // s'il n'y a plus assez d'octets à lire.
		return 0;

	// on augmente la taille du buffer si nécessaire.
	expandBufferLength(buffer, 2 * sizeof(int));

	// 2 : on lit la longueur des données.
	return_value = recv(socket, buffer->data, 2 * sizeof(unsigned int), MSG_PEEK);

	if (return_value == -1)
		return -1;
	else if(return_value == 0)
		return -2;
	else if (return_value < 2 * sizeof(unsigned int)) // s'il n'y a plus assez d'octets à lire.
		return 0;

	data_length = (unsigned int) ntohl(((int *) buffer->data)[1]);
	total_length = data_length + PACKET_HEADER_LENTGH;

	expandBufferLength(buffer, total_length);

	// on vérifie si le packet a été reçu en entier.
	return_value = recv(socket, buffer->data, total_length, MSG_PEEK);

	if (return_value == -1)
		return -1;
	else if(return_value == 0)
		return -2;
	else if (return_value < total_length) // s'il n'y a plus assez d'octets à lire.
		return 0;

	return_value = recv(socket, buffer->data, total_length, 0); // le packet a été reçu en entier, on l'extrait du buffer du socket.

	// on reconstruit le packet.
	packet->data_length = data_length;
	packet->type = ((char *) buffer->data)[2 * sizeof(int)];
	packet->data = &buffer->data[PACKET_HEADER_LENTGH];

	return 1;
}

/**Cette fontion permet de récupérer un packet sur le socket spécifié en paramètre.
 * Le packet récupéré est retourné dans le paramètre 'packet';
 * le buffer spécifié en paramètre est utilisé pour la construction du packet.
 *
 * Valeur de retour :
 * 		1 si un packet à été récupéré,
 * 		0 s'il n'y a aucun packet en attente,
 * 		-1 en cas d'erreur.
 * 		-2 si la connexion a été perdue (le socket est automatiquement ferme et devient donc invalide).
 */
int recvPacket(Connection *connection, GenericPacket *packet, Buffer *buffer)
{
	if(connection == NULL) return -1;

	int return_value;
	do
	{
		return_value = recvGenericPacket(connection->socket, packet, buffer);
		if(return_value == 1)
		{
			if(packet->type == PING_ANSWER)
				sendPingAnswer(connection);

			connection->last_time_activity = getTime();
			connection->ping_sent = FALSE;
		}
	}while(return_value == 1 && (packet->type == PING_ANSWER || packet->type == PING_REQUEST));

	if(return_value == -1)
	{
		int error = errno;
		if(error == EWOULDBLOCK)
		{
			if(connection->last_time_activity > MAX_IDLE_TIME)
				return sendPingRequest(connection);
			return 0;
		}

		errorMessage("Error %d happened while running", error);
		return -1;
	}
	else if(return_value == -2)
	{
		warningMessage("Connection is lost. Socket will be automatically closed.");
		return -2;
	}
	else if(return_value == 0 && connection->last_time_activity > MAX_IDLE_TIME)
	{
		sendPingRequest(connection);
	}
	return return_value;
}

/**
 * fonctions de manipulation des listes de connexions.
 */

int connectionListFull(ConnectionList *connection_list)
{
	return (connection_list->nb_connections == MAX_NB_CONNECTIONS_AUTHENTIFICATION_THREAD);
}

/** Ajoute la connexion spécifiée en parametre a la liste des connexions.
 *  Retourne une valeur négative en cas d'erreur, sinon 0.
 */
int addConnectionToList(ConnectionList *connection_list, Connection *connection)
{
	if (connection_list == NULL || connection == NULL ||connection->socket < 0)
		return -1;
	if (connection_list->nb_connections == MAX_NB_CONNECTIONS_AUTHENTIFICATION_THREAD)
		return -2;

	memcpy(& connection_list->connections[connection_list->nb_connections], connection, sizeof(Connection));
	connection_list->nb_connections ++;
	return 0;
}

/// retire le socket de la liste présent à la position spécifiée en paramètre /// retourne une valeur négative en cas d'erreur, sinon 0 ///
int removeConnectionFromList(ConnectionList *connection_list, int position)
{
	if (connection_list == NULL || position < 0 || position >= connection_list->nb_connections)
		return -1;

	connection_list->nb_connections--;
	memcpy(& connection_list->connections[position], & connection_list->connections[connection_list->nb_connections], sizeof(Connection));
	return 0;
}

