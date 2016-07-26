/*
 * reseau.h
 *
 *  Created on: Oct 12, 2013
 *      Author: nth
 */

#ifndef NETWORK_H_
#define NETWORK_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <pthread.h>
#include <netinet/tcp.h>
#include <fcntl.h>
#include <errno.h>

#include "utilities.h"

extern int errno;

#define SERVER_PORT 6798

// nombre maximum de demande de connexion dans la file d'attente au niveau du protocole TCP.
#define TCP_CONNECTION_QUEUE_LENGTH 10
// nombre maximal de joueurs connectés sur la carte.
#define MAX_NB_CONNECTIONS_AUTHENTIFICATION_THREAD 100

#define MAX_QUEUE_LENGTH 20

#define MAX_IDLE_TIME 500000 // temps maximum (d'inactivite sur une connexion) a attendre avant l'envoi d'un ping de type PING_REQUEST.

// structure contenant les elements nécessaire pour la partie réseau d'un joueur authentifié.
typedef struct
{
	int socket;
	long int last_time_activity;
	char ping_sent;
} Connection;

// liste des personnes en attente d'authentification.
typedef struct
{
	Connection connections[MAX_NB_CONNECTIONS_AUTHENTIFICATION_THREAD];
	int nb_connections;
} ConnectionList;

int connectionListFull(ConnectionList *connection_list);
int addConnectionToList(ConnectionList *connection_list, Connection *connection);
int removeConnectionFromList(ConnectionList *connection_list, int position);

void initServerSocketAddress(struct sockaddr_in *address);
int createServerListenSocket(struct sockaddr_in *server_address);
void acceptNewConnections(int listening_socket, ConnectionList *connection_list);

#define PACKET_HEADER_LENTGH sizeof(unsigned char) + 2*sizeof(int) // taille de l'entête des packets envoyés sur le réseau.
#define PACKET_ID_SEQUENCE 0x2d4a68e9

typedef enum
{
	// accounts management
	AUTHENTIFICATION_REQUEST = 0,
	ACCEPT_AUTHENTIFICATION = 1,
	REJECT_AUTHENTIFICATION = 2,

	// game management.
	SEND_CONTEXT = 32,
	MOVE_REQUEST = 33,
	PLAYER_UPDATE = 34,
	REJECT_MOVE = 35,
	DISCONNECTION = 36,
	ADD_PLAYER = 37,
	REMOVE_PLAYER = 38,

	// utilisés uniquement par la couche réseau.
	PING_REQUEST = 64,
	PING_ANSWER = 65
} PacketType;

// structure utilisé pour la représentation des paquets.
typedef struct
{
	unsigned int data_length;
	char type; /// type de packet, exemple : demande de création de compte, d'authentification, de déplacement, ....
	unsigned char *data;
} GenericPacket;

int sendPacket(Connection *connection, GenericPacket *packet, Buffer *buffer);
int recvPacket(Connection *connection, GenericPacket *packet, Buffer *buffer);

#endif /* NETWORK_H_ */
