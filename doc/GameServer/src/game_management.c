/*
 * game_management.c
 *
 * Author : Thibaut Genevois
 */

#include "../includes/game_management.h"

void runGameManagementThread(PlayersQueue *player_queues)
{
	pthread_t thread;
	pthread_create(&thread, NULL, (void * (*)(void *)) GameManagementThread, (void *) player_queues);
}

/**
 * Main function for game management
 */
void GameManagementThread(PlayersQueue *player_queues)
{
	PlayersQueue *incoming_players = player_queues; // Players that wait to enter the map.
	PlayersQueue *outgoing_players = &player_queues[1]; // Players that asked to disconnect or with whom we lost contact

	WorldMap world_map;
	FreePositions free_positions;

	GenericPacket packet;
	Buffer receive_buffer, send_buffer;
	initBuffer(&receive_buffer);
	initBuffer(&send_buffer);

	//A few necessary calls to set things that will be used later.
	initPlayerManagement(&world_map.player_management);
	srand(time(NULL));

	//If the map building sequence has failed, we quit the program.
	if (buildWorldMap(&world_map, &free_positions) == -1)
		exit(EXIT_FAILURE);

	int i, error_code;

	int no_activity = 0;
	struct timespec wait_time = {0, 150000}, sleep_return;

	while (1)
	{
		no_activity = TRUE;

		if (serveFirstWaitingPlayerInQueue(&world_map, incoming_players, outgoing_players, &free_positions, &world_map.player_management, &send_buffer) != 0)
			errorMessage("Error trying to serve a player.");

		//Let's see if a packet concerning a particular socket was received. No need to test if no players are currently playing
		if (world_map.player_management.nb_players > 0)
		{
			for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
			{
				if (world_map.player_management.players[i] != NULL)
				{
					error_code = recvPacket(&(world_map.player_management.players[i]->connection), &packet, &receive_buffer);

					if (error_code == -2)
					{
						removePlayerFromMapAndPlayerManagementAndSendToAll(&send_buffer, outgoing_players, &world_map.player_management, &world_map, world_map.player_management.players[i], &free_positions);
						no_activity = FALSE;
					}
					else if (error_code == 1)
					{
						dealWithIncommingPacket(&packet, &world_map.player_management, &world_map, outgoing_players, &free_positions, world_map.player_management.players[i], &send_buffer);
						no_activity = FALSE;
					}
				}
			}
		}

		// if 0 packet received and 0 connection request, the thread will be "sleeping" for a defined time.
		if(no_activity == TRUE)
					nanosleep(&wait_time, &sleep_return);
	}
}

/**
 * Initializes a PlayersQueue
 */
void initPlayersQueue(PlayersQueue *queue)
{
	if (queue != NULL)
	{
		queue->first = 0;
		queue->last = -1;
		queue->length = 0;
		pthread_mutex_init(&queue->mutex, NULL);
	}
}

int addPlayerToQueue(PlayersQueue *queue, Player *player)
{
	pthread_mutex_lock(&queue->mutex);
	if (queue->length == MAX_QUEUE_LENGTH || player == NULL)
	{
		pthread_mutex_unlock(&queue->mutex);
		return -1;
	}

	queue->length++;
	queue->last = (queue->last + 1) % MAX_QUEUE_LENGTH;
	queue->players[queue->last] = player;

	pthread_mutex_unlock(&queue->mutex);

	return 0;
}

Player* removePlayerFromQueue(PlayersQueue *queue)
{
	pthread_mutex_lock(&queue->mutex);
	if (queue->length == 0)
	{
		pthread_mutex_unlock(&queue->mutex);
		return NULL;
	}

	Player *player = queue->players[queue->first];
	queue->length--;
	queue->first = (queue->first + 1) % MAX_QUEUE_LENGTH;

	pthread_mutex_unlock(&queue->mutex);

	return player;
}

