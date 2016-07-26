/*
 * utilities.h
 *
 *  Created on: Oct 24, 2013
 *      Author: nth
 */

#ifndef UTILITIES_H_
#define UTILITIES_H_

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>

#define RUN_GAME_MANAGEMENT_THREAD 1 // decommenter (commenter) cette ligne pour lancer (ou non) le service gestion de jeu.

// différents niveaux des messages
#define TRACE_LEVEL 		0
#define DEBUG_LEVEL 		1
#define WARNING_LEVEL 		2
#define ERROR_LEVEL 		3
#define FATAL_ERROR_LEVEL 	4

#define NO_MESSAGE_SHOWN	5 // valeur a utiliser pour n'afficher aucun message.

// cette constante permet de spécifier quel est le niveau minimum de messages à afficher. les valeurs possibles sont les valeurs définies par les constantes ci-dessus.
#ifndef MIN_LEVEL_SHOWN_MESSAGES
#define MIN_LEVEL_SHOWN_MESSAGES TRACE_LEVEL
#endif

// cette macro-fonction ne doit pas être utilisée.
// le do{...}while(0) permet de pallier aux problemes du point-virgule lors de l'appel des macros dans une structure conditionnelle sans blocs (cf sans "{}").
#define message(level, ...) do{fprintf(stderr, "\n%s message in file \"%s\"    in function %s (line %d) : \n", level, __FILE__, __FUNCTION__, __LINE__); fprintf(stderr, __VA_ARGS__);}while(0)

/**
 * Ces fonctions permettent de gérer l'affichage de messages à différents niveaux : trace, debug, warning, error et fatal error.
 * L'utilisation de ces macros-fonctions est identique à l'utilisation de la fonction printf, les paramètres sont les mêmes.
 * Le message à afficher sera précédé d'une ligne du type :
 * "[niveau du message] message in file "[fichier dans lequel la macro-fonction est appelée]" in function [fonction dans laquelle la macro est utilisée] (line [numero de la ligne où la macro est utilisée]) : "
 */


#if MIN_LEVEL_SHOWN_MESSAGES <= TRACE_LEVEL
#define traceMessage(...) message("Trace", __VA_ARGS__)
#else
#define traceMessage(...) {}
#endif

#if MIN_LEVEL_SHOWN_MESSAGES <= DEBUG_LEVEL
#define debugMessage(...) message("Debug", __VA_ARGS__)
#else
#define debugMessage(...) {}
#endif

#if MIN_LEVEL_SHOWN_MESSAGES <= WARNING_LEVEL
#define warningMessage(...) message("Warning", __VA_ARGS__)
#else
#define warningMessage(...) {}
#endif

#if MIN_LEVEL_SHOWN_MESSAGES <= ERROR_LEVEL
#define errorMessage(...) message("Error", __VA_ARGS__)
#else
#define errorMessage(...) {}
#endif

#if MIN_LEVEL_SHOWN_MESSAGES <= FATAL_ERROR_LEVEL
#define fatalErrorMessage(...) message("Fatal error", __VA_ARGS__)
#else
#define fatalErrorMessage(...) {}
#endif





#define FALSE 0
#define TRUE !FALSE

char* strCreateCopy(const char *string);
char* extractStringWithoutSpace(const char *string);

int readUIntFromFile(int file_descriptor, unsigned int *result, char stop_character);
int goAfterCharacterInFile(int file_descriptor, char character);
int readStringFromFile(int file_descriptor, char *buffer, unsigned int length, char stop_character);

// structure pour nos buffers.
typedef struct
{
	unsigned int buffer_length;
	unsigned char *data;
} Buffer;


void initBuffer(Buffer *buffer);
// étend le buffer sans sauvegarder le contenu.
void expandBufferLength(Buffer *buffer, unsigned int new_length);

void expandBufferLengthWithCpy(Buffer *buffer, unsigned int new_length);

long int getTime();

#endif /* UTILITIES_H_ */
