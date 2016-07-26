/*
 * game_management.h
 *
 * Author : Thibaut Genevois
 */

#ifndef GAME_MANAGEMENT_H_
#define GAME_MANAGEMENT_H_

#include <stdio.h>
#include "pile.h"
#include "network.h"
#include "utilities.h"

/// Map File ///

#define TREE_CHAR 't'
#define WATER_CHAR 'w'
#define GRASS_CHAR 'g'
#define ROCK_CHAR 'r'
#define FREE_CHAR 'f'

#define MAX_HEIGHT 75
#define MAX_HEIGHT_STRING 2

#define MAX_LENGTH 100
#define MAX_LENGTH_STRING 3

#define MAX_LENGTH_OF_PARAMETERS_STRING 7

#define FREE_CELL_UNION_TYPE 2
#define SET_CELL_UNION_TYPE 1
#define PLAYER_CELL_UNION_TYPE 0

#define LEFT_MOVE 1
#define UP_MOVE 2
#define RIGHT_MOVE 3
#define DOWN_MOVE 4

/// Sets ///

typedef enum
{
	TREE = 1, WATER = 2, GRASS = 3, ROCK = 4
} SetType;

typedef struct
{
	int union_type;
	SetType set_enum;
} SetCell;

/// Connected players ///

#define MAX_NUMBER_OF_CONNECTED_PLAYERS 20

typedef struct
{
	int x;
	int y;
} Position;

// Position == -1 -1 at first connection

typedef struct player
{
	int index;
	Position position;
	char *login;
	Connection connection;
} Player;

typedef struct
{
	int union_type;
	Player *player;
} PlayerCell;

typedef struct
{
	Player *players[MAX_NUMBER_OF_CONNECTED_PLAYERS];
	int nb_players;
	Pile free_index_pile;
} PlayerManagement;

/// Map ///

typedef struct
{
	int max_number;
	int number;
	Position *array;
} FreePositions;

typedef union
{
	int union_type;
	SetCell set_cell;
	PlayerCell player_cell;
} Cell;

/// Map structure ///

typedef struct
{
	Cell **cells;
	PlayerManagement player_management;
	int height, length;
} WorldMap;

/*Represents the players that have been accepted but not yet 'served'*/

typedef struct
{
	Player *players[MAX_QUEUE_LENGTH];
	int first, last, length;
	pthread_mutex_t mutex;
} PlayersQueue;

//Related to the thread in itself

void runGameManagementThread(PlayersQueue *player_queues);
void GameManagementThread(PlayersQueue *player_queues);

//Related to PlayersQueue

void initPlayersQueue(PlayersQueue *queue);
int addPlayerToQueue(PlayersQueue *queue, Player *player_to_remove);
Player* removePlayerFromQueue(PlayersQueue *queue);

//Related to PlayerManagement

void initPlayerManagement(PlayerManagement *player_management);
int addPlayerToPlayerManagement(PlayerManagement *player_management, Player *player_to_remove);
int removePlayerFromPlayerManagement(PlayerManagement *player_management, Player *player_to_remove);

//Map generation related functions and methods

int buildWorldMap(WorldMap *world_map, FreePositions *free_positions);
int correctMapFileSyntax(FILE *map_file, int *lenght, int *height);
char getChar(FILE *map_file);
void setCellContentFromMapFile(int isFreeCell, char *setChar, Cell *cell);
int allocateMemForWorldMapAndContent(int height, int length, WorldMap *world_map);

//Related to FreePositions

int allocateMemForFreePositions(FreePositions *free_positions, int number_of_free_cells);
void fillFreePositions(WorldMap *world_map, FreePositions *free_positions);
int positionIsFree(FreePositions *free_positions, Position *position);
int addFreePosition(FreePositions *free_positions, Position *position);
int removeFreePosition(FreePositions *free_positions, Position *position);
int generateRandomFreePosition(WorldMap *world_map, Position *position, FreePositions *free_positions);

//Player interaction with map related methods and functions

int serveFirstWaitingPlayerInQueue(WorldMap *world_map, PlayersQueue *incoming_players, PlayersQueue *outgoing_players, FreePositions *free_positions, PlayerManagement *player_management, Buffer *send_buffer);
void putPlayerOnMapAndPlayerManagement(PlayerManagement *player_management, WorldMap *world_map, Player *player_to_remove, FreePositions *free_positions);
void removePlayerFromMapAndPlayerManagementAndSendToAll(Buffer *send_buffer, PlayersQueue *outgoing_players, PlayerManagement *player_management, WorldMap *world_map, Player *player_to_remove, FreePositions *free_positions);
void updatePlayerPositionOnMap(Player *player, WorldMap *world_map, Position *new_position);
int playerCanMoveToPosition(Player *player, WorldMap *world_map, unsigned char *direction, FreePositions *free_positions, Position *new_position);

//Game management network related methods

int sendContextPacketToNewPlayers(Player *player_to_remove, PlayerManagement *player_management, Buffer *send_buffer);
int sendAddedPlayerPacketToPlayer(Player *new_player, Player *player_to_send_to, Buffer *send_buffer);
int sendRemovedPlayerPacketToPlayer(Player *player_to_remove, Player *player_to_send_to, Buffer *send_buffer);
int sendPlayerPositionChangedPacketToPlayer(Player *player_that_moved, Position *previous_position, Player *player_to_send_to, Buffer *send_buffer);
int sendCouldNotMovePlayerPacketToPlayer(Player *player_to_send_to, Buffer *send_buffer);
void dealWithIncommingPacket(GenericPacket *packet, PlayerManagement *player_management, WorldMap *world_map, PlayersQueue *outgoing_players, FreePositions *free_positions, Player *player, Buffer *send_buffer);

#endif /* GAME_MANAGEMENT_H_ */