void initPlayerManagement(PlayerManagement *player_management)
{
	player_management->nb_players = 0;

	int i;

	for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
		player_management->players[i] = NULL;

	player_management->free_index_pile = generatePileOfIndex(MAX_NUMBER_OF_CONNECTED_PLAYERS);
}

int addPlayerToPlayerManagement(PlayerManagement *player_management, Player *player)
{
	if (player_management->nb_players == MAX_NUMBER_OF_CONNECTED_PLAYERS)
		return -1;

	int index = topOfPile(player_management->free_index_pile);

	player_management->free_index_pile = removeTopOfPile(player_management->free_index_pile);
	player_management->players[index] = player;
	player_management->nb_players++;

	player->index = index;

	return 0;
}
int removePlayerFromPlayerManagement(PlayerManagement *player_management, Player *player)
{
	if (player_management->nb_players == 0)
		return -1;

	player_management->free_index_pile = putOnTopOfPile(player_management->free_index_pile, player->index);
	player_management->players[player->index] = NULL;
	player_management->nb_players--;

	return 0;
}

/*
 * If an error occurred during the process of loading the map from the file the function returns -1.
 */
int buildWorldMap(WorldMap *world_map, FreePositions *free_positions)
{
	FILE* map_file = NULL;
	map_file = fopen("standard_map.txt", "r");

	if (map_file == NULL)
	{
		errorMessage("Error trying to read the standard_map.txt file.");
		return -1;
	}

	int height, length;

	if (!correctMapFileSyntax(map_file, &length, &height))
	{
		errorMessage("Bad syntax in standard_map.txt file.");
		fclose(map_file);
		return -1;
	}

	world_map->height = height;
	world_map->length = length;

	if (allocateMemForWorldMapAndContent(height, length, world_map) == -1)
	{
		errorMessage("Map memory allocation failed.");
		fclose(map_file);
		return -1;
	}

	rewind(map_file);

	int y = 0, x = 0, number_of_free_cells = 0;
	char char_from_file;

	do
	{
		char_from_file = fgetc(map_file);

		if (char_from_file == FREE_CHAR)
		{
			setCellContentFromMapFile(1, NULL, &(world_map->cells[y][x]));
			x++;
			number_of_free_cells++;
		}

		else if (char_from_file == GRASS_CHAR || char_from_file == ROCK_CHAR || char_from_file == TREE_CHAR || char_from_file == WATER_CHAR)
		{
			setCellContentFromMapFile(0, &char_from_file, &(world_map->cells[y][x]));
			x++;
		}

		if (x == length)
		{
			y++;
			x = 0;
		}

	} while (char_from_file != EOF);

	fclose(map_file);

	if (allocateMemForFreePositions(free_positions, number_of_free_cells) == -1)
	{
		errorMessage("Free position array memory allocation failed.");
		return -1;
	}

	fillFreePositions(world_map, free_positions);

	return 0;
}

/*
 * "Simple" function that checks the validity of the parameters of a map file and
 * its overall syntax. Correct syntax is "length;height;"
 * ex : 2;1;
 * 		r,w,
 * 		r,f;
 */
