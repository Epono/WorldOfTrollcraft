/*
 * utilities.c
 *
 *  Created on: Oct 27, 2013
 *      Author: nth
 */

#include "../includes/utilities.h"

char* strCreateCopy(const char *string)
{
	if(string == NULL)
		return NULL;

	size_t length = strlen(string) + 1;

	char *copy = (char *) calloc(length, sizeof(char));
	memcpy(copy, string, length);
	return copy;
}

/**
 * Supprime les caracteres blancs en debut et fin de chaine, en modifiant la chaine 'string'.
 * Retourne la chaine modifiée, ou 'NULL' en cas d'erreur.
 */
char* extractStringWithoutSpace(const char *string)
{
	if(string == NULL) return NULL;

	char *extracted_string = (char *) string;
	// suppression des blancs en debut de chaine.
	while(*extracted_string != '\0' && isspace(*extracted_string))
		extracted_string++;

	if(*extracted_string == '\0')
		return extracted_string;

	int length = strlen(extracted_string);
	int i;
	// suppression des blancs en fin de chaine.
	for(i = length-1; i >=0 && isspace(extracted_string[i]); i--);

	extracted_string[i+1] = '\0';
	return extracted_string;
}

/**
 * lit l'entier non signe suivant dans le fichier et le retourne dans 'resultat'. Ignore tous les caracteres precedants l'entier.
 * Retourne 0 si un entier est trouve, -2 si aucun entier n'est trouvé, -1 sinon.
 */
int readUIntFromFile(int file_descriptor, unsigned int *result, char stop_character)
{
	if(result == NULL)
		return -1;

	*result = 0;
	char next_c = 0;
	int return_value;

	while((return_value = read(file_descriptor, &next_c, sizeof(char))) > 0 && !isdigit(next_c) && next_c != stop_character);
	if(return_value == 0)
		return -2;
	else if(return_value < 0)
		return -1;
	else if(next_c == stop_character)
	{
		lseek(file_descriptor, (long) -1, SEEK_CUR);
		return -2;
	}

	*result = (*result) * 10 + (next_c - '0');
	while((return_value = read(file_descriptor, &next_c, sizeof(char))) > 0 && isdigit(next_c))
		*result = (*result) * 10 + (next_c - '0');

	if(return_value == 0)
		return 0;
	else if(return_value < 0)
		return -1;

	lseek(file_descriptor, (long) -1, SEEK_CUR);

	return 0;
}

/**
 * positionne le curseur du fichier apres le premier caractere 'c' lu.
 * Retourne -1 en cas d'erreur, sinon 0.
 */
int goAfterCharacterInFile(int file_descriptor, char character)
{
	char next_c = 0;
	int return_value;
	while((return_value = read(file_descriptor, &next_c, sizeof(char))) > 0 && next_c != character);

	if(return_value < 0)
			return -1;
	return 0;
}

/**
 * lit les caractères du fichiers et les écrit dans le buffer 'buffer' tant que le buffer n'est pas rempli et que le caractere 'stop_character' n'a pas été trouvé.
 * Retourne le nombre de caractère lus, ou -1 en cas d'erreur.
 */
int readStringFromFile(int file_descriptor, char *buffer, unsigned int length, char stop_character)
{
	if(length == 0) return 0;
	if(buffer == NULL) return -1;

	unsigned int nb_characters = 0;
	int return_value;
	char next_c;

	while(nb_characters < length-1 && (return_value = read(file_descriptor, &next_c, sizeof(char))) > 0 && next_c != stop_character)
	{
		buffer[nb_characters] = next_c;
		nb_characters++;
	}
	if(return_value < 0)
		return -1;
	else if(return_value > 0 && next_c == stop_character)
		lseek(file_descriptor, (long) -1, SEEK_CUR);
	buffer[nb_characters] = '\0'; // ajout du caractere de fin de chaine.
	return nb_characters;
}

/**
 * Retourne le nombre de micro-secondes passées depuis le 1er janvier 1970
 */
long int getTime()
{
	struct timeval time;
	if(gettimeofday(&time, NULL) < 0)
		return -1;
	return time.tv_sec * 1000000 + time.tv_usec;
}

// fonctions de manipulation de buffers.

void initBuffer(Buffer *buffer)
{
	if (buffer != NULL)
	{
		buffer->buffer_length = 0;
		buffer->data = NULL;
	}
}

/**
 * Cette procedure permet d'augmenter la taille du buffer si nécessaire (le contenu du buffer n'est pas garde).
 */
void expandBufferLength(Buffer *buffer, unsigned int new_length)
{
	if (buffer != NULL && buffer->buffer_length < new_length)
	{
		if (buffer->data != NULL)
			free(buffer->data);

		buffer->buffer_length = new_length;
		buffer->data = (unsigned char *) malloc(new_length);
	}
}

/**
 * Cette procedure permet d'augmenter la taille du buffer si nécessaire (le contenu du buffer n'est pas garde).
 */
void expandBufferLengthWithCpy(Buffer *buffer, unsigned int new_length)
{
	if (buffer != NULL && buffer->buffer_length < new_length)
	{
		unsigned char *data =(unsigned char *) malloc(new_length);
		if (buffer->data != NULL)
		{
			memcpy(data, buffer->data, buffer->buffer_length);
			free(buffer->data);
		}

		buffer->buffer_length = new_length;
		buffer->data = data;
	}
}