int correctMapFileSyntax(FILE *map_file, int *lenght, int *height)
{
	int line_length = 0, number_of_stop_char = 0, stop_char_indexes[2];
	char char_from_file;

	char_from_file = getChar(map_file);
	line_length++;

	if (char_from_file == EOF || char_from_file == 10)
		return 0;

	while (char_from_file != 10 && char_from_file != EOF)
	{
		char_from_file = getChar(map_file);
		if (char_from_file != 10)
		{
			line_length++;
			if (char_from_file == 59)
			{
				stop_char_indexes[number_of_stop_char] = line_length;
				number_of_stop_char++;
			}
		}
	}

	if (line_length > MAX_LENGTH_OF_PARAMETERS_STRING || number_of_stop_char != 2 || line_length < 4)
		return 0;

	rewind(map_file);

	if (stop_char_indexes[0] == 2)
	{
		if (stop_char_indexes[1] == 4)
		{
			if (line_length > 4)
				return 0;
		} else if (stop_char_indexes[1] == 5)
		{
			if (line_length > 5)
				return 0;
		} else
			return 0;
	} else if (stop_char_indexes[0] == 3)
	{
		if (stop_char_indexes[1] == 5)
		{
			if (line_length > 5)
				return 0;
		} else if (stop_char_indexes[1] == 6)
		{
			if (line_length > 6)
				return 0;
		} else
			return 0;
	} else if (stop_char_indexes[0] == 4)
	{
		if (stop_char_indexes[1] == 6)
		{
			if (line_length > 6)
				return 0;
		} else if (stop_char_indexes[1] == 7)
		{
			if (line_length > 7)
				return 0;
		} else
			return 0;
	} else
		return 0;

	int i, j = 0, length_param = 1;
	char height_array[MAX_HEIGHT_STRING], length_array[MAX_LENGTH_STRING];

	for (i = 0; i < line_length; i++)
	{
		char_from_file = getChar(map_file);
		if (char_from_file < 58 && char_from_file > 47)
		{
			if (length_param)
			{
				length_array[j] = char_from_file;
				j++;
			} else
			{
				height_array[j] = char_from_file;
				j++;
			}
		} else if (char_from_file == 59)
		{
			length_param = 0;
			j = 0;
		} else
			return 0;
	}

	*lenght = atoi(length_array);
	*height = atoi(height_array);

	if (*lenght < 1 || *lenght > 100 || *height < 1 || *height > 75)
		return 0;

	//should be char 10 LineFeed
	char_from_file = getChar(map_file);

	int file_length = (*lenght * 2) + 1, set_char = 1, end_of_line = 0, position_in_line = 1;
	j = (file_length * *height) - 1;

	for (i = 1; i <= j; i++)
	{
		char_from_file = getChar(map_file);

		if (!(char_from_file == 10 || char_from_file == 44 || char_from_file == 59 || char_from_file == FREE_CHAR || char_from_file == GRASS_CHAR || char_from_file == ROCK_CHAR || char_from_file == TREE_CHAR || char_from_file == WATER_CHAR))
			return 0;

		if (j - i == 0)
		{
			if (char_from_file != 59)
				return 0;

			char_from_file = getChar(map_file);

			if (char_from_file != 10)
				return 0;

			char_from_file = getChar(map_file);

			if (char_from_file != EOF)
				return 0;
		} else if (end_of_line)
		{
			if (!(char_from_file == 10))
				return 0;
			else
			{
				if (char_from_file == 10)
				{
					end_of_line = 0;
					position_in_line = 1;
					set_char = 1;
				}
			}
		} else
		{
			if (set_char)
			{
				if (!(char_from_file == FREE_CHAR || char_from_file == GRASS_CHAR || char_from_file == ROCK_CHAR || char_from_file == TREE_CHAR || char_from_file == WATER_CHAR))
					return 0;
				set_char = 0;
				position_in_line++;
			} else
			{
				if (char_from_file != 44)
					return 0;
				if (position_in_line == (file_length - 1))
					end_of_line = 1;
				else
				{
					set_char = 1;
					position_in_line++;
				}
			}
		}
	}

	return 1;
}

/*
 * The goal here is to exclude the CR char that is specific to Windows char encoding.
 */
char getChar(FILE *map_file)
{
	char char_from_file = fgetc(map_file);
	if (char_from_file == 13)
		return fgetc(map_file);
	return char_from_file;
}

/*
 * Used to fill cell contents when the map is read from a file.
 */

void setCellContentFromMapFile(int isFreeCell, char *setChar, Cell *cell)
{
	if (isFreeCell)
		cell->union_type = FREE_CELL_UNION_TYPE;
	else
	{
		cell->union_type = SET_CELL_UNION_TYPE;
		switch (*setChar)
		{
		case TREE_CHAR:
			cell->set_cell.set_enum = TREE;
			break;
		case WATER_CHAR:
			cell->set_cell.set_enum = WATER;
			break;
		case GRASS_CHAR:
			cell->set_cell.set_enum = GRASS;
			break;
		case ROCK_CHAR:
			cell->set_cell.set_enum = ROCK;
			break;
		}
	}
}

/**
 * Memory allocation for the map.
 */
int allocateMemForWorldMapAndContent(int height, int length, WorldMap *world_map)
{
	world_map->cells = (Cell**) calloc(height, sizeof(Cell*));

	if (world_map->cells == NULL)
		return -1;

	int y;

	for (y = 0; y < height; y++)
	{
		world_map->cells[y] = calloc(length, sizeof(Cell));
		if (world_map->cells[y] == NULL)
			return -1;
	}

	return 0;
}

/*
 * Memory allocation function for the free position structure.
 */
int allocateMemForFreePositions(FreePositions *free_positions, int number_of_free_cells)
{
	free_positions->array = (Position*) calloc(number_of_free_cells, sizeof(Position));
	free_positions->number = 0;
	free_positions->max_number = number_of_free_cells;

	if (free_positions->array == NULL)
		return -1;
	return 0;
}

/*
 * Fills the FreePositions structure accordingly to the free positions from the map.
 * Should only be called once at the "build world map" stage.
 */
void fillFreePositions(WorldMap *world_map, FreePositions *free_positions)
{
	int x, y;

	for (y = 0; y < world_map->height; y++)
	{
		for (x = 0; x < world_map->length; x++)
		{
			if (world_map->cells[y][x].union_type == FREE_CELL_UNION_TYPE)
			{
				free_positions->number++;
				free_positions->array[free_positions->number - 1].x = x;
				free_positions->array[free_positions->number - 1].y = y;
			}
		}
	}
}

/*
 * If the position can be found in the array of free positions, then it is free.
 */
int positionIsFree(FreePositions *free_positions, Position *position)
{
	int i;

	for (i = 0; i < free_positions->number; i++)
	{
		if (free_positions->array[i].x == position->x && free_positions->array[i].y == position->y)
			return 1;
	}
	return 0;
}

/*
 * If the array of free positions is full, will return -1.
 */
int addFreePosition(FreePositions *free_positions, Position *position)
{
	if (free_positions->number == free_positions->max_number)
		return -1;

	free_positions->array[free_positions->number].x = position->x;
	free_positions->array[free_positions->number].y = position->y;
	free_positions->number++;

	return 0;
}

/*
 * If the array of free positions is empty, will return -1.
 */
int removeFreePosition(FreePositions *free_positions, Position *position)
{
	if (!positionIsFree(free_positions, position) || free_positions->number == 0)
		return -1;

	int i = 0, found = 0;

	while (!found && (i < free_positions->number))
	{
		if (free_positions->array[i].x == position->x && free_positions->array[i].y == position->y)
			found = 1;
		else
			i++;
	}

	free_positions->array[i].x = free_positions->array[free_positions->number - 1].x;
	free_positions->array[i].y = free_positions->array[free_positions->number - 1].y;
	free_positions->number--;

	return 0;
}

/*
 * Generating a random free position will return -1 if none is available.
 * Make sure that srand(time(NULL)) has only been called once in your
 * program before calling this function.
 */
int generateRandomFreePosition(WorldMap *world_map, Position *position, FreePositions *free_positions)
{
	if (free_positions->number == 0)
		return -1;

	int x, y, i, found = 0;

	while (!found)
	{
		i = 0;
		x = rand() % (world_map->length);
		y = rand() % (world_map->height);

		while (i < free_positions->number && !found)
		{
			if (free_positions->array[i].x == x && free_positions->array[i].y == y)
				found = 1;
			else
				i++;
		}
	}

	position->x = x;
	position->y = y;

	return 0;
}
/*
 * Will serve the first player in the incoming players queue.
 * If the connection with the player was lost during transfer, he will
 * simply be eradicated from the server structures.
 */
int serveFirstWaitingPlayerInQueue(WorldMap *world_map, PlayersQueue *incoming_players, PlayersQueue *outgoing_players, FreePositions *free_positions, PlayerManagement *player_management, Buffer *send_buffer)
{
	Player *new_player = removePlayerFromQueue(incoming_players);

	if (new_player != NULL)
	{
		if (new_player->position.x == -1 && new_player->position.y == -1)
		{
			if (generateRandomFreePosition(world_map, &(new_player->position), free_positions) == -1)
			{
				addPlayerToQueue(outgoing_players, new_player);
				return -1;
			}
		} else
		{
			if (!positionIsFree(free_positions, &(new_player->position)))
			{
				if (generateRandomFreePosition(world_map, &(new_player->position), free_positions) == -1)
				{
					addPlayerToQueue(outgoing_players, new_player);
					return -1;
				}
			}
		}

		//put the player on the map, in the playerManagement, and remove his position from FreePositions
		putPlayerOnMapAndPlayerManagement(player_management, world_map, new_player, free_positions);

		//If the connection was lost or something went wrong, we remove the player from where he was put before
		if (sendContextPacketToNewPlayers(new_player, player_management, send_buffer) == -2)
			removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, new_player, free_positions);
		else
		{
			//Send to all the other players if any can be found on the map.
			if (player_management->nb_players > 1)
			{
				int i;
				for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
				{
					if (player_management->players[i] != NULL && new_player->index != player_management->players[i]->index)
					{
						if (sendAddedPlayerPacketToPlayer(new_player, player_management->players[i], send_buffer) == -2)
							removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player_management->players[i], free_positions);
					}
				}
			}

		}

		return 0;
	}

	return 0;
}

/*
 * Adds a player to the map structure and player management.
 */
void putPlayerOnMapAndPlayerManagement(PlayerManagement *player_management, WorldMap *world_map, Player *player, FreePositions *free_positions)
{
	world_map->cells[player->position.y][player->position.x].union_type = PLAYER_CELL_UNION_TYPE;
	world_map->cells[player->position.y][player->position.x].player_cell.player = player;

	if (addPlayerToPlayerManagement(player_management, player) == -1)
		errorMessage("Error addPlayerToPlayerManagement IN putPlayerOnMapAndPlayerManagement.");

	if (removeFreePosition(free_positions, &(player->position)) == -1)
		errorMessage("Error removeFreePosition IN putPlayerOnMapAndPlayerManagement.");
}

/*
 * Will remove the player from the map and from player management structures. If other clients did not
 * receive the changes and the connection is lost, they will be themselves removed.
 */
void removePlayerFromMapAndPlayerManagementAndSendToAll(Buffer *send_buffer, PlayersQueue *outgoing_players, PlayerManagement *player_management, WorldMap *world_map, Player *player_to_remove, FreePositions *free_positions)
{
	world_map->cells[player_to_remove->position.y][player_to_remove->position.x].union_type = FREE_CELL_UNION_TYPE;
	removePlayerFromPlayerManagement(player_management, player_to_remove);

	if (addFreePosition(free_positions, &(player_to_remove->position)) == -1)
		errorMessage("Error addFreePosition IN removePlayerFromMapAndPlayerManagementAndSendToAll.");

	int i;

	for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
	{
		if (player_management->players[i] != NULL)
		{
			if (sendRemovedPlayerPacketToPlayer(player_to_remove, player_management->players[i], send_buffer) == -2)
				removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player_management->players[i], free_positions);
		}
	}

	if (addPlayerToQueue(outgoing_players, player_to_remove) == -1)
		errorMessage("Error addPlayerToQueue IN removePlayerFromMapAndPlayerManagementAndSendToAll.");

}

void updatePlayerPositionOnMap(Player *player, WorldMap *world_map, Position *new_position)
{
	world_map->cells[player->position.y][player->position.x].union_type = FREE_CELL_UNION_TYPE;
	player->position.x = new_position->x;
	player->position.y = new_position->y;
}

int playerCanMoveToPosition(Player *player, WorldMap *world_map, unsigned char *direction, FreePositions *free_positions, Position *new_position)
{
	switch (*direction)
	{
	case LEFT_MOVE:
		new_position->x = player->position.x - 1;
		new_position->y = player->position.y;
		break;
	case RIGHT_MOVE:
		new_position->x = player->position.x + 1;
		new_position->y = player->position.y;
		break;
	case UP_MOVE:
		new_position->x = player->position.x;
		new_position->y = player->position.y - 1;
		break;
	case DOWN_MOVE:
		new_position->x = player->position.x;
		new_position->y = player->position.y + 1;
		break;
	}

	if (new_position->x < 0 || new_position->x > world_map->length - 1 || new_position->y < 0 || new_position->y > world_map->height - 1)
		return 0;

	return positionIsFree(free_positions, new_position);
}

/*
 * Will send a packet to the player containing his own login, index and position and those of other
 * players if any could be found.
 */
int sendContextPacketToNewPlayers(Player *new_player, PlayerManagement *player_management, Buffer *send_buffer)
{
	int i;

	GenericPacket packet;
	packet.type = SEND_CONTEXT;
	packet.data_length = 0;

	for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
	{
		if (player_management->players[i] != NULL)
			packet.data_length += (unsigned int) strlen(player_management->players[i]->login) + (2 * sizeof(char)) + (2 * sizeof(short)); // should be 6 = char \0 + char index + short x + short y
	}

	packet.data = calloc(packet.data_length, sizeof(char));

	if (packet.data == NULL)
		return -3;

	unsigned char *pointer = packet.data;

	int login_length = (int) strlen(new_player->login);

	memcpy(pointer, new_player->login, login_length);
	pointer = packet.data + login_length;

	*((char *) pointer) = '\0';
	pointer++;

	*((char *) pointer) = new_player->index;
	pointer++;

	*((short *) pointer) = htons((short) new_player->position.x);
	pointer += sizeof(short);

	*((short *) pointer) = htons((short) new_player->position.y);
	pointer += sizeof(short);

	traceMessage("LOGIN %s | INDEX %d | (%d,%d)", new_player->login, new_player->index, new_player->position.x, new_player->position.y);

	for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
	{
		if (player_management->players[i] != NULL && player_management->players[i]->index != new_player->index)
		{
			int login_length = (int) strlen(player_management->players[i]->login);

			memcpy(pointer, player_management->players[i]->login, login_length);
			pointer += login_length;

			*((char *) pointer) = '\0';
			pointer++;

			*((char *) pointer) = player_management->players[i]->index;
			pointer++;

			*((short *) pointer) = htons((short) player_management->players[i]->position.x);
			pointer += sizeof(short);

			*((short *) pointer) = htons((short) player_management->players[i]->position.y);
			pointer += sizeof(short);

			traceMessage("LOGIN %s | INDEX %d | (%d,%d)", player_management->players[i]->login, player_management->players[i]->index, player_management->players[i]->position.x, player_management->players[i]->position.y);

		}

	}

	traceMessage("Packet %d is going to be sent to %s.", SEND_CONTEXT, new_player->login);

	int error_code = -1;

	while (error_code == -1)
		error_code = sendPacket(&new_player->connection, &packet, send_buffer);

	free(packet.data);

	return error_code;
}

/*
 * Sends to player the login, index and position of the newly added to the map player
 */
int sendAddedPlayerPacketToPlayer(Player *new_player, Player *player_to_send_to, Buffer *send_buffer)
{
	GenericPacket packet;

	packet.type = ADD_PLAYER;
	packet.data_length = (int) strlen(new_player->login) + (2 * sizeof(char)) + (2 * sizeof(short)); // should be 6 = char \0 + char index + short x + short y
	packet.data = calloc(packet.data_length, sizeof(char));

	if (packet.data == NULL)
		return -3;

	unsigned char *pointer = packet.data;
	int error_code = -1, login_length = (int) strlen(new_player->login);

	memcpy(pointer, new_player->login, login_length);
	pointer += login_length;

	*((char *) pointer) = '\0';
	pointer++;

	*((char *) pointer) = new_player->index;
	pointer++;

	*((short *) pointer) = htons((short) new_player->position.x);
	pointer += sizeof(short);

	*((short *) pointer) = htons((short) new_player->position.y);
	pointer += sizeof(short);

	traceMessage("Packet %d is going to be sent to %s.", ADD_PLAYER, player_to_send_to->login);

	while (error_code == -1)
		error_code = sendPacket(&player_to_send_to->connection, &packet, send_buffer);

	free(packet.data);

	return error_code;
}

/*
 * Sends to player the index and position of the newly deleted from the map player
 */
int sendRemovedPlayerPacketToPlayer(Player *player_to_remove, Player *player_to_send_to, Buffer *send_buffer)
{
	GenericPacket packet;

	packet.type = REMOVE_PLAYER;
	packet.data_length = sizeof(char) + (2 * sizeof(short)); // should be 5 = char index + short x + short y
	packet.data = calloc(packet.data_length, sizeof(char));

	if (packet.data == NULL)
		return -3;

	unsigned char *pointer = packet.data;

	*((char*) pointer) = player_to_remove->index;
	pointer++;

	*((short*) pointer) = htons((short) player_to_remove->position.x);
	pointer += sizeof(short);

	*((short*) pointer) = htons((short) player_to_remove->position.y);
	pointer += sizeof(short);

	traceMessage("Packet %d is going to be sent to %s.", REMOVE_PLAYER, player_to_send_to->login);

	int error_code = -1;

	while (error_code == -1)
		error_code = sendPacket(&player_to_send_to->connection, &packet, send_buffer);

	free(packet.data);

	return error_code;
}

/*
 *	Notifies a player that another player has moved on the map.
 */
int sendPlayerPositionChangedPacketToPlayer(Player *player_that_moved, Position *previous_position, Player *player_to_send_to, Buffer *send_buffer)
{
	GenericPacket packet;

	packet.type = PLAYER_UPDATE;
	packet.data_length = sizeof(char) + (4 * sizeof(short)); // should be 9 = char index + short x + short y + short x + short y
	packet.data = calloc(packet.data_length, sizeof(char));

	if (packet.data == NULL)
		return -3;

	unsigned char *pointer = packet.data;

	*((char*) pointer) = player_that_moved->index;
	pointer++;

	*((short*) pointer) = htons((short) player_that_moved->position.x);
	pointer += sizeof(short);

	*((short*) pointer) = htons((short) player_that_moved->position.y);
	pointer += sizeof(short);

	*((short*) pointer) = htons((short) previous_position->x);
	pointer += sizeof(short);

	*((short*) pointer) = htons((short) previous_position->y);
	pointer += sizeof(short);

	traceMessage("Packet %d is going to be sent to %s.", PLAYER_UPDATE, player_to_send_to->login);

	int error_code = -1;

	while (error_code == -1)
		error_code = sendPacket(&player_to_send_to->connection, &packet, send_buffer);

	free(packet.data);

	return error_code;
}

/*
 * Sends to a player that his move demand has been rejected.
 */
int sendCouldNotMovePlayerPacketToPlayer(Player *player_to_send_to, Buffer *send_buffer)
{
	GenericPacket packet;

	packet.type = REJECT_MOVE;
	packet.data_length = 0;
	packet.data = NULL;

	traceMessage("Packet %d is going to be sent to %s.", REJECT_MOVE, player_to_send_to->login);

	int error_code = -1;

	while (error_code == -1)
		error_code = sendPacket(&player_to_send_to->connection, &packet, send_buffer);

	free(packet.data);

	return error_code;
}

void dealWithIncommingPacket(GenericPacket *packet, PlayerManagement *player_management, WorldMap *world_map, PlayersQueue *outgoing_players, FreePositions *free_positions, Player *player, Buffer *send_buffer)
{
	if (packet->type == MOVE_REQUEST && packet->data_length == 1)
	{
		//Check that what was received is coherent
		unsigned char *direction = packet->data;

		if (*direction != LEFT_MOVE && *direction != RIGHT_MOVE && *direction != UP_MOVE && *direction != DOWN_MOVE)
		{
			if (sendCouldNotMovePlayerPacketToPlayer(player, send_buffer) == -2)
				removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player, free_positions);
		} else
		{
			Position new_position, previous_position;

			if (playerCanMoveToPosition(player, world_map, direction, free_positions, &new_position))
			{
				previous_position.x = player->position.x;
				previous_position.y = player->position.y;

				if (removeFreePosition(free_positions, &new_position) == -1)
					errorMessage("Error removeFreePosition IN dealWithIncommingPacket");

				if (addFreePosition(free_positions, &previous_position) == -1)
					errorMessage("Error addFreePosition IN dealWithIncommingPacket.");

				updatePlayerPositionOnMap(player, world_map, &new_position);

				int i;

				for (i = 0; i < MAX_NUMBER_OF_CONNECTED_PLAYERS; i++)
				{
					if (player_management->players[i] != NULL)
					{
						if (sendPlayerPositionChangedPacketToPlayer(player, &previous_position, player_management->players[i], send_buffer) == -2)
							removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player_management->players[i], free_positions);
					}
				}

			} else
			{
				if (sendCouldNotMovePlayerPacketToPlayer(player, send_buffer) == -2)
					removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player, free_positions);
			}
		}
	} else if (packet->type == DISCONNECTION && packet->data_length == 0)
		removePlayerFromMapAndPlayerManagementAndSendToAll(send_buffer, outgoing_players, player_management, world_map, player, free_positions);
	else
		errorMessage("Error PACKET IGNORED (not a move or a disconnect) IN dealWithIncommingPacket.");
}

/*int main()
 {
 WorldMap world_map;
 FreePositions free_positions;

 //If the map building sequence has failed, we quit the program.
 if (buildWorldMap(&world_map, &free_positions) == -1)
 exit(EXIT_FAILURE);

 int y, x;

 printf("\nThe map.\n");

 for (y = 0; y < world_map.height; y++)
 {
 for (x = 0; x < world_map.length; x++)
 {
 if (world_map.cells[y][x].union_type == FREE_CELL_UNION_TYPE)
 printf("f");
 else if (world_map.cells[y][x].union_type == SET_CELL_UNION_TYPE)
 printf("%d", world_map.cells[y][x].set_cell.set_enum);

 }
 printf("\n");
 }

 printf("\nJust going to show free positions.\n");
 Position position;

 for (y = 0; y < world_map.height; y++)
 {
 for (x = 0; x < world_map.length; x++)
 {
 position.x = x;
 position.y = y;

 if (positionIsFree(&free_positions, &position))
 printf("f");
 else
 printf("-");
 }
 printf("\n");
 }

 printf("Adding free position. %d\n", addFreePosition(&free_positions, &position));

 printf("\nA few random generations of free positions.\n");

 x = 0;

 srand(time(NULL));

 while (x < 10)
 {
 y = generateRandomFreePosition(&world_map, &position, &free_positions);

 if (y == 0)
 printf("x %d, y %d\n", position.x, position.y);
 else
 printf("Problem");

 x++;

 removeFreePosition(&free_positions, &position);
 addFreePosition(&free_positions, &position);
 }

 return 0;
 }*/
